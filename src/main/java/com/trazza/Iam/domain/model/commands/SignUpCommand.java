package com.trazza.platform.iam.domain.model.commands;

import com.trazza.platform.iam.domain.model.valueobjects.UserRoleType;

import java.util.List;

/** Registers an account with its person identity and one or both roles. */
public record SignUpCommand(String email, String password, String fullName, String dni, String phone,
                            List<UserRoleType> roles, MerchantData merchant) {
}
