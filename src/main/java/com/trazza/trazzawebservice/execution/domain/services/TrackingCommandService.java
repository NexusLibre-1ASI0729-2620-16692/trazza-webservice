package com.trazza.trazzawebservice.execution.domain.services;

import com.trazza.trazzawebservice.execution.domain.model.commands.AcknowledgeDeviationAlertCommand;
import com.trazza.trazzawebservice.execution.domain.model.commands.RecordTrackingPointCommand;
import com.trazza.trazzawebservice.shared.domain.model.Result;

public interface TrackingCommandService {

    Result<TrackingResult> handle(RecordTrackingPointCommand command);

    Result<Boolean> handle(AcknowledgeDeviationAlertCommand command);
}
