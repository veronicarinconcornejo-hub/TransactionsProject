package com.financial.transactions.service;

import com.financial.transactions.client.PaymentProviderClient;
import com.financial.transactions.dto.ProviderResult;
import com.financial.transactions.dto.TransactionRequest;
import com.financial.transactions.dto.TransactionResponse;
import com.financial.transactions.exceptions.BusinessRuleException;
import com.financial.transactions.model.Transaction;
import com.financial.transactions.model.TransactionStatus;
import com.financial.transactions.model.TransactionType;
import com.financial.transactions.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.financial.transactions.client.PaymentProviderClient;
import com.financial.transactions.dto.ProviderResult;
import com.financial.transactions.dto.TransactionRequest;
import com.financial.transactions.dto.TransactionResponse;
import com.financial.transactions.exceptions.BusinessRuleException;
import com.financial.transactions.exceptions.ProviderException;
import com.financial.transactions.model.Transaction;
import com.financial.transactions.model.TransactionStatus;
import com.financial.transactions.model.TransactionType;
import com.financial.transactions.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private PaymentProviderClient providerClient;

    @Mock
    private TransactionRepository repository;

    @Mock
    private MongoTemplate mongoTemplate;

    private TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService(
                providerClient,
                repository,
                mongoTemplate
        );
    }

    @Test
    void shouldExecuteTransactionWhenProviderApproves() {

        TransactionRequest request = new TransactionRequest(
                "acc-aprobado",
                TransactionType.CREDIT,
                new BigDecimal("1500.00"),
                "MXN",
                "Transferencia recibida"
        );

        ProviderResult providerResult = new ProviderResult(
                true,
                "txn-789",
                new BigDecimal("9500.00"),
                null,
                null
        );

        when(providerClient.execute(any()))
                .thenReturn(providerResult);

        when(repository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = service.execute(request);

        assertNotNull(response);
        assertEquals(TransactionStatus.EXECUTED, response.status());
        assertEquals("txn-789", response.providerTransactionId());
        assertEquals(new BigDecimal("9500.00"), response.balanceAfter());

        verify(providerClient, times(1)).execute(any());
        verify(repository, times(1)).save(any(Transaction.class));
    }

    @Test
    void shouldRejectTransactionWhenProviderRejects() {

        TransactionRequest request = new TransactionRequest(
                "acc-rechazado",
                TransactionType.DEBIT,
                new BigDecimal("500.00"),
                "MXN",
                "Compra"
        );

        ProviderResult providerResult = new ProviderResult(
                false,
                null,
                null,
                "INSUFFICIENT_FUNDS",
                "Fondos insuficientes"
        );

        when(providerClient.execute(any()))
                .thenReturn(providerResult);

        when(repository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = service.execute(request);

        assertNotNull(response);
        assertEquals(TransactionStatus.REJECTED, response.status());
        assertNull(response.providerTransactionId());
        assertNull(response.balanceAfter());

        verify(providerClient, times(1)).execute(any());
        verify(repository, times(1)).save(any(Transaction.class));
    }

    @Test
    void shouldCreateRejectedTransactionWhenProviderFails() {

        TransactionRequest request = new TransactionRequest(
                "acc-fallo",
                TransactionType.DEBIT,
                new BigDecimal("500.00"),
                "MXN",
                "Prueba error proveedor"
        );

        when(providerClient.execute(any()))
                .thenThrow(
                        new ProviderException(
                                "No se pudo contactar al proveedor",
                                new RuntimeException()
                        )
                );

        when(repository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = service.execute(request);

        assertNotNull(response);
        assertEquals(TransactionStatus.REJECTED, response.status());

        verify(providerClient, times(1)).execute(any());
        verify(repository, times(1)).save(any(Transaction.class));
    }

    @Test
    void shouldThrowExceptionWhenAmountIsOneOrLess() {

        TransactionRequest request = new TransactionRequest(
                "acc-test",
                TransactionType.CREDIT,
                new BigDecimal("1.00"),
                "MXN",
                "Monto inválido"
        );

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.execute(request)
        );

        assertEquals(
                "El monto debe ser mayor a $1.00",
                exception.getMessage()
        );

        verifyNoInteractions(providerClient);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenDebitExceedsMaximumAmount() {

        TransactionRequest request = new TransactionRequest(
                "acc-test",
                TransactionType.DEBIT,
                new BigDecimal("10000.01"),
                "MXN",
                "Monto mayor al permitido"
        );

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.execute(request)
        );

        assertEquals(
                "Una transacción DEBIT no puede exceder $10,000.00",
                exception.getMessage()
        );

        verifyNoInteractions(providerClient);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenCurrencyIsNotMXN() {

        TransactionRequest request = new TransactionRequest(
                "acc-test",
                TransactionType.CREDIT,
                new BigDecimal("500.00"),
                "USD",
                "Moneda inválida"
        );

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.execute(request)
        );

        assertEquals(
                "Solo se aceptan transacciones en MXN",
                exception.getMessage()
        );

        verifyNoInteractions(providerClient);
        verify(repository, never()).save(any());
    }
}