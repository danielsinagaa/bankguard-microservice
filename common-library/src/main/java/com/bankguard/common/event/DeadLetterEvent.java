package com.bankguard.common.event;

public record DeadLetterEvent(
        String dlqEventId,
        String originalTopic,
        String originalKey,
        Object originalEvent,
        DeadLetterError error
) {
}
