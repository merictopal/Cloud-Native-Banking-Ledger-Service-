package com.bankingledger.transferservice.repository;

import com.bankingledger.transferservice.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {
    Optional<Transfer> findByTransactionId(String transactionId);
    List<Transfer> findByFromIban(String fromIban);
    List<Transfer> findByToIban(String toIban);
}
