package com.trazza.trazzawebservice.matchmaking.domain.services;

import com.trazza.trazzawebservice.matchmaking.domain.model.aggregates.Message;

import java.util.UUID;

public record MessageView(Message message, UUID senderPublicId, String senderName, boolean isMine) {
}
