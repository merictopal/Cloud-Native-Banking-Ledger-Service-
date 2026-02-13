package com.bankingledger.accountservice.service;

import com.bankingledger.accountservice.dto.AccountResponse;
import com.bankingledger.accountservice.dto.CreateAccountRequest;
import com.bankingledger.accountservice.entity.Account;
import com.bankingledger.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Creating new account for IBAN: {}", request.getIban());

        if (accountRepository.findByIban(request.getIban()).isPresent()) {
            throw new IllegalArgumentException("Account with IBAN " + request.getIban() + " already exists");
        }

        Account account = Account.builder()
                .iban(request.getIban())
                .accountHolder(request.getAccountHolder())
                .balance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO)
                .currency(request.getCurrency())
                .build();

        Account savedAccount = accountRepository.save(account);
        log.info("Account created successfully with ID: {}", savedAccount.getId());

        return AccountResponse.fromEntity(savedAccount);
    }

    public AccountResponse getAccount(String iban) {
        log.debug("Fetching account for IBAN: {}", iban);
        Account account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new RuntimeException("Account not found with IBAN: " + iban));
        return AccountResponse.fromEntity(account);
    }

    public AccountResponse getAccountById(Long id) {
        log.debug("Fetching account for ID: {}", id);
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with ID: " + id));
        return AccountResponse.fromEntity(account);
    }

    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(AccountResponse::fromEntity)
                .toList();
    }

    /**
     * KRITIK: Hesaptan para çekme (Transfer için)
     * Bu metod transaction içinde çalışmalı
     */
    public void debitAccount(String iban, BigDecimal amount) {
        log.info("Debiting amount {} from account {}", amount, iban);

        Account account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new RuntimeException("Account not found with IBAN: " + iban));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance. Current balance: " + account.getBalance());
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
        log.info("Amount debited successfully from {}", iban);
    }

    /**
     * KRITIK: Hesaba para yatırma (Transfer için)
     * Bu metod transaction içinde çalışmalı
     */
    public void creditAccount(String iban, BigDecimal amount) {
        log.info("Crediting amount {} to account {}", amount, iban);

        Account account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new RuntimeException("Account not found with IBAN: " + iban));

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        log.info("Amount credited successfully to {}", iban);
    }

    public void updateAccountStatus(String iban, Account.AccountStatus status) {
        log.info("Updating account status for IBAN: {} to {}", iban, status);

        Account account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new RuntimeException("Account not found with IBAN: " + iban));

        account.setStatus(status);
        accountRepository.save(account);
    }
}
