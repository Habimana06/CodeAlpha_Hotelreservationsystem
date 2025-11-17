package com.hotel.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Runs the schema and sample data scripts at startup so the UI can talk to a ready database.
 */
public class HotelDatabaseInitializer {
    private static final Logger log = LoggerFactory.getLogger(HotelDatabaseInitializer.class);
    private final DatabaseManager databaseManager;

    public HotelDatabaseInitializer(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void ensureSchema() {
        executeSqlResource("sql/schema.sql");
        executeSqlResource("sql/sample-data.sql");
    }

    private void executeSqlResource(String resourcePath) {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                log.warn("SQL resource {} not found", resourcePath);
                return;
            }
            String sql = new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .collect(Collectors.joining("\n"));
            runStatements(sql);
        } catch (IOException e) {
            log.error("Unable to read SQL resource {}", resourcePath, e);
        }
    }

    private void runStatements(String sqlBatch) {
        String[] statements = sqlBatch.split(";\\s*\n");
        try (Connection connection = databaseManager.getConnection(); Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                if (sql == null || sql.isBlank()) {
                    continue;
                }
                statement.execute(sql);
            }
        } catch (SQLException e) {
            log.error("Failed executing hotel reservation schema", e);
        }
    }
}
