package com.trazza.trazzawebservice.matchmaking.domain.model.commands;

import java.util.UUID;

public record SendMessageCommand(Long userId, UUID matchProposalId, String content) {
}
