package com.trazza.platform.billing.interfaces.rest;

import com.trazza.platform.billing.domain.services.BillingQueryService;
import com.trazza.platform.billing.interfaces.rest.transform.BillingResourceAssembler;
import com.trazza.platform.iam.infrastructure.authorization.AllowAnonymous;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/plans", produces = MediaType.APPLICATION_JSON_VALUE)
@AllowAnonymous
@Tag(name = "Plans", description = "Catalog of monitoring plans")
public class PlansController {

    private final BillingQueryService queryService;

    public PlansController(BillingQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "List the active plans")
    public ResponseEntity<Object> list() {
        return ResponseEntity.ok(queryService.listPlans().stream().map(BillingResourceAssembler::toResource).toList());
    }
}