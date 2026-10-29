package com.college.iothealth.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Manages configuration for Oracle Database and application server.
 * Priority:
 * 1. Environment variables (DB_URL, DB_USER, DB_PASSWORD, PORT)
 * 2. System properties (-Ddb.url=...)
 * 3. Classpath resource (db.properties)
 */
public class DatabaseConfig {

    private static final Properties properties = new Properties();

    static {
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream in = DatabaseConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                properties.load(in);
            } else {
                System.err.println("[WARN] db.properties not found in classpath, using fallback defaults.");
            }
        } catch (IOException e) {
            System.err.println("[WARN] Failed to load db.properties: " + e.getMessage());
        }
    }

    public static String getDriver() {
        return getProperty("db.driver", "oracle.jdbc.OracleDriver");
    }

    public static String getUrl() {
        String envUrl = System.getenv("DB_URL");
        if (envUrl != null && !envUrl.trim().isEmpty()) {
            return envUrl.trim();
        }
        return getProperty("db.url", "jdbc:oracle:thin:@//localhost:1521/XEPDB1");
    }

    public static String getUsername() {
        String envUser = System.getenv("DB_USER");
        if (envUser != null && !envUser.trim().isEmpty()) {
            return envUser.trim();
        }
        return getProperty("db.username", "system");
    }

    public static String getPassword() {
        String envPass = System.getenv("DB_PASSWORD");
        if (envPass != null) {
            return envPass;
        }
        return getProperty("db.password", "oracle");
    }

    public static int getServerPort() {
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                return Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {}
        }
        String propPort = getProperty("server.port", "8080");
        try {
            return Integer.parseInt(propPort.trim());
        } catch (NumberFormatException e) {
            return 8080;
        }
    }

    private static String getProperty(String key, String defaultValue) {
        String sysProp = System.getProperty(key);
        if (sysProp != null && !sysProp.trim().isEmpty()) {
            return sysProp.trim();
        }
        return properties.getProperty(key, defaultValue);
    }
}
