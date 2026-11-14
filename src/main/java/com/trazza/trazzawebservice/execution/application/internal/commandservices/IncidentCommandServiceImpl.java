package com.trazza.trazzawebservice.execution.application.internal.commandservices;

import com.trazza.trazzawebservice.execution.application.internal.TripAccessService;
import com.trazza.trazzawebservice.execution.domain.model.aggregates.Incident;
import com.trazza.trazzawebservice.execution.domain.model.commands.AddIncidentEvidenceCommand;
import com.trazza.trazzawebservice.execution.domain.model.commands.ReportIncidentCommand;
import com.trazza.trazzawebservice.execution.domain.model.commands.ResolveIncidentCommand;
import com.trazza.trazzawebservice.execution.domain.model.valueobjects.IncidentStatus;
import com.trazza.trazzawebservice.execution.domain.model.valueobjects.TripStatus;
import com.trazza.trazzawebservice.execution.domain.services.IncidentCommandService;
import com.trazza.trazzawebservice.execution.infrastructure.persistence.jpa.repositories.IncidentRepository;
import com.trazza.trazzawebservice.shared.domain.model.CommandError;
import com.trazza.trazzawebservice.shared.domain.model.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class IncidentCommandServiceImpl implements IncidentCommandService {

    private static final int MAX_TEXT_LENGTH = 1000;
    private static final int MAX_S3_KEY_LENGTH = 512;

    private final TripAccessService tripAccessService;
    private final IncidentRepository incidentRepository;

    public IncidentCommandServiceImpl(TripAccessService tripAccessService, IncidentRepository incidentRepository) {
        this.tripAccessService = tripAccessService;
        this.incidentRepository = incidentRepository;
    }

    @Override
    @Transactional
    public Result<Incident> handle(ReportIncidentCommand command) {
        var access = tripAccessService.find(command.userId(), command.tripId()).orElse(null);
        if (access == null) return Result.failure(CommandError.notFound("trip-not-found"));
        var status = access.trip().getStatus();
        if (status == TripStatus.SCHEDULED || status == TripStatus.CANCELLED)
            return Result.failure(CommandError.conflict("trip-status-invalid"));
        if (command.description() == null || command.description().isBlank()
                || command.description().trim().length() > MAX_TEXT_LENGTH)
            return Result.failure(CommandError.validation("description-invalid"));

        return Result.success(incidentRepository.saveAndFlush(new Incident(access.trip().getId(), command.userId(),
                command.type(), command.description(), Instant.now())));
    }

    @Override
    @Transactional
    public Result<Incident> handle(AddIncidentEvidenceCommand command) {
        var incident = find(command.userId(), command.tripId(), command.incidentId()).orElse(null);
        if (incident == null) return Result.failure(CommandError.notFound("incident-not-found"));
        if (incident.getStatus() == IncidentStatus.RESOLVED)
            return Result.failure(CommandError.conflict("incident-resolved"));
        var key = command.s3Key() == null ? "" : command.s3Key().trim();
        if (key.isEmpty() || key.length() > MAX_S3_KEY_LENGTH || key.contains(" "))
            return Result.failure(CommandError.validation("s3-key-invalid"));

        incident.addEvidence(key, Instant.now());
        return Result.success(incidentRepository.saveAndFlush(incident));
    }

    @Override
    @Transactional
    public Result<Incident> handle(ResolveIncidentCommand command) {
        var incident = find(command.userId(), command.tripId(), command.incidentId()).orElse(null);
        if (incident == null) return Result.failure(CommandError.notFound("incident-not-found"));
        if (incident.getStatus() == IncidentStatus.RESOLVED)
            return Result.failure(CommandError.conflict("incident-resolved"));
        if (command.resolution() == null || command.resolution().isBlank()
                || command.resolution().trim().length() > MAX_TEXT_LENGTH)
            return Result.failure(CommandError.validation("resolution-invalid"));

        incident.resolve(command.resolution(), Instant.now());
        return Result.success(incidentRepository.saveAndFlush(incident));
    }

    private Optional<Incident> find(Long userId, UUID tripId, Long incidentId) {
        if (incidentId == null) return Optional.empty();
        return tripAccessService.find(userId, tripId)
                .flatMap(access -> incidentRepository.findByIdAndTripId(incidentId, access.trip().getId()));
    }
}
