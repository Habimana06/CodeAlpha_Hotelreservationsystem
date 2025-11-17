package com.hotel.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Centralized utility for acquiring JDBC connections.
 */
public class DatabaseManager {
    private static DatabaseManager instance;
    private final DatabaseConfig config;

    private DatabaseManager() {
        this.config = new DatabaseConfig();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(config.getUrl(), config.getUsername(), config.getPassword());
    }

    public DatabaseConfig getConfig() {
        return config;
    }
}
