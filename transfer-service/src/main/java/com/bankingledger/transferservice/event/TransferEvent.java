package com.bankingledger.transferservice.event;

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
public class TransferEvent {
    private String transactionId;
    private String fromIban;
    private String toIban;
    private BigDecimal amount;
    private String status; // SUCCESS, FAILED
    private String description;
    private LocalDateTime timestamp;
    private String recipientEmail;
    private String recipientPhone;
}
