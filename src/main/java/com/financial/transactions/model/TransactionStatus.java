package com.financial.transactions.model;

public enum TransactionStatus {
    PENDING,
    EXECUTED,   // el proveedor aprobó
    REJECTED,
    FAILED// el proveedor rechazó (ej. fondos insuficientes)
}
