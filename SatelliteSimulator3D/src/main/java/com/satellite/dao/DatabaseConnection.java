package com.satellite.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL =
        "jdbc:sqlserver://localhost;databaseName=satellite_db;"
        + "integratedSecurity=true;encrypt=false;";

    // Để trống nếu dùng Windows Authentication
    private static final String USER = "";
    private static final String PASS = "";

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
