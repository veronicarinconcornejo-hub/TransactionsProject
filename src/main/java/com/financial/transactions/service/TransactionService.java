package com.financial.transactions.service;

import com.financial.transactions.dto.ProviderRequest;
import com.financial.transactions.dto.ProviderResult;
import com.financial.transactions.dto.TransactionRequest;
import com.financial.transactions.dto.TransactionResponse;
import com.financial.transactions.exceptions.BusinessRuleException;
import com.financial.transactions.exceptions.ProviderException;
import com.financial.transactions.model.Transaction;
import com.financial.transactions.model.TransactionStatus;
import com.financial.transactions.model.TransactionType;
import com.financial.transactions.repository.PaymentProviderClient;
import com.financial.transactions.repository.TransactionRepository;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TransactionService {

    private static final BigDecimal MIN_AMOUNT = new BigDecimal("1.00");
    private static final BigDecimal MAX_DEBIT  = new BigDecimal("10000.00");
    private static final String ALLOWED_CURRENCY = "MXN";

    private final PaymentProviderClient providerClient;
    private final TransactionRepository repository;
    private final MongoTemplate mongoTemplate;
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    public TransactionService(PaymentProviderClient providerClient,
                              TransactionRepository repository, MongoTemplate mongoTemplate) {
        this.providerClient = providerClient;
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    public TransactionResponse execute(TransactionRequest request) {

        validateBusinessRules(request);

        Transaction tx;

        try {

            ProviderResult result = providerClient.execute(
                    new ProviderRequest(
                            request.accountId(),
                            request.type(),
                            request.amount(),
                            request.currency()
                    )
            );

            tx = buildTransaction(request, result);

        } catch (ProviderException e) {

            logger.error(
                    "Error al comunicarse con el proveedor para accountId={}: {}",
                    request.accountId(),
                    e.getMessage()
            );

            tx = buildFailedTransaction(request);
        }
//agregar logs de error, 
        //spring security o validar ApiKey
        //agregar reintentos cuando falle conexion con el proveedor y circuitBreaker
        Transaction saved = repository.save(tx);

        return TransactionResponse.from(saved);
    }

    private void validateBusinessRules(TransactionRequest r) {
        if (r.amount().compareTo(MIN_AMOUNT) <= 0)
            throw new BusinessRuleException("El monto debe ser mayor a $1.00");
        if (r.type() == TransactionType.DEBIT && r.amount().compareTo(MAX_DEBIT) > 0)
            throw new BusinessRuleException("Una transacción DEBIT no puede exceder $10,000.00");
        if (!ALLOWED_CURRENCY.equalsIgnoreCase(r.currency()))
            throw new BusinessRuleException("Solo se aceptan transacciones en MXN");
    }

    private Transaction buildTransaction(TransactionRequest r, ProviderResult result) {
        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID().toString());
        tx.setAccountId(r.accountId());
        tx.setTransactionType(r.type());
        tx.setAmount(r.amount());
        tx.setCurrency(r.currency());
        tx.setDescription(r.description());
        tx.setCreatedAt(Instant.now());
        if (result.approved()) {
            tx.setStatus(TransactionStatus.EXECUTED);
            tx.setProviderTransactionId(result.providerTransactionId());
            tx.setBalanceAfter(result.balance());
        } else {
            tx.setStatus(TransactionStatus.REJECTED);
        }
        return tx;
    }
    private Transaction buildFailedTransaction(TransactionRequest request) {
        Transaction tx = new Transaction();

        tx.setId(UUID.randomUUID().toString());
        tx.setAccountId(request.accountId());
        tx.setTransactionType(request.type());
        tx.setAmount(request.amount());
        tx.setCurrency(request.currency());
        tx.setDescription(request.description());
        tx.setCreatedAt(Instant.now());
        tx.setStatus(TransactionStatus.REJECTED);

        return tx;
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
}