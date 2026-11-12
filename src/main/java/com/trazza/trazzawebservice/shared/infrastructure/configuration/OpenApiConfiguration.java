package com.trazza.trazzawebservice.shared.infrastructure.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI document (served at {@code /v3/api-docs}, rendered at {@code /swagger-ui.html}) with the JWT bearer scheme.
 */
@Configuration
public class OpenApiConfiguration {

    public static final String BEARER_SCHEME = "Bearer";

    @Bean
    public OpenAPI trazzaOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Trazza Platform API").version("1.0.0")
                        .description("REST API of Trazza: carriers publish the empty return trip of their trucks and "
                                + "merchants of Lima send their cargo in that free space.")
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                        .description("Access token returned by sign-up, sign-in or refresh.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
