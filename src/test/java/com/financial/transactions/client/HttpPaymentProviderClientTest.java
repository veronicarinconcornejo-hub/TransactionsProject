package com.financial.transactions.client;

import com.financial.transactions.dto.ProviderRequest;
import com.financial.transactions.dto.ProviderResponse;
import com.financial.transactions.dto.ProviderResult;
import com.financial.transactions.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class HttpPaymentProviderClientTest {

    @Mock
    private RestClient.Builder builder;

    @Mock
    private RestClient restClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private HttpPaymentProviderClient client;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.openMocks(this);

        when(builder.baseUrl(anyString()))
                .thenReturn(builder);

        when(builder.build())
                .thenReturn(restClient);

        client = new HttpPaymentProviderClient(
                builder,
                objectMapper,
                "http://localhost:8081",
                "ApiKeyExam"
        );
    }

    @Test
    void shouldReturnApprovedResultWhenProviderApproves() {

        ProviderRequest request = new ProviderRequest(
                "acc-aprobado",
                TransactionType.CREDIT,
                new BigDecimal("500.00"),
                "MXN"
        );

        ProviderResponse providerResponse = new ProviderResponse(
                "txn-789",
                "APPROVED",
                new BigDecimal("9500.00"),
                "2026-08-20T14:00:00Z",
                null,
                null
        );

        when(restClient.post())
                .thenReturn(requestBodyUriSpec);

        when(requestBodyUriSpec.uri("/provider/v1/execute"))
                .thenReturn(requestBodySpec);

        when(requestBodySpec.header(anyString(), anyString()))
                .thenReturn(requestBodySpec);

        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON))
                .thenReturn(requestBodySpec);

        doReturn(requestBodySpec)
                .when(requestBodySpec)
                .body(any(Object.class));

        when(requestBodySpec.retrieve())
                .thenReturn(responseSpec);

        when(responseSpec.body(ProviderResponse.class))
                .thenReturn(providerResponse);

        ProviderResult result = client.execute(request);

        assertTrue(result.approved());
        assertEquals("txn-789", result.providerTransactionId());
        assertEquals(new BigDecimal("9500.00"), result.balance());

        verify(requestBodySpec)
                .header("X-API-KEY", "ApiKeyExam");
    }
}
