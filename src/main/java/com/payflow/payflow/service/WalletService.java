package com.payflow.payflow.service;

import com.payflow.payflow.model.*;
import com.payflow.payflow.dto.*;
import com.payflow.payflow.repository.WalletRepository;
import com.payflow.payflow.repository.WalletTransactionRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    private static final int MAX_RETRIES = 3;

    public WalletResponse deposit(UUID userId, DepositRequest request) {
        return retryOnConflict(() -> doDeposit(userId, request.amount()));
    }

    public WalletResponse withdraw(UUID userId, WithdrawRequest request) {
        return retryOnConflict(() -> doWithdraw(userId, request.amount()));
    }

    @Transactional
    protected WalletResponse doDeposit(UUID userId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet); // @Version check happens here on flush

        recordTransaction(wallet.getId(), TransactionType.DEPOSIT, amount, wallet.getBalance());

        return new WalletResponse(wallet.getId(), wallet.getBalance(), wallet.getCurrency());
    }

    @Transactional
    protected WalletResponse doWithdraw(UUID userId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet); // @Version check happens here on flush

        recordTransaction(wallet.getId(), TransactionType.WITHDRAWAL, amount, wallet.getBalance());

        return new WalletResponse(wallet.getId(), wallet.getBalance(), wallet.getCurrency());
    }

    private void recordTransaction(UUID walletId, TransactionType type, BigDecimal amount, BigDecimal balanceAfter) {
        WalletTransaction txn = WalletTransaction.builder()
                .walletId(walletId)
                .type(type)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .status(TransactionStatus.SUCCESS)
                .build();
        transactionRepository.save(txn);
    }

    /**
     * Retries the operation if an optimistic lock conflict occurs —
     * meaning another concurrent request modified the same wallet first.
     * This is what makes concurrent deposits/withdrawals SAFE without
     * blocking everyone with a database-level lock.
     */
    private WalletResponse retryOnConflict(java.util.function.Supplier<WalletResponse> action) {
        int attempts = 0;
        while (true) {
            try {
                return action.get();
            } catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
                attempts++;
                if (attempts >= MAX_RETRIES) {
                    throw new IllegalStateException("Could not complete transaction due to high contention, please retry");
                }
                // small backoff before retrying — real systems add jitter here
                try { Thread.sleep(20L * attempts); } catch (InterruptedException ignored) {}
            }
        }
    }
}