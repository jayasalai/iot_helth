package com.college.iothealth.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Reusable JDBC connection utility for Oracle Database.
 * Fulfills core Database Systems Lab requirement:
 * Demonstrates java.sql.DriverManager, java.sql.Connection, and java.sql.SQLException handling.
 *
 * Supported Oracle Connection URL formats:
 * - Service Name (Recommended for 12c, 19c, 21c, 23c):
 *     jdbc:oracle:thin:@//<host>:<port>/<service_name>
 *     e.g., jdbc:oracle:thin:@//localhost:1521/XEPDB1
 * - SID (Legacy 10g/11g):
 *     jdbc:oracle:thin:@<host>:<port>:<sid>
 *     e.g., jdbc:oracle:thin:@localhost:1521:XE
 */
public class DBConnection {

    static {
        try {
            // Explicitly load Oracle JDBC Driver class into JVM memory
            Class.forName(DatabaseConfig.getDriver());
            System.out.println("[INFO] Oracle JDBC Driver successfully registered.");
        } catch (ClassNotFoundException e) {
            System.err.println("[FATAL] Oracle JDBC Driver not found in classpath: " + e.getMessage());
            System.err.println("Ensure ojdbc11 is present in pom.xml dependencies.");
        }
    }

    private DBConnection() {
        // Prevent direct instantiation
    }

    /**
     * Establishes and returns a new java.sql.Connection to the Oracle database.
     * Callers must close the Connection using try-with-resources.
     *
     * @return active java.sql.Connection object
     * @throws SQLException if a database access error occurs or credentials fail
     */
    public static Connection getConnection() throws SQLException {
        String url = DatabaseConfig.getUrl();
        String user = DatabaseConfig.getUsername();
        String password = DatabaseConfig.getPassword();

        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            printDiagnosticInfo(e, url, user);
            throw e;
        }
    }

    /**
     * Tests database connectivity and prints verification details.
     *
     * @return true if connection succeeds, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            boolean valid = conn != null && !conn.isClosed();
            if (valid) {
                System.out.println("[SUCCESS] Connected to Oracle Database: " 
                        + conn.getMetaData().getDatabaseProductName() + " "
                        + conn.getMetaData().getDatabaseProductVersion());
            }
            return valid;
        } catch (SQLException e) {
            System.err.println("[ERROR] Database connectivity test failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Translates common Oracle Error Codes into clear, actionable student explanations.
     */
    private static void printDiagnosticInfo(SQLException e, String url, String user) {
        int errorCode = e.getErrorCode();
        System.err.println("\n==================== ORACLE JDBC DIAGNOSTIC ====================");
        System.err.println("JDBC URL    : " + url);
        System.err.println("DB Username : " + user);
        System.err.println("SQLState    : " + e.getSQLState());
        System.err.println("Error Code  : " + errorCode);
        System.err.println("Message     : " + e.getMessage());

        switch (errorCode) {
            case 1017:
                System.err.println("[TROUBLESHOOTING ORA-01017]: Invalid username/password logon denied.");
                System.err.println("Check DB_USER and DB_PASSWORD in db.properties or environment.");
                break;
            case 12514:
                System.err.println("[TROUBLESHOOTING ORA-12514]: TNS:listener does not currently know of service.");
                System.err.println("Verify the Oracle Pluggable Database (PDB) service name in URL (e.g., XEPDB1 vs FREEPDB1 vs ORCL).");
                break;
            case 12541:
                System.err.println("[TROUBLESHOOTING ORA-12541]: TNS:no listener.");
                System.err.println("Check if Oracle service or Docker container is running and listening on port 1521.");
                break;
            case 17002:
                System.err.println("[TROUBLESHOOTING The Network Adapter could not establish the connection]:");
                System.err.println("Verify the Oracle host IP/hostname and ensure port 1521 is reachable.");
                break;
            default:
                break;
        }
        System.err.println("================================================================\n");
    }
}
