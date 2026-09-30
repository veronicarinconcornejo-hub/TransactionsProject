package com.financial.transactions.service;

import com.financial.transactions.client.PaymentProviderClient;
import com.financial.transactions.dto.*;
import com.financial.transactions.exceptions.BusinessRuleException;
import com.financial.transactions.exceptions.IdempotencyConflictException;
import com.financial.transactions.exceptions.IdempotencyKeyMismatchException;
import com.financial.transactions.exceptions.ProviderException;
/* ===== AQUÍ VA ESTO: el import de la nueva excepción del 409 ===== */
import com.financial.transactions.exceptions.TransactionAlreadyProcessedException;
import com.financial.transactions.kafka.PaymentEvent;
import com.financial.transactions.kafka.PaymentEventProducer;
import com.financial.transactions.model.Transaction;
import com.financial.transactions.model.TransactionStatus;
import com.financial.transactions.model.TransactionType;
import com.financial.transactions.repository.TransactionRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;

@Service
public class TransactionService {

    private static final BigDecimal MIN_AMOUNT = new BigDecimal("1.00");
    private static final BigDecimal MAX_DEBIT  = new BigDecimal("10000.00");
    private static final String ALLOWED_CURRENCY = "MXN";

    private final PaymentProviderClient providerClient;
    private final TransactionRepository repository;
    private final MongoTemplate mongoTemplate;
    private final PaymentEventProducer eventProducer;
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    public TransactionService(PaymentProviderClient providerClient,
                              TransactionRepository repository,
                              MongoTemplate mongoTemplate,
                              PaymentEventProducer eventProducer) {
        this.providerClient = providerClient;
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.eventProducer = eventProducer;
    }

    /* ===== AQUÍ VA ESTO: execute ahora devuelve TransactionResponse (antes era TransactionExecutionResult) ===== */
    public TransactionResponse execute(TransactionRequest request,
                                       String idempotencyKey) {

        // La llave de idempotencia es obligatoria y no puede venir vacia
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessRuleException(
                    "El header Idempotency-Key es obligatorio y no puede estar vacio"
            );
        }

        // La validacion va primero, para que una peticion invalida no reserve la llave
        validateBusinessRules(request);

        // Capa 1: la llave ya se uso?
        Optional<Transaction> existente = repository.findByIdempotencyKey(idempotencyKey);
        if (existente.isPresent()) {
            return responderPeticionRepetida(existente.get(), request, idempotencyKey);
        }

        // Capa 2: reservar la llave ANTES de llamar al proveedor
        Transaction tx = buildPendingTransaction(request, idempotencyKey);
        try {
            tx = repository.insert(tx);
            logger.info(">>> Idempotencia: llave {} reservada, id={}", idempotencyKey, tx.getId());
        } catch (DuplicateKeyException e) {
            logger.warn(">>> Idempotencia: llave {} reservada por otra peticion concurrente", idempotencyKey);
            Transaction ganadora = repository.findByIdempotencyKey(idempotencyKey).orElseThrow(() -> e);
            return responderPeticionRepetida(ganadora, request, idempotencyKey);
        }

        try {
            ProviderResult result = providerClient.execute(
                    new ProviderRequest(
                            request.accountId(),
                            request.type(),
                            request.amount(),
                            request.currency()
                    )
            );
            applyProviderResult(tx, result);

        } catch (CallNotPermittedException e) {
            logger.warn("Circuit Breaker abierto. No se realizo llamada al proveedor para accountId={}",
                    request.accountId());
            tx.setStatus(TransactionStatus.FAILED);

        } catch (ProviderException e) {
            logger.error("Error al comunicarse con el proveedor para accountId={}: {}",
                    request.accountId(), e.getMessage());
            tx.setStatus(TransactionStatus.FAILED);

        } catch (RuntimeException e) {
            logger.error("Error inesperado al procesar la transaccion id={}", tx.getId(), e);
            tx.setStatus(TransactionStatus.FAILED);
        }

        Transaction saved = repository.save(tx);

        logger.info(">>> Transaccion guardada en BD: id={}, status={}", saved.getId(), saved.getStatus());

        PaymentEvent event = new PaymentEvent(
                "transactionExecuted",
                saved.getId(),
                saved.getUserId(),
                saved.getAccountId(),
                saved.getTransactionType().toString(),
                saved.getAmount(),
                saved.getCurrency(),
                saved.getStatus().toString(),
                obtenerMotivoFallo(saved),
                java.time.Instant.now());

        if (saved.getStatus() == TransactionStatus.EXECUTED) {
            logger.info(">>> Publicando evento EXITOSO a Kafka para id={}", saved.getId());
            eventProducer.publishSuccess(event);
        } else {
            logger.info(">>> Publicando evento FALLIDO a Kafka para id={}, status={}",
                    saved.getId(), saved.getStatus());
            eventProducer.publishFailure(event);
        }

