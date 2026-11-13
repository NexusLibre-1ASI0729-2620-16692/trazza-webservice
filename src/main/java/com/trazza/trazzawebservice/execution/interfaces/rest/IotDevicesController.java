package com.trazza.trazzawebservice.execution.interfaces.rest;

import com.trazza.trazzawebservice.execution.domain.services.ExecutionQueryService;
import com.trazza.trazzawebservice.execution.interfaces.rest.transform.ExecutionResourceAssembler;
import com.trazza.trazzawebservice.iam.domain.model.valueobjects.Roles;
import com.trazza.trazzawebservice.iam.infrastructure.authorization.AuthenticatedUser;
import com.trazza.trazzawebservice.iam.infrastructure.authorization.Authorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** IoT cargo sensors lent to the signed-in carrier's trucks. */
@RestController
@RequestMapping(value = "/api/v1/iot-devices", produces = MediaType.APPLICATION_JSON_VALUE)
@Authorize(Roles.CARRIER)
@Tag(name = "IoT devices", description = "Cargo sensors lent to my trucks")
public class IotDevicesController {

    private final ExecutionQueryService queryService;

    public IotDevicesController(ExecutionQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "List the IoT devices lent to my trucks")
    public ResponseEntity<Object> listMine(AuthenticatedUser user) {
        return ResponseEntity.ok(queryService.listMyIotDevices(user.id()).stream()
                .map(ExecutionResourceAssembler::toResource).toList());
    }
}
