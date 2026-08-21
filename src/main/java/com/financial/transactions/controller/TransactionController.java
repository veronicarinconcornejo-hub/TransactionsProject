package com.financial.transactions.controller;

import com.financial.transactions.dto.TransactionRequest;
import com.financial.transactions.dto.TransactionResponse;
import com.financial.transactions.model.TransactionStatus;
import com.financial.transactions.model.TransactionType;
import com.financial.transactions.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@SecurityRequirement(name = "ApiKeyAuth")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @Operation(
            summary = "Ejecutar una transacción",
            description = "Ejecuta una transacción CREDIT o DEBIT contra el proveedor externo."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Transacción ejecutada correctamente"
            )
    })

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest request) {

        TransactionResponse response = service.execute(request);

        if (response.status() == TransactionStatus.REJECTED) {
            return ResponseEntity.status(422).body(response);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Consultar transacciones",
            description = "Consulta transacciones utilizando filtros opcionales y paginación."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Consulta realizada correctamente"
            )
    })

    @GetMapping
    public Page<TransactionResponse> list(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return service.search(accountId, status, type, page, limit);
    }

    @Operation(
            summary = "Consultar todas las transacciones",
            description = "Obtiene todas las transacciones almacenadas."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Consulta realizada correctamente"
            )
    })
    @GetMapping("/all")
    public List<TransactionResponse> listAll() {
        return service.searchAll();
    }
}
