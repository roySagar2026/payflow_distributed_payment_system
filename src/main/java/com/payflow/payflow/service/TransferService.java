package com.payflow.payflow.service;

import com.payflow.payflow.model.*;
import com.payflow.payflow.dto.*;
import com.payflow.payflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final WalletRepository walletRepository;
    private final TransferRepository transferRepository;
    private final WalletTransactionRepository transactionRepository;

    @Transactional
    public TransferResponse transfer(UUID senderUserId, TransferRequest request) {
        Wallet senderWallet = walletRepository.findByUserId(senderUserId)
                .orElseThrow(() -> new IllegalArgumentException("Sender wallet not found"));

        Wallet receiverWallet = walletRepository.findByUserId(request.receiverUserId())
                .orElseThrow(() -> new IllegalArgumentException("Receiver wallet not found"));

        if (senderWallet.getId().equals(receiverWallet.getId())) {
            throw new IllegalArgumentException("Cannot transfer to your own wallet");
        }

        // CRITICAL: always lock wallets in a CONSISTENT ORDER (e.g., by ID) to prevent deadlocks.
        // If two transfers happen at once — A→B and B→A — without ordering, they could each
        // lock one wallet and wait forever for the other. Locking by sorted ID order eliminates this.
        Wallet firstLock, secondLock;
        boolean senderFirst = senderWallet.getId().compareTo(receiverWallet.getId()) < 0;
        if (senderFirst) {
            firstLock = walletRepository.findByIdForUpdate(senderWallet.getId()).orElseThrow();
            secondLock = walletRepository.findByIdForUpdate(receiverWallet.getId()).orElseThrow();
        } else {
            firstLock = walletRepository.findByIdForUpdate(receiverWallet.getId()).orElseThrow();
            secondLock = walletRepository.findByIdForUpdate(senderWallet.getId()).orElseThrow();
        }

        Wallet lockedSender = senderFirst ? firstLock : secondLock;
        Wallet lockedReceiver = senderFirst ? secondLock : firstLock;

        if (lockedSender.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        // Perform the actual movement
        lockedSender.setBalance(lockedSender.getBalance().subtract(request.amount()));
        lockedReceiver.setBalance(lockedReceiver.getBalance().add(request.amount()));

        walletRepository.save(lockedSender);
        walletRepository.save(lockedReceiver);

        // Record the transfer itself
        Transfer transfer = Transfer.builder()
                .senderWalletId(lockedSender.getId())
                .receiverWalletId(lockedReceiver.getId())
                .amount(request.amount())
                .status(TransferStatus.SUCCESS)
                .build();
        transfer = transferRepository.save(transfer);

        // Double-entry: one row for the debit, one for the credit — both linked to the same transferId
        WalletTransaction debit = WalletTransaction.builder()
                .walletId(lockedSender.getId())
                .type(TransactionType.TRANSFER_OUT)
                .amount(request.amount())
                .balanceAfter(lockedSender.getBalance())
                .status(TransactionStatus.SUCCESS)
                .transferId(transfer.getId())
                .counterpartyWalletId(lockedReceiver.getId())
                .build();

        WalletTransaction credit = WalletTransaction.builder()
                .walletId(lockedReceiver.getId())
                .type(TransactionType.TRANSFER_IN)
                .amount(request.amount())
                .balanceAfter(lockedReceiver.getBalance())
                .status(TransactionStatus.SUCCESS)
                .transferId(transfer.getId())
                .counterpartyWalletId(lockedSender.getId())
                .build();

        transactionRepository.save(debit);
        transactionRepository.save(credit);

        return new TransferResponse(
                transfer.getId(),
                lockedSender.getId(),
                lockedReceiver.getId(),
                request.amount(),
                lockedSender.getBalance()
        );
    }
}