package com.trazza.platform.iam.domain.model.commands;

public record SubmitIdentityVerificationCommand(Long userId, String documentFrontS3Key, String documentBackS3Key) {
}
