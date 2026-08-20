package com.financial.transactions.dto;

import java.math.BigDecimal;

public record ProviderResponse(
        String transactionId,
        String status,
        BigDecimal balance,
        String executedAt,
        String code,
        String message
) {}