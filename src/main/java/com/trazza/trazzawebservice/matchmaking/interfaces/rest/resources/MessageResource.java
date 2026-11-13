package com.trazza.trazzawebservice.matchmaking.interfaces.rest.resources;

import java.time.Instant;
import java.util.UUID;

public record MessageResource(Long id, UUID senderId, String senderName, boolean isMine, String content, Instant sentAt,
                              Instant readAt) {
}
