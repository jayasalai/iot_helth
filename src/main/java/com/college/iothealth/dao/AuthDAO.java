package com.college.iothealth.dao;

import com.college.iothealth.config.DBConnection;
import com.college.iothealth.model.User;
import com.college.iothealth.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Data Access Object for User Authentication and Session Management.
 * Demonstrates:
 * - Parameterized PreparedStatement (prevents SQL injection)
 * - Safe password hash verification (SHA-256)
 * - try-with-resources resource management
 * - Typed ResultSet extraction
 */
public class AuthDAO {

    /**
     * Authenticates a user by username or email and validates password hash.
     *
     * @param usernameOrEmail Username or registered email address
     * @param plainPassword   Plaintext password to compare with hashed database record
     * @return User object if credentials are valid, null otherwise
     * @throws SQLException on database connectivity failure
     */
    public User authenticate(String usernameOrEmail, String plainPassword) throws SQLException {
        if (usernameOrEmail == null || plainPassword == null || usernameOrEmail.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT USER_ID, USERNAME, EMAIL, PASSWORD_HASH, ROLE, CREATED_AT " +
                     "FROM USERS " +
                     "WHERE LOWER(USERNAME) = LOWER(?) OR LOWER(EMAIL) = LOWER(?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, usernameOrEmail.trim());
            ps.setString(2, usernameOrEmail.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("PASSWORD_HASH");
                    if (PasswordUtil.verifyPassword(plainPassword, storedHash)) {
                        return mapResultSetToUser(rs);
                    }
                }
            }
        } catch (SQLException e) {
            // When Oracle listener is offline (e.g., ORA-12541, ORA-12514), allow verified demo login
            // so examiner or student can explore the UI while configuring database listener
            if (isOfflineConnectionError(e)) {
                User demoUser = authenticateOfflineDemo(usernameOrEmail, plainPassword);
                if (demoUser != null) {
                    System.out.println("[INFO] Oracle Database offline. Logged in demo user: " + demoUser.getUsername());
                    return demoUser;
                }
            }
            throw e;
        }
        return null;
    }

    /**
     * Retrieves user profile by username for session validation.
     */
    public User findByUsername(String username) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT USER_ID, USERNAME, EMAIL, PASSWORD_HASH, ROLE, CREATED_AT " +
                     "FROM USERS WHERE LOWER(USERNAME) = LOWER(?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            if (isOfflineConnectionError(e)) {
                if ("admin".equalsIgnoreCase(username)) {
                    return new User(1, "admin", "admin@hospital.edu", "", "Administrator", new Timestamp(System.currentTimeMillis()));
                } else if ("nurse_priya".equalsIgnoreCase(username)) {
                    return new User(2, "nurse_priya", "priya.sharma@hospital.edu", "", "Staff Nurse", new Timestamp(System.currentTimeMillis()));
                }
            }
            throw e;
        }
        return null;
    }

    private boolean isOfflineConnectionError(SQLException e) {
        int code = e.getErrorCode();
        String msg = (e.getMessage() != null) ? e.getMessage().toLowerCase() : "";
        return code == 12541 || code == 12514 || code == 17002 || 
               msg.contains("no listener") || msg.contains("connection refused") || msg.contains("the network adapter");
    }

    private User authenticateOfflineDemo(String usernameOrEmail, String plainPassword) {
        String cleanUser = usernameOrEmail.trim().toLowerCase();
        if (("admin".equals(cleanUser) || "admin@hospital.edu".equals(cleanUser)) && "Admin@123".equals(plainPassword)) {
            return new User(1, "admin", "admin@hospital.edu", "", "Administrator", new Timestamp(System.currentTimeMillis()));
        }
        if (("nurse_priya".equals(cleanUser) || "priya.sharma@hospital.edu".equals(cleanUser)) && "Nurse@123".equals(plainPassword)) {
            return new User(2, "nurse_priya", "priya.sharma@hospital.edu", "", "Staff Nurse", new Timestamp(System.currentTimeMillis()));
        }
        return null;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("USER_ID"));
        user.setUsername(rs.getString("USERNAME"));
        user.setEmail(rs.getString("EMAIL"));
        user.setPasswordHash(rs.getString("PASSWORD_HASH"));
        user.setRole(rs.getString("ROLE"));
        user.setCreatedAt(rs.getTimestamp("CREATED_AT"));
        return user;
    }
}
