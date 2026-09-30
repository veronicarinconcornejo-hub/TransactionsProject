package com.financial.transactions.exceptions;

/*
 * [NUEVO] ARCHIVO COMPLETO
 * Se lanza cuando llega una petición con una llave que otra petición
 * todavía está procesando (estado PENDING). El handler la convierte en 409.
 */
public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String message) {
        super(message);
    }
}