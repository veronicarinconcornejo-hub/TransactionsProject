package com.financial.transactions.repository.Implement;

import com.financial.transactions.dto.ProviderRequest;
import com.financial.transactions.dto.ProviderResponse;
import com.financial.transactions.dto.ProviderResult;
import com.financial.transactions.repository.PaymentProviderClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import tools.jackson.databind.ObjectMapper;
import com.financial.transactions.exceptions.ProviderException;


@Component
public class HttpPaymentProviderClient implements PaymentProviderClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public HttpPaymentProviderClient(RestClient.Builder builder, ObjectMapper objectMapper,
                                     @Value("${provider.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public ProviderResult execute(ProviderRequest request) {
        try {
            ProviderResponse resp = restClient.post()
                    .uri("/provider/v1/execute")
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