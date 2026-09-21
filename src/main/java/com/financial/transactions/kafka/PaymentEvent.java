package com.financial.transactions.kafka;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentEvent(
        String eventType,
        String transactionId,
        String userId,
        String accountId,
        String type,
        BigDecimal amount,
        String currency,
        String status,
        String failureReason,
        Instant timestamp
) {}