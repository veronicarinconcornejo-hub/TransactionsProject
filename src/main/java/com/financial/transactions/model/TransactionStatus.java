package com.financial.transactions.model;

public enum TransactionStatus {
    EXECUTED,   // el proveedor aprobó
    REJECTED,   // el proveedor rechazó (ej. fondos insuficientes)
}
