package com.financial.transactions.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    // contadores para las metricas (en memoria)
    private final AtomicInteger pagosExitosos = new AtomicInteger(0);
    private final AtomicInteger pagosFallidos = new AtomicInteger(0);

    // escucha el topic de pagos exitosos
    @KafkaListener(topics = "payment-succeeded", groupId = "metrics-group-v2")
    public void consumirExitosos(String mensaje) {
        int total = pagosExitosos.incrementAndGet();
        log.info(">>> [CONSUMER] Pago EXITOSO #{} recibido: {}", total, mensaje);

        if (mensaje.contains("9999.00")) {
            throw new RuntimeException("Error simulado para probar DLT de exitosos");
        }

    }

    // escucha el topic de pagos fallidos
    @KafkaListener(topics = "payment-failed", groupId = "metrics-group")
    public void consumirFallidos(String mensaje) {
        // simula un error para probar el DLT
        if (mensaje.contains("acc-fallo")) {
            throw new RuntimeException("Error simulado para probar DLT");
        }

        int total = pagosFallidos.incrementAndGet();
        log.warn(">>> [CONSUMER] Pago FALLIDO #{} recibido: {}", total, mensaje);
    }



    // metodos para exponer las metricas
    public int getPagosExitosos() {
        return pagosExitosos.get();
    }

    public int getPagosFallidos() {
        return pagosFallidos.get();
    }
}