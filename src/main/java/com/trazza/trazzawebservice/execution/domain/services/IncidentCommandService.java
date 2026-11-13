package com.trazza.trazzawebservice.execution.domain.services;

import com.trazza.trazzawebservice.execution.domain.model.aggregates.Incident;
import com.trazza.trazzawebservice.execution.domain.model.commands.AddIncidentEvidenceCommand;
import com.trazza.trazzawebservice.execution.domain.model.commands.ReportIncidentCommand;
import com.trazza.trazzawebservice.execution.domain.model.commands.ResolveIncidentCommand;
import com.trazza.trazzawebservice.shared.domain.model.Result;

public interface IncidentCommandService {

    Result<Incident> handle(ReportIncidentCommand command);

    Result<Incident> handle(AddIncidentEvidenceCommand command);

    Result<Incident> handle(ResolveIncidentCommand command);
}
