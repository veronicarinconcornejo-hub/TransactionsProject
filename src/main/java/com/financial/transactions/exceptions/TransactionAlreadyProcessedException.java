package com.financial.transactions.exceptions;



public class TransactionAlreadyProcessedException extends RuntimeException {
    public TransactionAlreadyProcessedException(String message) {
        super(message);
    }
}