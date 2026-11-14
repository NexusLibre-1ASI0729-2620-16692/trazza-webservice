package com.trazza.trazzawebservice.iam.domain.model.commands;

public record CreateSavedAddressCommand(Long userId, String label, String address, String district, Double latitude,
                                        Double longitude) {
}
