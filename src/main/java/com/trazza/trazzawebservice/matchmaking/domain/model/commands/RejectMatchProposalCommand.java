package com.trazza.trazzawebservice.matchmaking.domain.model.commands;

import java.util.UUID;

public record RejectMatchProposalCommand(Long userId, UUID matchProposalId) {
}
