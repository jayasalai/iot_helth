package com.college.iothealth.servlet;

import com.college.iothealth.config.DBConnection;
import com.college.iothealth.config.DatabaseConfig;
import com.college.iothealth.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Diagnostic Health Check Servlet for verifying JDBC Connectivity to Oracle Database.
 * Endpoint:
 * - GET /api/health : Returns connection status, Oracle version, and JDBC configuration
 */
public class HealthCheckServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        Map<String, Object> status = new HashMap<>();
        status.put("application", "IoT-Based Health Monitoring System");
        status.put("configuredUrl", DatabaseConfig.getUrl());
        status.put("configuredUser", DatabaseConfig.getUsername());

        try (Connection conn = DBConnection.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            status.put("databaseConnected", true);
            status.put("databaseProduct", meta.getDatabaseProductName());
            status.put("databaseVersion", meta.getDatabaseProductVersion());
            status.put("driverName", meta.getDriverName());
            status.put("driverVersion", meta.getDriverVersion());

            JsonUtil.sendJsonResponse(resp, HttpServletResponse.SC_OK, 
                    new JsonUtil.ApiResponse(true, "Oracle Database connection is healthy and verified.", status));
        } catch (SQLException e) {
            status.put("databaseConnected", false);
            status.put("errorMessage", e.getMessage());
            status.put("errorCode", e.getErrorCode());
            status.put("sqlState", e.getSQLState());

            // Return HTTP 200 with databaseConnected: false so frontend diagnostic pill displays exact details
            JsonUtil.sendJsonResponse(resp, HttpServletResponse.SC_OK, 
                    new JsonUtil.ApiResponse(false, "Oracle Database listener currently offline: " + e.getMessage(), status));
        }
    }
}