        /* ===== AQUÍ VA ESTO: el return final ahora es TransactionResponse directo
                 (antes era: return new TransactionExecutionResult(..., false);) ===== */
        return TransactionResponse.from(saved);
    }

    /* ===== AQUÍ VA ESTO: responderPeticionRepetida ahora devuelve TransactionResponse
             y en el caso "ya termino" LANZA la excepción del 409 en vez de devolver 200 ===== */
    private TransactionResponse responderPeticionRepetida(
            Transaction existente,
            TransactionRequest request,
            String idempotencyKey) {

        // Misma llave con otros datos -> 422
        if (!esLaMismaPeticion(existente, request)) {
            throw new IdempotencyKeyMismatchException(
                    "La Idempotency-Key ya se uso con datos diferentes"
            );
        }

        // La primera peticion sigue procesandose -> 409 (en proceso)
        if (existente.getStatus() == TransactionStatus.PENDING) {
            throw new IdempotencyConflictException(
                    "La transaccion con esta llave se esta procesando"
            );
        }

        /* ===== AQUÍ VA ESTO: el cambio principal del 409.
                 Antes devolvía: return new TransactionExecutionResult(..., true);
                 Ahora lanza la excepción que el handler convierte en 409 ===== */
        logger.info(">>> Idempotencia: la llave {} ya fue procesada, id={}",
                idempotencyKey, existente.getId());
        throw new TransactionAlreadyProcessedException(
                "La transaccion ya fue procesada anteriormente"
        );
    }

    // Compara la peticion repetida contra la original
    private boolean esLaMismaPeticion(Transaction tx, TransactionRequest r) {
        return Objects.equals(tx.getAccountId(), r.accountId())
                && Objects.equals(tx.getUserId(), r.userId())
                && tx.getTransactionType() == r.type()
                && tx.getAmount().compareTo(r.amount()) == 0
                && tx.getCurrency().equalsIgnoreCase(r.currency());
    }

    private void validateBusinessRules(TransactionRequest r) {
        if (r.amount().compareTo(MIN_AMOUNT) <= 0) {
            throw new BusinessRuleException("El monto debe ser mayor a $1.00");
        }
        if (r.type() == TransactionType.DEBIT && r.amount().compareTo(MAX_DEBIT) > 0) {
            throw new BusinessRuleException("Una transaccion DEBIT no puede exceder $10,000.00");
        }
        if (!ALLOWED_CURRENCY.equalsIgnoreCase(r.currency())) {
            throw new BusinessRuleException("Solo se aceptan transacciones en MXN");
        }
    }

    private Transaction buildPendingTransaction(TransactionRequest r, String idempotencyKey) {
        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID().toString());
        tx.setUserId(r.userId());
        tx.setAccountId(r.accountId());
        tx.setTransactionType(r.type());
        tx.setAmount(r.amount());
        tx.setCurrency(r.currency());
        tx.setDescription(r.description());
        tx.setCreatedAt(Instant.now());
        tx.setStatus(TransactionStatus.PENDING);
        tx.setIdempotencyKey(idempotencyKey);
        return tx;
    }

    private void applyProviderResult(Transaction tx, ProviderResult result) {
        if (result.approved()) {
            tx.setStatus(TransactionStatus.EXECUTED);
            tx.setProviderTransactionId(result.providerTransactionId());
            tx.setBalanceAfter(result.balance());
        } else {
            tx.setStatus(TransactionStatus.REJECTED);
        }
    }

    public Page<TransactionResponse> search(String accountId, TransactionStatus status,
                                            TransactionType type, int page, int limit) {
        Query query = new Query();
        if (accountId != null) query.addCriteria(Criteria.where("accountId").is(accountId));
        if (status != null)    query.addCriteria(Criteria.where("status").is(status));
        if (type != null)      query.addCriteria(Criteria.where("transactionType").is(type));

        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        long total = mongoTemplate.count(query, Transaction.class);
        List<Transaction> results = mongoTemplate.find(query.with(pageable), Transaction.class);

        return new PageImpl<>(results.stream().map(TransactionResponse::from).toList(), pageable, total);
    }

    public List<TransactionResponse> searchAll() {
        return repository.findAll().stream()
                .map(TransactionResponse::from)
                .toList();
    }

    private String obtenerMotivoFallo(Transaction tx) {
        if (tx.getStatus() == TransactionStatus.REJECTED) return "Rechazada por el proveedor";
        if (tx.getStatus() == TransactionStatus.FAILED) return "Proveedor no disponible";
        return null;
    }
}