package com.bankingledger.notificationservice.service;

import com.bankingledger.notificationservice.event.TransferEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendTransferNotification(TransferEvent event) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@bankingledger.com");
            message.setTo(event.getRecipientEmail());
            
            if ("SUCCESS".equals(event.getStatus())) {
                message.setSubject("Transfer Başarılı - Transaction ID: " + event.getTransactionId());
                message.setText(buildSuccessEmailBody(event));
            } else {
                message.setSubject("Transfer Başarısız - Transaction ID: " + event.getTransactionId());
                message.setText(buildFailureEmailBody(event));
            }

            mailSender.send(message);
            log.info("Email notification sent successfully for transaction: {}", event.getTransactionId());

        } catch (Exception e) {
            log.error("Failed to send email notification for transaction: {}", event.getTransactionId(), e);
        }
    }

    private String buildSuccessEmailBody(TransferEvent event) {
        return """
                Sayın Müşterimiz,
                
                Hesabınıza %s tutarında para transferi gerçekleştirilmiştir.
                
                Transfer Detayları:
                - İşlem ID: %s
                - Gönderici IBAN: %s
                - Alıcı IBAN: %s
                - Tutar: %s %s
                - Tarih: %s
                - Açıklama: %s
                
                İyi günler,
                Banking Ledger Sistemi
                """.formatted(
                event.getAmount(),
                event.getTransactionId(),
                maskIban(event.getFromIban()),
                maskIban(event.getToIban()),
                event.getAmount(),
                "TRY",
                event.getTimestamp(),
                event.getDescription()
        );
    }

    private String buildFailureEmailBody(TransferEvent event) {
        return """
                Sayın Müşterimiz,
                
                Hesabınızdan gerçekleştirmeye çalıştığınız transfer işlemi başarısız olmuştur.
                
                Transfer Detayları:
                - İşlem ID: %s
                - Gönderici IBAN: %s
                - Alıcı IBAN: %s
                - Tutar: %s %s
                - Tarih: %s
                - Hata: %s
                
                Lütfen müşteri hizmetleri ile iletişime geçiniz.
                
                İyi günler,
                Banking Ledger Sistemi
                """.formatted(
                event.getTransactionId(),
                maskIban(event.getFromIban()),
                maskIban(event.getToIban()),
                event.getAmount(),
                "TRY",
                event.getTimestamp(),
                event.getDescription()
        );
    }

    private String maskIban(String iban) {
        if (iban == null || iban.length() < 4) {
            return iban;
        }
        return iban.substring(0, 4) + "*".repeat(Math.max(0, iban.length() - 8)) + iban.substring(Math.max(4, iban.length() - 4));
    }
}
