package com.trazza.trazzawebservice.iam.domain.model.commands;

public record UpdateMerchantProfileCommand(Long userId, String businessName, String businessType) {
}
