package com.bankingledger.transferservice.client;

import com.bankingledger.transferservice.dto.AccountResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "account-service")
public interface AccountServiceClient {

    @GetMapping("/api/v1/accounts/{iban}")
    AccountResponse getAccount(@PathVariable String iban);
}
