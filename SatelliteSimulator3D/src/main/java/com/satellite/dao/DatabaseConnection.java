package com.satellite.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL =
        "jdbc:sqlserver://localhost:1433;databaseName=satellite_db;encrypt=false;";

    private static final String USER = "satellite_user";
    private static final String PASS = "Satellite@2026";

    private static Connection connection;

    public static Connection get() {
        try {
            if (connection == null || connection.isClosed()) {
                if (USER.isEmpty()) {
                    connection = DriverManager.getConnection(URL);
                } else {
                    connection = DriverManager.getConnection(URL, USER, PASS);
                }
                System.out.println("✅ Kết nối SQL Server thành công!");
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi kết nối DB: " + e.getMessage());
        }
        return connection;
    }

    public static void close() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
