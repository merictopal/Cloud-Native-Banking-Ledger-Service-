package com.bankingledger.transferservice.controller;

import com.bankingledger.transferservice.dto.TransferRequest;
import com.bankingledger.transferservice.dto.TransferResponse;
import com.bankingledger.transferservice.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferResponse> executeTransfer(@RequestBody TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transferService.executeTransfer(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> getTransfer(@PathVariable Long id) {
        return ResponseEntity.ok(transferService.getTransferById(id));
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<TransferResponse> getTransferByTransaction(@PathVariable String transactionId) {
        return ResponseEntity.ok(transferService.getTransferByTransactionId(transactionId));
    }
}
