package com.financial.transactions.dto;

public record TransactionExecutionResult(
        TransactionResponse transaction,
        boolean idempotentReplay
) {
}