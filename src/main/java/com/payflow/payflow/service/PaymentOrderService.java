package com.payflow.payflow.service;

import com.payflow.payflow.model.*;
import com.payflow.payflow.dto.*;
import com.payflow.payflow.event.PaymentCompletedEvent;
import com.payflow.payflow.event.PaymentFailedEvent;
import com.payflow.payflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentOrderService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final WalletRepository walletRepository;
    private final TransferService transferService;
    private final FraudCheckService fraudCheckService;
    private final OutboxService outboxService;

    @Transactional
    public PaymentOrderResponse createAndProcess(UUID payerUserId, CreatePaymentOrderRequest request) {
        Wallet payerWallet = walletRepository.findByUserId(payerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Payer wallet not found"));

        Wallet payeeWallet = walletRepository.findByUserId(request.payeeUserId())
                .orElseThrow(() -> new IllegalArgumentException("Payee wallet not found"));

        if (payerWallet.getId().equals(payeeWallet.getId())) {
            throw new IllegalArgumentException("Cannot pay yourself");
        }

        // Step 1: CREATED
        PaymentOrder order = PaymentOrder.builder()
                .payerWalletId(payerWallet.getId())
                .payeeWalletId(payeeWallet.getId())
                .amount(request.amount())
                .status(PaymentOrderStatus.CREATED)
                .build();
        order = paymentOrderRepository.save(order);

        // Step 2: CREATED -> PENDING (basic validation passed)
        order.transitionTo(PaymentOrderStatus.PENDING);
        paymentOrderRepository.save(order);

        // Step 3: Fraud check happens here, before PROCESSING
        FraudDecision fraudDecision = fraudCheckService.evaluate(order);

        if (fraudDecision == FraudDecision.REJECTED) {
            order.setFailureReason("Rejected by fraud check");
            order.transitionTo(PaymentOrderStatus.FAILED);
            paymentOrderRepository.save(order);

            outboxService.saveEvent(
                    "PaymentOrder",
                    order.getId(),
                    "PaymentFailed",
                    new PaymentFailedEvent(
                            order.getId(), order.getPayerWalletId(), order.getPayeeWalletId(),
                            order.getAmount(), order.getFailureReason())
            );

            throw new IllegalArgumentException("Payment rejected due to fraud risk");
        }

        // Step 4: PENDING -> PROCESSING (fraud check passed)
        order.transitionTo(PaymentOrderStatus.PROCESSING);
        paymentOrderRepository.save(order);

        // Step 5: Attempt the actual money movement
        try {
            var transferRequest = new TransferRequest(request.payeeUserId(), request.amount());
            TransferResponse transferResult = transferService.transfer(payerUserId, transferRequest);

            order.setTransferId(transferResult.transferId());
            order.transitionTo(PaymentOrderStatus.COMPLETED);
            paymentOrderRepository.save(order);

            outboxService.saveEvent(
                    "PaymentOrder",
                    order.getId(),
                    "PaymentCompleted",
                    new PaymentCompletedEvent(
                            order.getId(), order.getPayerWalletId(), order.getPayeeWalletId(),
                            order.getAmount(), transferResult.transferId())
            );

        } catch (Exception e) {
            order.setFailureReason(e.getMessage());
            order.transitionTo(PaymentOrderStatus.FAILED);
            paymentOrderRepository.save(order);

            outboxService.saveEvent(
                    "PaymentOrder",
                    order.getId(),
                    "PaymentFailed",
                    new PaymentFailedEvent(
                            order.getId(), order.getPayerWalletId(), order.getPayeeWalletId(),
                            order.getAmount(), e.getMessage())
            );

            throw e; // re-throw so the caller/HTTP response reflects the failure
        }

        return toResponse(order);
    }

    public PaymentOrderResponse getById(UUID orderId) {
        PaymentOrder order = paymentOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment order not found"));
        return toResponse(order);
    }

    private PaymentOrderResponse toResponse(PaymentOrder order) {
        return new PaymentOrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getAmount(),
                order.getTransferId(),
                order.getFailureReason(),
                order.getCreatedAt()
        );
    }
}