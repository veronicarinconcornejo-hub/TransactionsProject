package com.financial.transactions.dto;
import com.financial.transactions.model.TransactionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

//es lo que el cliente manda a mi api
//Es la puerta de entrada de tu servicio. Por eso tiene:
//
//Validaciones (@NotBlank, @NotNull, @DecimalMin) → porque son datos que vienen de afuera y no confías en ellos.
public record TransactionRequest(
        @NotBlank(message = "accountId es obligatorio")
        String accountId,

        @NotNull(message = "type es obligatorio")
        TransactionType type,

        @NotNull @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal amount,

        @NotBlank(message = "currency es obligatorio")
        String currency,
        String description

        ) { }
