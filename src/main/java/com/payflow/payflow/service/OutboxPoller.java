package com.payflow.payflow.service;

import com.payflow.payflow.model.OutboxEvent;
import com.payflow.payflow.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String TOPIC_PREFIX = "payflow.";

    @Scheduled(fixedDelay = 2000) // poll every 2 seconds
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findTop50ByPublishedFalseOrderByCreatedAtAsc();

        for (OutboxEvent event : pending) {
            try {
                String topic = TOPIC_PREFIX + event.getAggregateType().toLowerCase();
                kafkaTemplate.send(topic, event.getAggregateId().toString(), event.getPayload()).get();

                event.setPublished(true);
                event.setPublishedAt(Instant.now());
                outboxEventRepository.save(event);

                log.info("Published outbox event {} of type {}", event.getId(), event.getEventType());
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}, will retry next poll", event.getId(), e);
                // Intentionally don't mark as published — it'll be retried on the next poll cycle
            }
        }
    }
}