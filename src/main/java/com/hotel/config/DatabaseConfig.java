package com.hotel.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

/**
 * Loads database connection details from application.properties with sane defaults.
 */
public class DatabaseConfig {
    private static final String PROPERTIES_FILE = "application.properties";
    private final Properties properties = new Properties();

    public DatabaseConfig() {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load application properties", e);
        }
    }

    public String getUrl() {
        return getEnvOrProperty("APP_DB_URL", "app.datasource.url", "jdbc:mysql://localhost:3306/hotel_reservation_system");
    }

    public String getUsername() {
        return getEnvOrProperty("APP_DB_USER", "app.datasource.username", "root");
    }

    public String getPassword() {
        return getEnvOrProperty("APP_DB_PASSWORD", "app.datasource.password", "changeme");
    }

    public String getHotelName() {
        return getEnvOrProperty("APP_BRAND_HOTEL", "app.branding.hotelName", "UC Grand Hotel");
    }

    public String getTagline() {
        return getEnvOrProperty("APP_BRAND_TAGLINE", "app.branding.tagline", "Luxury hospitality at its finest");
    }

    private String getEnvOrProperty(String envKey, String propertyKey, String fallback) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return properties.getProperty(propertyKey, Objects.requireNonNullElse(fallback, ""));
    }
}
