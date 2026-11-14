package com.trazza.trazzawebservice.iam.domain.services;

import com.trazza.trazzawebservice.iam.domain.model.aggregates.Driver;
import com.trazza.trazzawebservice.iam.domain.model.aggregates.SavedAddress;
import com.trazza.trazzawebservice.iam.domain.model.aggregates.Vehicle;

import java.util.List;

public interface FleetQueryService {

    List<Vehicle> listVehicles(Long userId);

    List<Driver> listDrivers(Long userId);

    List<SavedAddress> listSavedAddresses(Long userId);
}
