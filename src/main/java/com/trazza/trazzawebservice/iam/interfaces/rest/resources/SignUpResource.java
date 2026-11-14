package com.trazza.trazzawebservice.iam.interfaces.rest.resources;

import java.util.List;

public record SignUpResource(String email, String password, String fullName, String dni, String phone,
                             List<String> roles, MerchantResource merchant) {
}
