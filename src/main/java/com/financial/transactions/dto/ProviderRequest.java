package com.financial.transactions.dto;

import com.financial.transactions.model.TransactionType;

import java.math.BigDecimal;

//es lo que TU API le manda al proveedor externo.
// Es lo que sale de tu servicio hacia afuera. Por eso:
//
//No tiene validaciones → porque esos datos ya los validaste tú antes.
// Cuando construyes el ProviderRequest, ya pasaron por tus reglas de negocio,
// así que son datos limpios y confiables. No hay que validarlos otra vez.
public record ProviderRequest(
        String accountId,
        TransactionType type,
        BigDecimal amount,
        String currency

) {}
