package com.financial.transactions.dto;

import java.math.BigDecimal;

public record ProviderResult(
        boolean approved,              // ¿el proveedor aprobó la transacción?
        String providerTransactionId, // el "txn-789" que devuelve
        BigDecimal balance,            // el saldo que retorna el proveedor
        String rejectionCode,          // ej. "INSUFFICIENT_FUNDS" (null si fue aprobada)
        String rejectionMessage        // el mensaje de error (null si fue aprobada)
) {}