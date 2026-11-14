package com.trazza.trazzawebservice.iam.domain.model.commands;

import com.trazza.trazzawebservice.iam.domain.model.valueobjects.UserRoleType;

public record AddRoleCommand(Long userId, UserRoleType role, MerchantData merchant) {
}
