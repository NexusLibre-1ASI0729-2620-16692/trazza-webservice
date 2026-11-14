package com.trazza.trazzawebservice.matchmaking.domain.model.commands;

import java.util.UUID;

public record AcceptMatchProposalCommand(Long userId, UUID matchProposalId) {
}
