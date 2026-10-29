package com.college.iothealth.dao;

import com.college.iothealth.config.DBConnection;
import com.college.iothealth.model.Patient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Data Access Object for Patient CRUD and Transaction Management.
 * Performs real Oracle JDBC operations when connected, with seed fallback when Oracle listener is offline.
 */
public class PatientDAO {

    // Seed data cache for offline preview when Oracle listener is not running
    private static final List<Patient> offlinePatients = new CopyOnWriteArrayList<>();

    static {
        initOfflineSeeds();
    }

    private static void initOfflineSeeds() {
        offlinePatients.add(new Patient(1, "Rajesh Kumar", 58, "Male", "+91 9876543210", "rajesh.kumar@example.com", "B+", "Type 2 Diabetes, Mild Hypertension", new Timestamp(System.currentTimeMillis() - 7 * 86400000L)));
        offlinePatients.add(new Patient(2, "Ananya Sen", 32, "Female", "+91 9811223344", "ananya.sen@example.com", "O+", "Post-operative recovery (Appendectomy)", new Timestamp(System.currentTimeMillis() - 5 * 86400000L)));
        offlinePatients.add(new Patient(3, "Vikram Malhotra", 67, "Male", "+91 9822334455", "vikram.m@example.com", "A+", "Chronic Obstructive Pulmonary Disease (COPD)", new Timestamp(System.currentTimeMillis() - 4 * 86400000L)));
        offlinePatients.add(new Patient(4, "Meera Nair", 45, "Female", "+91 9833445566", "meera.nair@example.com", "AB+", "Asthma, Seasonal allergies", new Timestamp(System.currentTimeMillis() - 3 * 86400000L)));
        offlinePatients.add(new Patient(5, "Arjun Reddy", 29, "Male", "+91 9844556677", "arjun.reddy@example.com", "O-", "Sports trauma recovery, no chronic illness", new Timestamp(System.currentTimeMillis() - 2 * 86400000L)));
        offlinePatients.add(new Patient(6, "Sunita Gupta", 63, "Female", "+91 9855667788", "sunita.g@example.com", "A-", "Cardiovascular monitoring, Arrhythmia history", new Timestamp(System.currentTimeMillis() - 1 * 86400000L)));
    }

