package com.trazza.trazzawebservice.shared.domain.model.valueobjects;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Public identifiers are UUID version 7 (time ordered). Internal numeric ids are never exposed by the API.
 */
public final class PublicIds {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PublicIds() {
    }

    public static UUID newId() {
        var bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        var millis = System.currentTimeMillis();
        for (var i = 0; i < 6; i++) bytes[i] = (byte) (millis >>> (40 - 8 * i));
        bytes[6] = (byte) ((bytes[6] & 0x0F) | 0x70);
        bytes[8] = (byte) ((bytes[8] & 0x3F) | 0x80);
        long most = 0, least = 0;
        for (var i = 0; i < 8; i++) most = (most << 8) | (bytes[i] & 0xFF);
        for (var i = 8; i < 16; i++) least = (least << 8) | (bytes[i] & 0xFF);
        return new UUID(most, least);
    }

    /** Compact form without dashes, used for simulated gateway ids. */
    public static String newCompactId() {
        return newId().toString().replace("-", "");
    }
}
