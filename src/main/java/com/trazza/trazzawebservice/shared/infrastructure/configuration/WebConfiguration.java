package com.trazza.trazzawebservice.shared.infrastructure.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.module.SimpleModule;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

/**
 * Web conventions shared by every bounded context: CORS for the web application, request culture from
 * {@code Accept-Language} (en, es) and UTC date-times written as {@code yyyy-MM-ddTHH:mm:ss.SSSZ}.
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    private static final DateTimeFormatter UTC_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private final List<String> allowedOrigins;

    public WebConfiguration(@Value("${trazza.cors.allowed-origins:*}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var mapping = registry.addMapping("/**").allowedMethods("*").allowedHeaders("*");
        if (allowedOrigins.isEmpty() || allowedOrigins.contains("*")) mapping.allowedOriginPatterns("*");
        else mapping.allowedOrigins(allowedOrigins.toArray(String[]::new));
    }

    @Bean
    public LocaleResolver localeResolver() {
        var resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(List.of(Locale.ENGLISH, Locale.forLanguageTag("es"), Locale.forLanguageTag("es-PE"),
                Locale.forLanguageTag("en-US")));
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }

    @Bean
    public SimpleModule utcDateTimeModule() {
        var module = new SimpleModule("trazza-utc-date-time");
        module.addSerializer(Instant.class, new ValueSerializer<>() {
            @Override
            public void serialize(Instant value, JsonGenerator generator, SerializationContext context) {
                generator.writeString(UTC_FORMAT.format(value));
            }
        });
        module.addDeserializer(Instant.class, new ValueDeserializer<>() {
            @Override
            public Instant deserialize(JsonParser parser, DeserializationContext context) {
                var text = parser.getValueAsString();
                if (text == null || text.isBlank()) return null;
                try {
                    return OffsetDateTime.parse(text).toInstant();
                } catch (DateTimeParseException ignored) {
                    try {
                        return LocalDateTime.parse(text).toInstant(ZoneOffset.UTC);
                    } catch (DateTimeParseException exception) {
                        throw InvalidFormatException.from(parser, "date-time-invalid", text, Instant.class);
                    }
                }
            }
        });
        return module;
    }
}