    public int createPatient(Patient patient) throws SQLException {
        String sql = "INSERT INTO PATIENTS (FULL_NAME, AGE, GENDER, CONTACT_NUMBER, EMAIL, BLOOD_GROUP, MEDICAL_HISTORY) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, new String[]{"PATIENT_ID"})) {

            ps.setString(1, patient.getFullName());
            ps.setInt(2, patient.getAge());
            ps.setString(3, patient.getGender());
            ps.setString(4, patient.getContactNumber());
            ps.setString(5, patient.getEmail());
            ps.setString(6, patient.getBloodGroup());
            ps.setString(7, patient.getMedicalHistory());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Patient registration failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int newId = generatedKeys.getInt(1);
                    patient.setPatientId(newId);
                    return newId;
                }
            }
            return 1;
        } catch (SQLException e) {
            if (isOfflineConnectionError(e)) {
                int nextId = offlinePatients.stream().mapToInt(Patient::getPatientId).max().orElse(0) + 1;
                patient.setPatientId(nextId);
                patient.setCreatedAt(new Timestamp(System.currentTimeMillis()));
                offlinePatients.add(0, patient);
                return nextId;
            }
            throw e;
        }
    }

    public List<Patient> getPatients(String search, int page, int pageSize) throws SQLException {
        try {
            return queryOraclePatients(search, page, pageSize);
        } catch (SQLException e) {
            if (isOfflineConnectionError(e)) {
                return getOfflinePatients(search, page, pageSize);
            }
            throw e;
        }
    }

    private List<Patient> queryOraclePatients(String search, int page, int pageSize) throws SQLException {
        List<Patient> patients = new ArrayList<>();
        boolean hasSearch = (search != null && !search.trim().isEmpty());

        StringBuilder sql = new StringBuilder("SELECT PATIENT_ID, FULL_NAME, AGE, GENDER, CONTACT_NUMBER, EMAIL, BLOOD_GROUP, MEDICAL_HISTORY, CREATED_AT FROM PATIENTS ");
        if (hasSearch) {
            sql.append("WHERE LOWER(FULL_NAME) LIKE ? OR TO_CHAR(PATIENT_ID) = ? ");
        }
        sql.append("ORDER BY PATIENT_ID DESC ");

        int offset = Math.max(0, (page - 1) * pageSize);
        sql.append("OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (hasSearch) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                ps.setString(paramIndex++, searchPattern);
                ps.setString(paramIndex++, search.trim());
            }
            ps.setInt(paramIndex++, offset);
            ps.setInt(paramIndex, pageSize);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    patients.add(mapResultSetToPatient(rs));
                }
            }
        }
        return patients;
    }

    private List<Patient> getOfflinePatients(String search, int page, int pageSize) {
        List<Patient> filtered = offlinePatients;
        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim().toLowerCase();
            filtered = offlinePatients.stream()
                    .filter(p -> p.getFullName().toLowerCase().contains(q) || String.valueOf(p.getPatientId()).equals(q))
                    .collect(Collectors.toList());
        }
        int fromIndex = Math.min((page - 1) * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        return new ArrayList<>(filtered.subList(fromIndex, toIndex));
    }

    public int countPatients(String search) throws SQLException {
        try {
            boolean hasSearch = (search != null && !search.trim().isEmpty());
            StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS TOTAL FROM PATIENTS ");
            if (hasSearch) {
                sql.append("WHERE LOWER(FULL_NAME) LIKE ? OR TO_CHAR(PATIENT_ID) = ?");
            }

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql.toString())) {

                if (hasSearch) {
                    String searchPattern = "%" + search.trim().toLowerCase() + "%";
                    ps.setString(1, searchPattern);
                    ps.setString(2, search.trim());
                }

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("TOTAL");
                    }
                }
            }
        } catch (SQLException e) {
            if (isOfflineConnectionError(e)) {
                if (search != null && !search.trim().isEmpty()) {
                    String q = search.trim().toLowerCase();
                    return (int) offlinePatients.stream()
                            .filter(p -> p.getFullName().toLowerCase().contains(q) || String.valueOf(p.getPatientId()).equals(q))
                            .count();
                }
                return offlinePatients.size();
            }
            throw e;
        }
        return 0;
    }

    public Patient getPatientById(int patientId) throws SQLException {
        try {
            String sql = "SELECT PATIENT_ID, FULL_NAME, AGE, GENDER, CONTACT_NUMBER, EMAIL, BLOOD_GROUP, MEDICAL_HISTORY, CREATED_AT " +
                         "FROM PATIENTS WHERE PATIENT_ID = ?";

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, patientId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToPatient(rs);
                    }
                }
            }
        } catch (SQLException e) {
            if (isOfflineConnectionError(e)) {
                return offlinePatients.stream()
                        .filter(p -> p.getPatientId() == patientId)
                        .findFirst()
                        .orElse(null);
            }
            throw e;
        }
        return null;
    }

    public boolean updatePatient(Patient patient) throws SQLException {
        try {
            String sql = "UPDATE PATIENTS SET FULL_NAME = ?, AGE = ?, GENDER = ?, CONTACT_NUMBER = ?, " +
                         "EMAIL = ?, BLOOD_GROUP = ?, MEDICAL_HISTORY = ? WHERE PATIENT_ID = ?";

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, patient.getFullName());
                ps.setInt(2, patient.getAge());
                ps.setString(3, patient.getGender());
                ps.setString(4, patient.getContactNumber());
                ps.setString(5, patient.getEmail());
                ps.setString(6, patient.getBloodGroup());
                ps.setString(7, patient.getMedicalHistory());
                ps.setInt(8, patient.getPatientId());

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            if (isOfflineConnectionError(e)) {
                for (int i = 0; i < offlinePatients.size(); i++) {
                    if (offlinePatients.get(i).getPatientId() == patient.getPatientId()) {
                        offlinePatients.set(i, patient);
                        return true;
                    }
                }
                return false;
            }
            throw e;
        }
    }

    public boolean deletePatient(int patientId) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            String deleteReadingsSql = "DELETE FROM HEALTH_READINGS WHERE PATIENT_ID = ?";
            try (PreparedStatement psReadings = conn.prepareStatement(deleteReadingsSql)) {
                psReadings.setInt(1, patientId);
                psReadings.executeUpdate();
            }

            String deletePatientSql = "DELETE FROM PATIENTS WHERE PATIENT_ID = ?";
            int patientRowsDeleted;
            try (PreparedStatement psPatient = conn.prepareStatement(deletePatientSql)) {
                psPatient.setInt(1, patientId);
                patientRowsDeleted = psPatient.executeUpdate();
            }

            if (patientRowsDeleted > 0) {
                conn.commit();
                return true;
            } else {
                conn.rollback();
                return false;
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {}
            }
            if (isOfflineConnectionError(e)) {
                return offlinePatients.removeIf(p -> p.getPatientId() == patientId);
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    private boolean isOfflineConnectionError(SQLException e) {
        int code = e.getErrorCode();
        String msg = (e.getMessage() != null) ? e.getMessage().toLowerCase() : "";
        return code == 12541 || code == 12514 || code == 17002 || 
               msg.contains("no listener") || msg.contains("connection refused") || msg.contains("the network adapter");
    }

    private Patient mapResultSetToPatient(ResultSet rs) throws SQLException {
        Patient p = new Patient();
        p.setPatientId(rs.getInt("PATIENT_ID"));
        p.setFullName(rs.getString("FULL_NAME"));
        p.setAge(rs.getInt("AGE"));
        p.setGender(rs.getString("GENDER"));
        p.setContactNumber(rs.getString("CONTACT_NUMBER"));
        p.setEmail(rs.getString("EMAIL"));
        p.setBloodGroup(rs.getString("BLOOD_GROUP"));
        p.setMedicalHistory(rs.getString("MEDICAL_HISTORY"));
        p.setCreatedAt(rs.getTimestamp("CREATED_AT"));
        return p;
    }
}
