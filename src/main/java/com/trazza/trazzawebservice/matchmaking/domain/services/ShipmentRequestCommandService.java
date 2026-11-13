package com.trazza.trazzawebservice.matchmaking.domain.services;

import com.trazza.trazzawebservice.matchmaking.domain.model.commands.CancelShipmentRequestCommand;
import com.trazza.trazzawebservice.matchmaking.domain.model.commands.CreateShipmentRequestCommand;
import com.trazza.trazzawebservice.shared.domain.model.Result;

public interface ShipmentRequestCommandService {

    Result<ShipmentRequestView> handle(CreateShipmentRequestCommand command);

    Result<ShipmentRequestView> handle(CancelShipmentRequestCommand command);
}
