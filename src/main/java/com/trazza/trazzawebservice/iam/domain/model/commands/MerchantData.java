package com.trazza.trazzawebservice.iam.domain.model.commands;

/** Business data required for the merchant role. */
public record MerchantData(String businessName, String ruc, String businessType) {
}
