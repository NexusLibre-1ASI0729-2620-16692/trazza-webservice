package com.trazza.trazzawebservice.shared;

import com.trazza.trazzawebservice.shared.infrastructure.persistence.jpa.converters.PublicIdConverter;
import org.junit.jupiter.api.Test;

import java.util.HexFormat;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PublicIdConverterTest {

    private static final UUID ID = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");

    @Test
    void writesTheSameBytesAsDotNetGuidToByteArray() {
        assertEquals("33221100554477668899aabbccddeeff", HexFormat.of().formatHex(PublicIdConverter.toBytes(ID)));
    }

    @Test
    void readsBackTheSameUuid() {
        assertEquals(ID, PublicIdConverter.fromBytes(PublicIdConverter.toBytes(ID)));
    }
}
