package com.trazza.trazzawebservice.iam.domain.services;

import java.time.Instant;

/** Result of sign-up, sign-in and refresh. */
public record AuthenticatedSession(UserAccount account, String accessToken, Instant accessTokenExpiresAt,
                                   String refreshToken, Instant refreshTokenExpiresAt) {
}
