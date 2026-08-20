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

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private PaymentProviderClient providerClient;

    @Mock
    private TransactionRepository repository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private TransactionService service;

    private TransactionRequest request(TransactionType type, String amount, String currency) {
        return new TransactionRequest("acc-123", type, new BigDecimal(amount), currency, "test");
    }

    @Test
    void rejectsAmountLessThanOrEqualToMinimum(){
        TransactionRequest req = request(TransactionType.CREDIT, "1.00", "MXN");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.execute(req));

        assertTrue(ex.getMessage().contains("mayor a $1.00"));
        verifyNoInteractions(providerClient); // valida ANTES de llamar al proveedor
    }

    @Test
    void rechazaDebitQueExcedeElLimite() {
        TransactionRequest req = request(TransactionType.DEBIT, "10001.00", "MXN");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.execute(req));

        assertTrue(ex.getMessage().contains("10,000"));
        verifyNoInteractions(providerClient);
    }

    @Test
    void permiteCreditSinLimiteMaximo() {
        TransactionRequest req = request(TransactionType.CREDIT, "50000.00", "MXN");
        when(providerClient.execute(any())).thenReturn(
                new ProviderResult(true, "txn-1", new BigDecimal("50000.00"), "INSUFFICIENT_FUNDS", null));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> service.execute(req));
    }

    @Test
    void rechazaMonedaDistintaDeMXN() {
        TransactionRequest req = request(TransactionType.CREDIT, "1500.00", "USD");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.execute(req));

        assertTrue(ex.getMessage().contains("MXN"));
        verifyNoInteractions(providerClient);
    }

    @Test
    void guardaComoExecutedCuandoElProveedorAprueba() {
        TransactionRequest req = request(TransactionType.CREDIT, "1500.00", "MXN");
        when(providerClient.execute(any())).thenReturn(
                new ProviderResult(true, "txn-789", new BigDecimal("5500.00"), "INSUFFICIENT_FUNDS", null));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse res = service.execute(req);

        assertEquals(TransactionStatus.EXECUTED, res.status());
        assertEquals("txn-789", res.providerTransactionId());
        verify(repository).save(any(Transaction.class));
    }

    @Test
    void guardaComoRejectedCuandoElProveedorRechaza() {
        TransactionRequest req = request(TransactionType.CREDIT, "1500.00", "MXN");
        when(providerClient.execute(any())).thenReturn(
                new ProviderResult(false, null, null, "INSUFFICIENT_FUNDS" , "Sin fondos"));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse res = service.execute(req);

        assertEquals(TransactionStatus.REJECTED, res.status());
        assertNull(res.providerTransactionId());
        verify(repository).save(any(Transaction.class));
    }
}
