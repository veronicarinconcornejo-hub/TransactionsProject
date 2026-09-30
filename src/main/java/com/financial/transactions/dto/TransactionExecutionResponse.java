package com.financial.transactions.dto;

public record TransactionExecutionResponse(
        TransactionResponse transaction,
        String message
) {
}