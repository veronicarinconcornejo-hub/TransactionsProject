package com.financial.transactions.model;

public enum TransactionStatus {
    EXECUTED,   // el proveedor aprobó
    REJECTED,
    FAILED// el proveedor rechazó (ej. fondos insuficientes)
}
