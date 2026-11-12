package com.trazza.platform.execution.domain.services;

import com.trazza.platform.execution.domain.model.commands.AcknowledgeDeviationAlertCommand;
import com.trazza.platform.execution.domain.model.commands.RecordTrackingPointCommand;
import com.trazza.platform.shared.domain.model.Result;

public interface TrackingCommandService {

    Result<TrackingResult> handle(RecordTrackingPointCommand command);

    Result<Boolean> handle(AcknowledgeDeviationAlertCommand command);
}
