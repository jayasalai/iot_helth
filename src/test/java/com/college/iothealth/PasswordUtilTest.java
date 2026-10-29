package com.college.iothealth;

import com.college.iothealth.util.PasswordUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for SHA-256 password hashing and verification.
 */
public class PasswordUtilTest {

    @Test
    @DisplayName("Should successfully hash and verify matching passwords")
    void testPasswordVerification() {
        String rawPassword = "Admin@123";
        String hash = PasswordUtil.hashPassword(rawPassword);

        assertNotNull(hash);
        assertTrue(hash.length() == 64); // SHA-256 produces 64 hex characters
        assertTrue(PasswordUtil.verifyPassword(rawPassword, hash));
        assertFalse(PasswordUtil.verifyPassword("WrongPassword", hash));
    }

    @Test
    @DisplayName("Precalculated test hashes should match correctly")
    void testPrecalculatedHashes() {
        String adminHash = "e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7";
        assertTrue(PasswordUtil.verifyPassword("Admin@123", adminHash));

        String nurseHash = "07429ac3dabe393512f2633b9514473ec7fc452dd202eb7735e60b900d273fd3";
        assertTrue(PasswordUtil.verifyPassword("Nurse@123", nurseHash));
    }
}
