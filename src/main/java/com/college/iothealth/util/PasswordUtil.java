package com.college.iothealth.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for secure password hashing and verification using SHA-256.
 * Essential for PAT examination: demonstrates storing and comparing cryptographic hashes
 * instead of vulnerable plaintext credentials.
 */
public class PasswordUtil {

    private PasswordUtil() {
        // Prevent instantiation
    }

    /**
     * Hashes a raw password string using SHA-256 and returns a hex-encoded string.
     *
     * @param plainPassword Raw password entered by user
     * @return Hexadecimal SHA-256 hash
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available in current JRE", e);
        }
    }

    /**
     * Verifies if a raw password matches an existing stored SHA-256 hash.
     *
     * @param plainPassword Raw password entered by user
     * @param storedHash    Existing SHA-256 hash from database
     * @return true if matches, false otherwise
     */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) {
            return false;
        }
        String calculatedHash = hashPassword(plainPassword);
        return calculatedHash.equalsIgnoreCase(storedHash.trim());
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
