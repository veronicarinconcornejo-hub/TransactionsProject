package com.financial.transactions.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(String message,
                                String code,
                                String description,
                                String status
    ) {}

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException e) {
        return ResponseEntity.status(422)
                .body(new ErrorResponse(
                        e.getMessage(),
                        "BUSINESS_RULE_VIOLATION",
                        e.getMessage(),
                        "422 UNPROCESSABLE_ENTITY"
                ));
    }

    @ExceptionHandler(ProviderException.class)
    public ResponseEntity<ErrorResponse> handleProvider(ProviderException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(
                        e.getMessage(),
                        "PROVIDER_ERROR",
                        e.getMessage(),
                        "502 BAD_GATEWAY"
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        msg,
                        "VALIDATION_ERROR",
                        msg,
                        "400 BAD_REQUEST"
                ));
    }

    // 409: la primera petición con esta llave todavía no termina
    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyConflict(IdempotencyConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        e.getMessage(),
                        "IDEMPOTENCY_IN_PROGRESS",
                        e.getMessage(),
                        "409 CONFLICT"
                ));
    }

    // 422: la llave se reutilizó con datos diferentes
    @ExceptionHandler(IdempotencyKeyMismatchException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyMismatch(IdempotencyKeyMismatchException e) {
        return ResponseEntity.status(422)
                .body(new ErrorResponse(
                        e.getMessage(),
                        "IDEMPOTENCY_KEY_REUSED",
                        e.getMessage(),
                        "422 UNPROCESSABLE_ENTITY"
                ));
    }

    // 409: la transacción con esta llave YA fue procesada (replay)
    @ExceptionHandler(TransactionAlreadyProcessedException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyProcessed(TransactionAlreadyProcessedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        e.getMessage(),
                        "TRANSACTION_ALREADY_PROCESSED",
                        e.getMessage(),
                        "409 CONFLICT"
                ));
    }
}