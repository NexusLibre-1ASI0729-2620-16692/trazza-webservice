package com.trazza.iam.interfaces.rest.controllers;

import com.trazza.iam.domain.model.commands.FleetCommands.*;
import com.trazza.iam.domain.services.IamServices.FleetCommandService;
import com.trazza.iam.domain.services.IamServices.FleetQueryService;
import com.trazza.iam.infrastructure.pipeline.AuthenticatedUser.HttpContextAuthenticationExtensions;
import com.trazza.iam.interfaces.rest.resources.*;
import com.trazza.iam.interfaces.rest.transform.IamResourceAssembler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

public class FleetControllers {

    /**
     * Trucks of the signed-in carrier. Identified by their public id.
     */
    @RestController
    @RequestMapping("/api/v1/vehicles")
    public static class VehiclesController {
        private final FleetCommandService fleetCommandService;
        private final FleetQueryService fleetQueryService;

        public VehiclesController(FleetCommandService fleetCommandService, FleetQueryService fleetQueryService) {
            this.fleetCommandService = fleetCommandService;
            this.fleetQueryService = fleetQueryService;
        }

        private Long userId() {
            var request = HttpContextAuthenticationExtensions.getCurrentRequest();
            return HttpContextAuthenticationExtensions.getAuthenticatedUser(request).id();
        }

        @GetMapping
        public ResponseEntity<?> list() {
            return fleetQueryService.listVehicles(userId())
                    .thenApply(vehicles -> ResponseEntity.ok(vehicles.stream().map(IamResourceAssembler::toResource).toList()))
                    .join();
        }

        @PostMapping
        public ResponseEntity<?> create(@RequestBody SaveVehicleResource resource) {
            return fleetCommandService.handle(new CreateVehicleCommand(userId(), IamResourceAssembler.toVehicleData(resource)))
                    .thenApply(result -> result.isSuccess()
                            ? ResponseEntity.status(201).body(IamResourceAssembler.toResource(result.getValue()))
                            : ResponseEntity.badRequest().body(result.getError()))
                    .join();
        }

        @PutMapping("/{vehicleId}")
        public ResponseEntity<?> update(@PathVariable UUID vehicleId, @RequestBody SaveVehicleResource resource) {
            return fleetCommandService.handle(new UpdateVehicleCommand(userId(), vehicleId, IamResourceAssembler.toVehicleData(resource)))
                    .thenApply(result -> result.isSuccess()
                            ? ResponseEntity.ok(IamResourceAssembler.toResource(result.getValue()))
                            : ResponseEntity.badRequest().body(result.getError()))
                    .join();
        }

        @DeleteMapping("/{vehicleId}")
        public ResponseEntity<?> deactivate(@PathVariable UUID vehicleId) {
            return fleetCommandService.handle(new DeactivateVehicleCommand(userId(), vehicleId))
                    .thenApply(result -> result.isSuccess()
                            ? ResponseEntity.noContent().build()
                            : ResponseEntity.badRequest().body(result.getError()))
                    .join();
        }
    }

    /**
     * Drivers employed by the signed-in carrier.
     */
    @RestController
    @RequestMapping("/api/v1/drivers")
    public static class DriversController {
        private final FleetCommandService fleetCommandService;
        private final FleetQueryService fleetQueryService;

        public DriversController(FleetCommandService fleetCommandService, FleetQueryService fleetQueryService) {
            this.fleetCommandService = fleetCommandService;
            this.fleetQueryService = fleetQueryService;
        }

        private Long userId() {
            var request = HttpContextAuthenticationExtensions.getCurrentRequest();
            return HttpContextAuthenticationExtensions.getAuthenticatedUser(request).id();
        }

        @GetMapping
        public ResponseEntity<?> list() {
            return fleetQueryService.listDrivers(userId())
                    .thenApply(drivers -> ResponseEntity.ok(drivers.stream().map(IamResourceAssembler::toResource).toList()))
                    .join();
        }

        @PostMapping
        public ResponseEntity<?> create(@RequestBody SaveDriverResource resource) {
            var data = IamResourceAssembler.toDriverData(resource);
            if (data.isFailure()) return ResponseEntity.badRequest().body(data.getError());

            return fleetCommandService.handle(new CreateDriverCommand(userId(), data.getValue()))
                    .thenApply(result -> result.isSuccess()
                            ? ResponseEntity.status(201).body(IamResourceAssembler.toResource(result.getValue()))
                            : ResponseEntity.badRequest().body(result.getError()))
                    .join();
        }

        @PutMapping("/{driverId}")
        public ResponseEntity<?> update(@PathVariable Long driverId, @RequestBody SaveDriverResource resource) {
            var data = IamResourceAssembler.toDriverData(resource);
            if (data.isFailure()) return ResponseEntity.badRequest().body(data.getError());

            return fleetCommandService.handle(new UpdateDriverCommand(userId(), driverId, data.getValue()))
                    .thenApply(result -> result.isSuccess()
                            ? ResponseEntity.ok(IamResourceAssembler.toResource(result.getValue()))
                            : ResponseEntity.badRequest().body(result.getError()))
                    .join();
        }

        @DeleteMapping("/{driverId}")
        public ResponseEntity<?> deactivate(@PathVariable Long driverId) {
            return fleetCommandService.handle(new DeactivateDriverCommand(userId(), driverId))
                    .thenApply(result -> result.isSuccess()
                            ? ResponseEntity.noContent().build()
                            : ResponseEntity.badRequest().body(result.getError()))
                    .join();
        }
    }

    /**
     * Frequent pick-up and delivery addresses of the signed-in user, with real coordinates.
     */
    @RestController
    @RequestMapping("/api/v1/saved-addresses")
    public static class SavedAddressesController {
        private final FleetCommandService fleetCommandService;
        private final FleetQueryService fleetQueryService;

        public SavedAddressesController(FleetCommandService fleetCommandService, FleetQueryService fleetQueryService) {
            this.fleetCommandService = fleetCommandService;
            this.fleetQueryService = fleetQueryService;
        }

        private Long userId() {
            var request = HttpContextAuthenticationExtensions.getCurrentRequest();
            return HttpContextAuthenticationExtensions.getAuthenticatedUser(request).id();
        }

        @GetMapping
        public ResponseEntity<?> list() {
            return fleetQueryService.listSavedAddresses(userId())
                    .thenApply(addresses -> ResponseEntity.ok(addresses.stream().map(IamResourceAssembler::toResource).toList()))
                    .join();
        }

        @PostMapping
        public ResponseEntity<?> create(@RequestBody CreateSavedAddressResource resource) {
            return fleetCommandService.handle(new CreateSavedAddressCommand(userId(), resource.label(),
                            resource.address(), resource.district(), resource.latitude(), resource.longitude()))
                    .thenApply(result -> result.isSuccess()
                            ? ResponseEntity.status(201).body(IamResourceAssembler.toResource(result.getValue()))
                            : ResponseEntity.badRequest().body(result.getError()))
                    .join();
        }

        @DeleteMapping("/{savedAddressId}")
        public ResponseEntity<?> delete(@PathVariable Long savedAddressId) {
            return fleetCommandService.handle(new DeleteSavedAddressCommand(userId(), savedAddressId))
                    .thenApply(result -> result.isSuccess()
                            ? ResponseEntity.noContent().build()
                            : ResponseEntity.badRequest().body(result.getError()))
                    .join();
        }
    }
}