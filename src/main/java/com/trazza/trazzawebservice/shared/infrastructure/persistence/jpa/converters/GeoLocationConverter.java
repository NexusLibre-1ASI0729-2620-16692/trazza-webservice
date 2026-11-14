package com.trazza.trazzawebservice.shared.infrastructure.persistence.jpa.converters;

import com.trazza.trazzawebservice.shared.domain.model.valueobjects.GeoLocation;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Turns a {@link GeoLocation} into well-known text. Used together with
 * {@code @ColumnTransformer(read = "ST_AsText(col, 'axis-order=lat-long')", write = "ST_PointFromText(?, 4326, 'axis-order=lat-long')")}
 * on the {@code POINT SRID 4326} columns of MySQL.
 */
@Converter
public class GeoLocationConverter implements AttributeConverter<GeoLocation, String> {

    @Override
    public String convertToDatabaseColumn(GeoLocation location) {
        return location == null ? null : location.toWkt();
    }

    @Override
    public GeoLocation convertToEntityAttribute(String wkt) {
        return GeoLocation.fromWkt(wkt);
    }
}
