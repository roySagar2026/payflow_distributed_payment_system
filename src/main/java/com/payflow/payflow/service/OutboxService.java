package com.payflow.payflow.service;

import com.payflow.payflow.model.OutboxEvent;
import com.payflow.payflow.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    public void saveEvent(String aggregateType, UUID aggregateId, String eventType, Object payload) {
        // Wrap payload with its type so consumers know exactly what they're deserializing
        Map<String, Object> envelope = Map.of(
                "eventType", eventType,
                "payload", payload
        );
        String json = jsonMapper.writeValueAsString(envelope);

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(json)
                .build();

        outboxEventRepository.save(event);
    }
}