package com.trazza.trazzawebservice.execution.domain.services;

import com.trazza.trazzawebservice.execution.domain.model.aggregates.DeviationAlert;
import com.trazza.trazzawebservice.execution.domain.model.aggregates.Incident;
import com.trazza.trazzawebservice.execution.domain.model.aggregates.TrackingPoint;
import com.trazza.trazzawebservice.execution.domain.model.entities.TripStatusChange;
import com.trazza.trazzawebservice.shared.domain.model.Result;

import java.util.List;
import java.util.UUID;

public interface ExecutionQueryService {

    List<TripView> listMyTrips(Long userId);

    Result<TripView> getTrip(Long userId, UUID tripId);

    Result<List<TripStatusChange>> listHistory(Long userId, UUID tripId);

    Result<List<TrackingPoint>> listTrackingPoints(Long userId, UUID tripId);

    Result<List<DeviationAlert>> listDeviationAlerts(Long userId, UUID tripId);

    Result<List<Incident>> listIncidents(Long userId, UUID tripId);

    List<IotDeviceView> listMyIotDevices(Long userId);
}
