package com.trazza.trazzawebservice.iam.interfaces.rest.resources;

import java.time.Instant;

public record AuthenticatedSessionResource(String accessToken, Instant accessTokenExpiresAt, String refreshToken,
                                           Instant refreshTokenExpiresAt, AccountResource user) {
}
