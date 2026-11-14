package com.trazza.platform.iam.domain.model.commands;

/** Business data required for the merchant role. */
public record MerchantData(String businessName, String ruc, String businessType) {
}
