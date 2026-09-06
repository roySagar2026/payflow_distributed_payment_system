package com.payflow.payflow.service;

import com.payflow.payflow.model.*;
import com.payflow.payflow.dto.*;
import com.payflow.payflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final RefundRepository refundRepository;
    private final WalletRepository walletRepository;
    private final TransferService transferService;

    @Transactional
    public RefundResponse refund(UUID orderId, RefundRequest request) {
        PaymentOrder order = paymentOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment order not found"));

        // This check reuses the exact state machine from Step 8 — refund is only
        // ever valid from COMPLETED. Trying to refund a PENDING or FAILED order
        // will throw here automatically, since transitionTo() rejects invalid moves.
        order.transitionTo(PaymentOrderStatus.REFUNDED);

        Wallet payerWallet = walletRepository.findById(order.getPayerWalletId())
                .orElseThrow(() -> new IllegalStateException("Payer wallet not found"));
        Wallet payeeWallet = walletRepository.findById(order.getPayeeWalletId())
                .orElseThrow(() -> new IllegalStateException("Payee wallet not found"));

        // The refund moves money the OPPOSITE direction: payee -> payer
        // We reuse TransferService directly since it's just a normal wallet-to-wallet
        // movement — no need to duplicate the locking/ledger logic.
        var payeeUserId = getUserIdForWallet(payeeWallet);
        var payerUserId = getUserIdForWallet(payerWallet);

        var reverseTransferRequest = new com.payflow.payflow.dto.TransferRequest(payerUserId, order.getAmount());
        TransferResponse reverseTransfer = transferService.transfer(payeeUserId, reverseTransferRequest);

        paymentOrderRepository.save(order);

        Refund refund = Refund.builder()
                .originalPaymentOrderId(order.getId())
                .refundTransferId(reverseTransfer.transferId())
                .amount(order.getAmount())
                .reason(request.reason())
                .status(RefundStatus.COMPLETED)
                .build();
        refund = refundRepository.save(refund);

        return new RefundResponse(refund.getId(), order.getId(), refund.getAmount(), refund.getStatus().name());
    }

    private UUID getUserIdForWallet(Wallet wallet) {
        return wallet.getUserId();
    }
}