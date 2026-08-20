package com.financial.transactions.controller;

import com.financial.transactions.dto.TransactionRequest;
import com.financial.transactions.dto.TransactionResponse;
import com.financial.transactions.model.TransactionStatus;
import com.financial.transactions.model.TransactionType;
import com.financial.transactions.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = service.execute(request);

        if (response.status() == TransactionStatus.REJECTED) {
            return ResponseEntity.status(422).body(response);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping
    public Page<TransactionResponse> list(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return service.search(accountId, status, type, page, limit);
    }
}
