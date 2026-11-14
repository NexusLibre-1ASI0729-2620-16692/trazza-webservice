package com.trazza.trazzawebservice.iam.infrastructure.authorization;

import com.trazza.trazzawebservice.iam.domain.model.valueobjects.UserRoleType;

import java.util.List;
import java.util.UUID;

/**
 * User resolved from the bearer token for the current request. Controllers receive it as a method argument.
 */
public record AuthenticatedUser(Long id, UUID publicId, List<UserRoleType> roles) {

    public static final String REQUEST_ATTRIBUTE = "trazza.authenticatedUser";

    public boolean isCarrier() {
        return roles.contains(UserRoleType.CARRIER);
    }

    public boolean isMerchant() {
        return roles.contains(UserRoleType.MERCHANT);
    }
}
