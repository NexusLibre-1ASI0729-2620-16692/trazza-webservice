package com.trazza.trazzawebservice.execution.domain.services;

import com.trazza.trazzawebservice.execution.domain.model.commands.AssignDriverCommand;
import com.trazza.trazzawebservice.execution.domain.model.commands.CancelTripCommand;
import com.trazza.trazzawebservice.execution.domain.model.commands.ConfirmTripCommand;
import com.trazza.trazzawebservice.execution.domain.model.commands.DeliverTripCommand;
import com.trazza.trazzawebservice.execution.domain.model.commands.PickUpTripCommand;
import com.trazza.trazzawebservice.shared.domain.model.Result;

public interface TripCommandService {

    Result<TripView> handle(AssignDriverCommand command);

    Result<TripView> handle(PickUpTripCommand command);

    Result<TripView> handle(DeliverTripCommand command);

    Result<TripView> handle(ConfirmTripCommand command);

    Result<TripView> handle(CancelTripCommand command);
}
