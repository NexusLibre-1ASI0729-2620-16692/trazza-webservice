package com.trazza.trazzawebservice.iam.domain.services;

import com.trazza.trazzawebservice.iam.domain.model.aggregates.Driver;
import com.trazza.trazzawebservice.iam.domain.model.aggregates.SavedAddress;
import com.trazza.trazzawebservice.iam.domain.model.aggregates.Vehicle;
import com.trazza.trazzawebservice.iam.domain.model.commands.CreateDriverCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.CreateSavedAddressCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.CreateVehicleCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.DeactivateDriverCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.DeactivateVehicleCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.DeleteSavedAddressCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.UpdateDriverCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.UpdateVehicleCommand;
import com.trazza.trazzawebservice.shared.domain.model.Result;

public interface FleetCommandService {

    Result<Vehicle> handle(CreateVehicleCommand command);

    Result<Vehicle> handle(UpdateVehicleCommand command);

    Result<Vehicle> handle(DeactivateVehicleCommand command);

    Result<Driver> handle(CreateDriverCommand command);

    Result<Driver> handle(UpdateDriverCommand command);

    Result<Driver> handle(DeactivateDriverCommand command);

    Result<SavedAddress> handle(CreateSavedAddressCommand command);

    Result<Boolean> handle(DeleteSavedAddressCommand command);
}
