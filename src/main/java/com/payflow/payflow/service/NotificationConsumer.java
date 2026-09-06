package com.payflow.payflow.service;

import com.payflow.payflow.event.PaymentCompletedEvent;
import com.payflow.payflow.event.PaymentFailedEvent;
import com.payflow.payflow.model.Notification;
import com.payflow.payflow.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final JsonMapper jsonMapper;

    @KafkaListener(topics = "payflow.paymentorder", groupId = "payflow-service")
    @Transactional
    public void handlePaymentOrderEvent(String rawPayload) {
        JsonNode envelope = jsonMapper.readTree(rawPayload);
        String eventType = envelope.get("eventType").asString();
        JsonNode payloadNode = envelope.get("payload");

        switch (eventType) {
            case "PaymentCompleted" -> {
                PaymentCompletedEvent event = jsonMapper.treeToValue(payloadNode, PaymentCompletedEvent.class);
                handleCompleted(event);
            }
            case "PaymentFailed" -> {
                PaymentFailedEvent event = jsonMapper.treeToValue(payloadNode, PaymentFailedEvent.class);
                handleFailed(event);
            }
            default -> log.warn("Unknown event type received: {}", eventType);
        }
    }

    private void handleCompleted(PaymentCompletedEvent event) {
        String message = "Payment of ₹" + event.amount() + " completed successfully.";
        simulateSend(event.payerWalletId(), "PAYMENT_COMPLETED_SENDER", message, event.orderId());
        simulateSend(event.payeeWalletId(), "PAYMENT_RECEIVED", "You received ₹" + event.amount() + "!", event.orderId());
    }

    private void handleFailed(PaymentFailedEvent event) {
        String message = "Payment of ₹" + event.amount() + " failed: " + event.failureReason();
        simulateSend(event.payerWalletId(), "PAYMENT_FAILED", message, event.orderId());
    }

    private void simulateSend(UUID walletId, String type, String message, UUID orderId) {
        log.info("[NOTIFICATION] To wallet {}: [{}] {}", walletId, type, message);

        Notification notification = Notification.builder()
                .userWalletId(walletId)
                .type(type)
                .message(message)
                .paymentOrderId(orderId)
                .build();
        notificationRepository.save(notification);
    }
}