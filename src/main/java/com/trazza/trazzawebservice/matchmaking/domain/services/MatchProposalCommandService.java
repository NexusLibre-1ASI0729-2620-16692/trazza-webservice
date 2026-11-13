package com.trazza.trazzawebservice.matchmaking.domain.services;

import com.trazza.trazzawebservice.matchmaking.domain.model.commands.AcceptMatchProposalCommand;
import com.trazza.trazzawebservice.matchmaking.domain.model.commands.CancelMatchProposalCommand;
import com.trazza.trazzawebservice.matchmaking.domain.model.commands.CreateMatchProposalCommand;
import com.trazza.trazzawebservice.matchmaking.domain.model.commands.RejectMatchProposalCommand;
import com.trazza.trazzawebservice.matchmaking.domain.model.commands.SendMessageCommand;
import com.trazza.trazzawebservice.shared.domain.model.Result;

public interface MatchProposalCommandService {

    Result<MatchProposalView> handle(CreateMatchProposalCommand command);

    Result<MatchProposalView> handle(AcceptMatchProposalCommand command);

    Result<MatchProposalView> handle(RejectMatchProposalCommand command);

    Result<MatchProposalView> handle(CancelMatchProposalCommand command);

    Result<MessageView> handle(SendMessageCommand command);
}
