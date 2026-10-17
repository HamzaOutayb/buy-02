package com.userservice.userservice.event;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.userservice.userservice.exception.EventPublishingException;

@Component
public class UserEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String userDeletedTopic;

    public UserEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topics.user-deleted:user.deleted}") String userDeletedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.userDeletedTopic = userDeletedTopic;
    }

    public void publishUserDeleted(String userId) {
        UserDeletedEvent event = new UserDeletedEvent(UUID.randomUUID().toString(), userId, Instant.now());
        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(userDeletedTopic, userId, message).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new EventPublishingException("Interrupted while publishing the user-deleted event", exception);
        } catch (ExecutionException | TimeoutException | JsonProcessingException exception) {
            throw new EventPublishingException("Could not publish the user-deleted event", exception);
        }
    }
}
