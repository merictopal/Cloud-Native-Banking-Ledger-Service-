package com.bankingledger.transferservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequest {
    private String fromIban;
    private String toIban;
    private BigDecimal amount;
    private String currency;
    private String description;
}
