package com.trazza.trazzawebservice.execution.application.internal.commandservices;

import com.trazza.trazzawebservice.execution.application.internal.TripAccessService;
import com.trazza.trazzawebservice.execution.domain.model.aggregates.DeviationAlert;
import com.trazza.trazzawebservice.execution.domain.model.aggregates.TrackingPoint;
import com.trazza.trazzawebservice.execution.domain.model.commands.AcknowledgeDeviationAlertCommand;
import com.trazza.trazzawebservice.execution.domain.model.commands.RecordTrackingPointCommand;
import com.trazza.trazzawebservice.execution.domain.model.valueobjects.TrackingSource;
import com.trazza.trazzawebservice.execution.domain.services.TrackingCommandService;
import com.trazza.trazzawebservice.execution.domain.services.TrackingResult;
import com.trazza.trazzawebservice.execution.infrastructure.persistence.jpa.repositories.DeviationAlertRepository;
import com.trazza.trazzawebservice.execution.infrastructure.persistence.jpa.repositories.TrackingPointRepository;
import com.trazza.trazzawebservice.execution.infrastructure.persistence.jpa.repositories.TripRepository;
import com.trazza.trazzawebservice.shared.domain.model.CommandError;
import com.trazza.trazzawebservice.shared.domain.model.Result;
import com.trazza.trazzawebservice.shared.domain.model.valueobjects.GeoLocation;
import com.trazza.trazzawebservice.shared.infrastructure.transactions.Transactions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * GPS points sent by the carrier's phone (or by the simulator / an IoT device). A point farther than
 * {@link #DEVIATION_THRESHOLD_KM} from the expected path raises a deviation alert.
 */
@Service
public class TrackingCommandServiceImpl implements TrackingCommandService {

    public static final double DEVIATION_THRESHOLD_KM = 2;
    private static final BigDecimal MAX_SPEED_KMH = new BigDecimal("300");
    private static final Duration ALERT_COOLDOWN = Duration.ofMinutes(15);
    private static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(5);

    private final TripAccessService tripAccessService;
    private final TripRepository tripRepository;
    private final TrackingPointRepository trackingPointRepository;
    private final DeviationAlertRepository deviationAlertRepository;

    public TrackingCommandServiceImpl(TripAccessService tripAccessService, TripRepository tripRepository,
                                      TrackingPointRepository trackingPointRepository,
                                      DeviationAlertRepository deviationAlertRepository) {
        this.tripAccessService = tripAccessService;
        this.tripRepository = tripRepository;
        this.trackingPointRepository = trackingPointRepository;
        this.deviationAlertRepository = deviationAlertRepository;
    }

    @Override
    @Transactional
    public Result<TrackingResult> handle(RecordTrackingPointCommand command) {
        return Transactions.rollbackOnFailure(record(command));
    }

    @Override
    @Transactional
    public Result<Boolean> handle(AcknowledgeDeviationAlertCommand command) {
        var access = tripAccessService.find(command.userId(), command.tripId()).orElse(null);
        if (access == null) return Result.failure(CommandError.notFound("trip-not-found"));
        var alert = command.alertId() == null ? null
                : deviationAlertRepository.findByIdAndTripId(command.alertId(), access.trip().getId()).orElse(null);
        if (alert == null) return Result.failure(CommandError.notFound("alert-not-found"));
        alert.acknowledge(Instant.now());
        deviationAlertRepository.save(alert);
        return Result.success(true);
    }

    private Result<TrackingResult> record(RecordTrackingPointCommand command) {
        var access = tripAccessService.find(command.userId(), command.tripId()).orElse(null);
        if (access == null) return Result.failure(CommandError.notFound("trip-not-found"));
        if (!access.isCarrier()) return Result.failure(CommandError.forbidden("only-carrier"));
        var trip = access.trip();
        if (!trip.isOnTheRoad()) return Result.failure(CommandError.conflict("trip-not-on-the-road"));

        var location = GeoLocation.create(command.latitude(), command.longitude());
        if (location.isFailure()) return location.asFailure();
        if (command.speedKmh() != null
                && (command.speedKmh().signum() < 0 || command.speedKmh().compareTo(MAX_SPEED_KMH) > 0))
            return Result.failure(CommandError.validation("speed-invalid"));

        var now = Instant.now();
        var recordedAt = command.recordedAt() == null ? now : command.recordedAt();
        if (recordedAt.isAfter(now.plus(MAX_CLOCK_SKEW)))
            return Result.failure(CommandError.validation("recorded-at-invalid"));

        var last = trackingPointRepository.findFirstByTripIdOrderByRecordedAtDescIdDesc(trip.getId()).orElse(null);
        var addedKm = last == null ? 0 : last.getLocation().distanceTo(location.value());

        var point = trackingPointRepository.save(new TrackingPoint(trip.getId(), location.value(),
                command.speedKmh() == null ? null : command.speedKmh().setScale(1, RoundingMode.HALF_EVEN),
                command.source() == null ? TrackingSource.PHONE : command.source(), recordedAt));

        DeviationAlert alert = null;
        var context = access.context();
        var deviationKm = location.value().distanceToPath(
                List.of(context.origin(), context.pickup(), context.delivery(), context.destination()));
        if (deviationKm > DEVIATION_THRESHOLD_KM) {
            var lastOpen = deviationAlertRepository
                    .findFirstByTripIdAndAcknowledgedAtIsNullOrderByDetectedAtDesc(trip.getId()).orElse(null);
            if (lastOpen == null || lastOpen.getDetectedAt().isBefore(now.minus(ALERT_COOLDOWN)))
                alert = deviationAlertRepository.save(new DeviationAlert(trip.getId(), location.value(),
                        BigDecimal.valueOf(deviationKm).setScale(2, RoundingMode.HALF_EVEN), now));
        }

        trip.registerMovement(addedKm, command.userId(), now);
        tripRepository.saveAndFlush(trip);
        return Result.success(new TrackingResult(point, alert, trip.getStatus().name()));
    }
}
