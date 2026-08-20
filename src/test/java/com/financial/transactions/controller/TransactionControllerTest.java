package com.financial.transactions.controller;

import com.financial.transactions.dto.TransactionRequest;
import com.financial.transactions.dto.TransactionResponse;
import com.financial.transactions.model.TransactionStatus;
import com.financial.transactions.model.TransactionType;
import com.financial.transactions.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService service;

    @InjectMocks
    private TransactionController controller;

    private TransactionRequest request() {
        return new TransactionRequest("acc-123", TransactionType.CREDIT,
                new BigDecimal("1500.00"), "MXN", "test");
    }

    private TransactionResponse responseCon(TransactionStatus status) {
        return new TransactionResponse("id-1", "acc-123", TransactionType.CREDIT,
                new BigDecimal("1500.00"), "MXN", "test", status,
                "txn-789", new BigDecimal("5500.00"), Instant.now());
    }

    @Test
    void devuelve201CuandoLaTransaccionSeEjecuta() {
        when(service.execute(any())).thenReturn(responseCon(TransactionStatus.EXECUTED));

        ResponseEntity<TransactionResponse> resp = controller.create(request());

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
    }

    @Test
    void devuelve422CuandoLaTransaccionEsRechazada() {
        when(service.execute(any())).thenReturn(responseCon(TransactionStatus.REJECTED));

        ResponseEntity<TransactionResponse> resp = controller.create(request());

        assertEquals(422, resp.getStatusCode().value());
    }
}