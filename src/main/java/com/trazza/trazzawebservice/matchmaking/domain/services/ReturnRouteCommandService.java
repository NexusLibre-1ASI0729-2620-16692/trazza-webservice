package com.trazza.trazzawebservice.matchmaking.domain.services;

import com.trazza.trazzawebservice.matchmaking.domain.model.commands.CancelReturnRouteCommand;
import com.trazza.trazzawebservice.matchmaking.domain.model.commands.CreateReturnRouteCommand;
import com.trazza.trazzawebservice.shared.domain.model.Result;

public interface ReturnRouteCommandService {

    Result<ReturnRouteView> handle(CreateReturnRouteCommand command);

    Result<ReturnRouteView> handle(CancelReturnRouteCommand command);
}
