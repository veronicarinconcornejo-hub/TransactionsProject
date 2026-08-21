package com.financial.transactions.controller;

import com.financial.transactions.dto.TransactionRequest;
import com.financial.transactions.dto.TransactionResponse;
import com.financial.transactions.model.TransactionStatus;
import com.financial.transactions.model.TransactionType;
import com.financial.transactions.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionControllerTest {

    @Mock
    private TransactionService service;

    private TransactionController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        controller = new TransactionController(service);
    }

    @Test
    void shouldReturnCreatedWhenTransactionIsExecuted() {

        TransactionRequest request = new TransactionRequest(
                "acc-aprobado",
                TransactionType.CREDIT,
                new BigDecimal("500.00"),
                "MXN",
                "Transferencia"
        );

        TransactionResponse response = new TransactionResponse(
                "id-1",
                "acc-aprobado",
                TransactionType.CREDIT,
                new BigDecimal("500.00"),
                "MXN",
                "Transferencia",
                TransactionStatus.EXECUTED,
                "txn-789",
                new BigDecimal("9500.00"),
                Instant.now()
        );

        when(service.execute(request))
                .thenReturn(response);

        ResponseEntity<TransactionResponse> result =
                controller.create(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());

        verify(service).execute(request);
    }

    @Test
    void shouldReturn422WhenTransactionIsRejected() {

        TransactionRequest request = new TransactionRequest(
                "acc-rechazado",
                TransactionType.DEBIT,
                new BigDecimal("500.00"),
                "MXN",
                "Compra"
        );

        TransactionResponse response = new TransactionResponse(
                "id-2",
                "acc-rechazado",
                TransactionType.DEBIT,
                new BigDecimal("500.00"),
                "MXN",
                "Compra",
                TransactionStatus.REJECTED,
                null,
                null,
                Instant.now()
        );

        when(service.execute(request))
                .thenReturn(response);

        ResponseEntity<TransactionResponse> result =
                controller.create(request);

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void shouldReturnTransactionsPage() {

        TransactionResponse response = new TransactionResponse(
                "id-1",
                "userId1",
                TransactionType.CREDIT,
                new BigDecimal("155.00"),
                "MXN",
                "Transferencia",
                TransactionStatus.EXECUTED,
                "txn-789",
                new BigDecimal("9500.00"),
                Instant.now()
        );

        PageImpl<TransactionResponse> page =
                new PageImpl<>(List.of(response));

        when(service.search(
                "userId1",
                TransactionStatus.EXECUTED,
                TransactionType.CREDIT,
                0,
                20
        )).thenReturn(page);

        var result = controller.list(
                "userId1",
                TransactionStatus.EXECUTED,
                TransactionType.CREDIT,
                0,
                20
        );

        assertEquals(1, result.getTotalElements());
        assertEquals(response, result.getContent().get(0));

        verify(service).search(
                "userId1",
                TransactionStatus.EXECUTED,
                TransactionType.CREDIT,
                0,
                20
        );
    }
}