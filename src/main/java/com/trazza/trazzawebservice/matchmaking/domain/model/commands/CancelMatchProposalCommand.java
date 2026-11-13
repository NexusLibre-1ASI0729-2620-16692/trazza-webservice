package com.trazza.trazzawebservice.matchmaking.domain.model.commands;

import java.util.UUID;

public record CancelMatchProposalCommand(Long userId, UUID matchProposalId) {
}
