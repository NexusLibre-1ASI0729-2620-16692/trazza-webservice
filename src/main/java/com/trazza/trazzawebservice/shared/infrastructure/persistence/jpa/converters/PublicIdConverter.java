package com.trazza.trazzawebservice.shared.infrastructure.persistence.jpa.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Stores a UUID in a {@code BINARY(16)} column with the same byte order the .NET version used
 * ({@code Guid.ToByteArray}: the first three groups little-endian), so both APIs can share {@code trazza_database}.
 */
@Converter
public class PublicIdConverter implements AttributeConverter<UUID, byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(UUID id) {
        return id == null ? null : toBytes(id);
    }

    @Override
    public UUID convertToEntityAttribute(byte[] bytes) {
        return bytes == null ? null : fromBytes(bytes);
    }

    public static byte[] toBytes(UUID id) {
        var standard = ByteBuffer.allocate(16).putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits()).array();
        return swap(standard);
    }

    public static UUID fromBytes(byte[] bytes) {
        var standard = ByteBuffer.wrap(swap(bytes));
        return new UUID(standard.getLong(), standard.getLong());
    }

    private static byte[] swap(byte[] source) {
        var result = source.clone();
        result[0] = source[3];
        result[1] = source[2];
        result[2] = source[1];
        result[3] = source[0];
        result[4] = source[5];
        result[5] = source[4];
        result[6] = source[7];
        result[7] = source[6];
        return result;
    }
}
