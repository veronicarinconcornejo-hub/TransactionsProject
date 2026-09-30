package com.financial.transactions.exceptions;

/*
 * [NUEVO] ARCHIVO COMPLETO
 * Se lanza cuando reutilizan una llave de idempotencia con datos diferentes
 * (otro monto, otra cuenta, etc.). El handler la convierte en 422.
 */
public class IdempotencyKeyMismatchException extends RuntimeException {
    public IdempotencyKeyMismatchException(String message) {
        super(message);
    }
}