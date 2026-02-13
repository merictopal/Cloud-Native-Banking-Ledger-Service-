package com.bankingledger.notificationservice.consumer;

import com.bankingledger.notificationservice.event.TransferEvent;
import com.bankingledger.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransferEventConsumer {

    private final EmailService emailService;

    /**
     * Kafka'dan transfer event'lerini dinle ve email/SMS gönder
     * Bu asenkron, non-blocking bir işlem
     */
    @KafkaListener(topics = "transfer-events", groupId = "notification-service-group")
    public void consumeTransferEvent(TransferEvent event) {
        log.info("Received transfer event for transaction: {}", event.getTransactionId());
        
        try {
            // Email gönder
            emailService.sendTransferNotification(event);
            
            // SMS simülasyonu
            log.info("SMS notification sent (simulated) to: {}", event.getRecipientPhone());
            
            log.info("Notifications sent successfully for transaction: {}", event.getTransactionId());
        } catch (Exception e) {
            log.error("Failed to process transfer event for transaction: {}", event.getTransactionId(), e);
        }
    }
}
