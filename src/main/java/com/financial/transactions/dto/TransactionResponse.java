package com.financial.transactions.dto;


import com.financial.transactions.model.TransactionStatus;
import com.financial.transactions.model.TransactionType;
import com.financial.transactions.model.Transaction;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        String id,
        String accountId,
        TransactionType type,
        BigDecimal amount,
        String currency,
        String description,
        TransactionStatus status,
        String providerTransactionId,
        BigDecimal balanceAfter,
        Instant createdAt
) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getAccountId(),
                t.getTransactionType(),
                t.getAmount(),
                t.getCurrency(),
                t.getDescription(),
                t.getStatus(),
                t.getProviderTransactionId(),
                t.getBalanceAfter(),
                t.getCreatedAt()
        );
    }
}