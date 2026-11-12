package com.trazza.platform.execution.domain.services;

import com.trazza.platform.execution.domain.model.commands.AssignDriverCommand;
import com.trazza.platform.execution.domain.model.commands.CancelTripCommand;
import com.trazza.platform.execution.domain.model.commands.ConfirmTripCommand;
import com.trazza.platform.execution.domain.model.commands.DeliverTripCommand;
import com.trazza.platform.execution.domain.model.commands.PickUpTripCommand;
import com.trazza.platform.shared.domain.model.Result;

public interface TripCommandService {

    Result<TripView> handle(AssignDriverCommand command);

    Result<TripView> handle(PickUpTripCommand command);

    Result<TripView> handle(DeliverTripCommand command);

    Result<TripView> handle(ConfirmTripCommand command);

    Result<TripView> handle(CancelTripCommand command);
}
