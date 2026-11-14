package com.trazza.platform.iam.domain.model.commands;

public record UpdateMerchantProfileCommand(Long userId, String businessName, String businessType) {
}
