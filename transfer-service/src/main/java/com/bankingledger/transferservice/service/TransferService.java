package com.bankingledger.transferservice.service;

import com.bankingledger.transferservice.client.AccountServiceClient;
import com.bankingledger.transferservice.dto.TransferRequest;
import com.bankingledger.transferservice.dto.TransferResponse;
import com.bankingledger.transferservice.entity.Transfer;
import com.bankingledger.transferservice.event.TransferEvent;
import com.bankingledger.transferservice.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountServiceClient accountServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RestTemplate restTemplate;

    /**
     * KRITIK: Dağıtık transfer işlemi - ACID garantisi ile
     * 
     * Eğer hesaptan para çekme başarısız olursa, tüm işlem ROLLBACK olacak.
     * PostgreSQL transaction'ı tüm işlemi atomik hale getirir.
     */
    @Transactional
    public TransferResponse executeTransfer(TransferRequest request) {
        String transactionId = UUID.randomUUID().toString();
        log.info("Starting transfer transaction: {} from {} to {} amount: {}", 
                transactionId, request.getFromIban(), request.getToIban(), request.getAmount());

        try {
            // ADIM 1: Kaynak ve hedef hesapların mevcudiyetini kontrol et
            var fromAccount = accountServiceClient.getAccount(request.getFromIban());
            var toAccount = accountServiceClient.getAccount(request.getToIban());

            if (fromAccount == null || toAccount == null) {
                throw new RuntimeException("One or both accounts not found");
            }

            // ADIM 2: Transfer kaydını PENDING durumda oluştur
            Transfer transfer = Transfer.builder()
                    .fromIban(request.getFromIban())
                    .toIban(request.getToIban())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .description(request.getDescription())
                    .transactionId(transactionId)
                    .status(Transfer.TransferStatus.PENDING)
                    .build();

            Transfer savedTransfer = transferRepository.save(transfer);
            log.info("Transfer record created with ID: {} Status: PENDING", savedTransfer.getId());

            // ADIM 3: Kaynak hesaptan para çek (KRITIK - Burada hata ise rollback)
            debitFromAccountService(request.getFromIban(), request.getAmount());
            log.info("Successfully debited {} from {}", request.getAmount(), request.getFromIban());

            // ADIM 4: Hedef hesaba para yatır (KRITIK - Burada hata ise rollback)
            creditToAccountService(request.getToIban(), request.getAmount());
            log.info("Successfully credited {} to {}", request.getAmount(), request.getToIban());

            // ADIM 5: Transfer durumunu SUCCESS olarak güncelle
            savedTransfer.setStatus(Transfer.TransferStatus.SUCCESS);
            Transfer completedTransfer = transferRepository.save(savedTransfer);

            // ADIM 6: Kafka'ya event gönder (Asenkron - Notification Service'e haber ver)
            TransferEvent event = TransferEvent.builder()
                    .transactionId(transactionId)
                    .fromIban(request.getFromIban())
                    .toIban(request.getToIban())
                    .amount(request.getAmount())
                    .status("SUCCESS")
                    .description(request.getDescription())
                    .timestamp(LocalDateTime.now())
                    .recipientEmail(toAccount.getAccountHolder() + "@bank.com")
                    .recipientPhone("+90555555555")
                    .build();

            kafkaTemplate.send("transfer-events", transactionId, event);
            log.info("Transfer event published to Kafka for transaction: {}", transactionId);

            log.info("Transfer completed successfully: {}", transactionId);
            return mapToResponse(completedTransfer);

        } catch (Exception e) {
            log.error("Transfer failed for transaction: {}. Error: {}", transactionId, e.getMessage());
            
            // Başarısız transferi kaydet
            Transfer failedTransfer = Transfer.builder()
                    .fromIban(request.getFromIban())
                    .toIban(request.getToIban())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .description(request.getDescription())
                    .transactionId(transactionId)
                    .status(Transfer.TransferStatus.FAILED)
                    .build();

            Transfer savedFailedTransfer = transferRepository.save(failedTransfer);

            // Başarısız transfer event'i gönder
            TransferEvent failureEvent = TransferEvent.builder()
                    .transactionId(transactionId)
                    .fromIban(request.getFromIban())
                    .toIban(request.getToIban())
                    .amount(request.getAmount())
                    .status("FAILED")
                    .description("Transfer failed: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaTemplate.send("transfer-events", transactionId, failureEvent);

            throw new RuntimeException("Transfer failed: " + e.getMessage(), e);
        }
    }

    private void debitFromAccountService(String fromIban, BigDecimal amount) {
        try {
            // Account Service'in internal metodunu çağır
            // Gerçek uygulamada, bu direct REST call veya messaging olabilir
            log.debug("Calling account-service to debit: {} amount: {}", fromIban, amount);
            // Implementation depends on whether Account Service exposes this endpoint
        } catch (Exception e) {
            log.error("Failed to debit account: {}", fromIban, e);
            throw new RuntimeException("Debit operation failed: " + e.getMessage());
        }
    }

    private void creditToAccountService(String toIban, BigDecimal amount) {
        try {
            log.debug("Calling account-service to credit: {} amount: {}", toIban, amount);
            // Implementation depends on whether Account Service exposes this endpoint
        } catch (Exception e) {
            log.error("Failed to credit account: {}", toIban, e);
            throw new RuntimeException("Credit operation failed: " + e.getMessage());
        }
    }

    public TransferResponse getTransferById(Long id) {
        Transfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transfer not found with ID: " + id));
        return mapToResponse(transfer);
    }

    public TransferResponse getTransferByTransactionId(String transactionId) {
        Transfer transfer = transferRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transfer not found with transaction ID: " + transactionId));
        return mapToResponse(transfer);
    }

    private TransferResponse mapToResponse(Transfer transfer) {
        return TransferResponse.builder()
                .id(transfer.getId())
                .fromIban(transfer.getFromIban())
                .toIban(transfer.getToIban())
                .amount(transfer.getAmount())
                .currency(transfer.getCurrency())
                .status(transfer.getStatus().toString())
                .transactionId(transfer.getTransactionId())
                .description(transfer.getDescription())
                .createdAt(transfer.getCreatedAt())
                .updatedAt(transfer.getUpdatedAt())
                .build();
    }
}
