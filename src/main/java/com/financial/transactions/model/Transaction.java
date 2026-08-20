package com.financial.transactions.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@Document(collection = "transactions")
public class Transaction {
    @Id
    @Indexed
    private String id;

    @Indexed
    private String accountId;

    private TransactionType transactionType;

    private BigDecimal amount;// BigDecimal para dinero, nunca double

    private String currency; //moneda

    private String description;

    @Indexed
    private TransactionStatus status;

    private String providerTransactionId;

    private BigDecimal balanceAfter;

    private Instant createdAt;

}
