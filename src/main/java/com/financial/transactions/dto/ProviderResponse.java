package com.financial.transactions.dto;

import java.math.BigDecimal;
import java.util.Date;

public record ProviderResponse(
        String id,   // "txn-789"
        String account,          // "APPROVED" o "REJECTED"
        String TransactionType,     // 5500.00
        BigDecimal amount,      // "2025-03-15T10:30:00Z"
        String currency,            // "INSUFFICIENT_FUNDS" (solo en rechazo)
        String description,
        String status,
        String transactionId,
        BigDecimal balance,
        Date createdAt
        // el mensaje de error (solo en rechazo)
) {}