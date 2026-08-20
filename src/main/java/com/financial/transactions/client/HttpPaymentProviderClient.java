package com.financial.transactions.client;

import com.financial.transactions.config.ApiKeyFilter;
import com.financial.transactions.dto.ProviderRequest;
import com.financial.transactions.dto.ProviderResponse;
import com.financial.transactions.dto.ProviderResult;
import com.financial.transactions.exceptions.ProviderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Component
public class HttpPaymentProviderClient implements PaymentProviderClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String providerApiKey;
    private static final Logger logger = LoggerFactory.getLogger(HttpPaymentProviderClient.class);

    public HttpPaymentProviderClient(RestClient.Builder builder, ObjectMapper objectMapper,
                                     @Value("${provider.base-url}") String baseUrl,
                                     @Value("${provider.api-key}") String providerApiKey) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.providerApiKey = providerApiKey;
    }

    @Retry(name = "paymentProvider")
    @CircuitBreaker(name = "paymentProvider")
    @Override
    public ProviderResult execute(ProviderRequest request) {

        logger.info(
                "Intentando ejecutar transacción con proveedor para accountId={}",
                request.accountId()
        );

        try {
            ProviderResponse resp = restClient.post()
                    .uri("/provider/v1/execute")
                    .header("X-API-KEY", providerApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ProviderResponse.class);
            if (resp == null) {
                throw new ProviderException("El proveedor devolvió una respuesta vacía", null);
            }
            return new ProviderResult(true, resp.transactionId(), resp.balance(), null, null);

        } catch (RestClientResponseException e) {   // 4xx / 5xx con cuerpo de rechazo
            ProviderResponse error = parseError(e.getResponseBodyAsString());
            if (error != null && "REJECTED".equalsIgnoreCase(error.status())) {
                return new ProviderResult(false, null, null, error.code(), error.message());
            }
            throw new ProviderException("Respuesta inesperada del proveedor", e);

        } catch (ResourceAccessException e) {        // timeout / red
            throw new ProviderException("No se pudo contactar al proveedor", e);
        }
    }

    private ProviderResponse parseError(String body) {
        try { return objectMapper.readValue(body, ProviderResponse.class); }
        catch (Exception ex) { return null; }
    }
}