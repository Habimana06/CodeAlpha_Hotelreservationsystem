package com.hotel.service;

import com.hotel.config.DatabaseManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class AnalyticsService {
    private final DatabaseManager databaseManager = DatabaseManager.getInstance();

    public Map<String, Number> getDashboardMetrics() {
        Map<String, Number> metrics = new HashMap<>();
        String summarySql = "SELECT " +
                "(SELECT COUNT(*) FROM rooms) AS total_rooms, " +
                "(SELECT COUNT(*) FROM rooms WHERE status IN ('RESERVED','OCCUPIED')) AS busy_rooms, " +
                "(SELECT COUNT(*) FROM reservations WHERE reservation_status='CONFIRMED') AS active_reservations";
        try (Connection connection = databaseManager.getConnection(); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(summarySql)) {
            if (rs.next()) {
                int totalRooms = rs.getInt("total_rooms");
                int busy = rs.getInt("busy_rooms");
                metrics.put("totalRooms", totalRooms);
                metrics.put("busyRooms", busy);
                metrics.put("occupancy", totalRooms == 0 ? 0 : (busy * 100.0) / totalRooms);
                metrics.put("activeReservations", rs.getInt("active_reservations"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to fetch dashboard metrics", e);
        }
        return metrics;
    }
}
