package com.trazza.platform.execution.domain.services;

import com.trazza.platform.execution.domain.model.aggregates.Incident;
import com.trazza.platform.execution.domain.model.commands.AddIncidentEvidenceCommand;
import com.trazza.platform.execution.domain.model.commands.ReportIncidentCommand;
import com.trazza.platform.execution.domain.model.commands.ResolveIncidentCommand;
import com.trazza.platform.shared.domain.model.Result;

public interface IncidentCommandService {

    Result<Incident> handle(ReportIncidentCommand command);

    Result<Incident> handle(AddIncidentEvidenceCommand command);

    Result<Incident> handle(ResolveIncidentCommand command);
}
