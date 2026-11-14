package com.trazza.trazzawebservice.iam.interfaces.acl;

import java.util.UUID;

public record ContactSnapshot(Long userId, UUID publicId, String fullName, String phone, String businessName) {

    /** Business name for merchants, full name otherwise. */
    public String displayName() {
        return businessName != null ? businessName : fullName;
    }
}
