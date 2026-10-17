package com.userservice.userservice.event;

import java.time.Instant;

public class UserDeletedEvent {
    private final String eventId;
    private final String userId;
    private final Instant occurredAt;

    public UserDeletedEvent(String eventId, String userId, Instant occurredAt) {
        this.eventId = eventId;
        this.userId = userId;
        this.occurredAt = occurredAt;
    }

    public String getEventId() {
        return eventId;
    }

    public String getUserId() {
        return userId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
