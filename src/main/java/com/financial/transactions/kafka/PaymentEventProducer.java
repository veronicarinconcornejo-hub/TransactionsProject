package com.financial.transactions.kafka;

import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventProducer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventProducer.class);
    private static final String TOPIC_SUCCESS = "payment-succeeded";
    private static final String TOPIC_FAILED = "payment-failed";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PaymentEventProducer(KafkaTemplate<String, String> kafkaTemplate,
                                ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishSuccess(PaymentEvent event) {
        publish(TOPIC_SUCCESS, event);
    }

    public void publishFailure(PaymentEvent event) {
        publish(TOPIC_FAILED, event);
    }

    private void publish(String topic, PaymentEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, event.transactionId(), json);
            log.info("Evento publicado en {}: {}", topic, event.transactionId());
        } catch (Exception e) {
            log.error("Error al publicar evento en Kafka: {}", e.getMessage());
        }
    }
}