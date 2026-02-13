package com.bankingledger.transferservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferResponse {
    private Long id;
    private String fromIban;
    private String toIban;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String transactionId;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
