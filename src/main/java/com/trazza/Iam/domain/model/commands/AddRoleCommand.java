package com.trazza.platform.iam.domain.model.commands;

import com.trazza.platform.iam.domain.model.valueobjects.UserRoleType;

public record AddRoleCommand(Long userId, UserRoleType role, MerchantData merchant) {
}
