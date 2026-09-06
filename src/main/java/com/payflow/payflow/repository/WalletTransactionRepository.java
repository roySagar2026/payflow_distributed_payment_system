package com.payflow.payflow.repository;

import com.payflow.payflow.model.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {
    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(UUID walletId);
}