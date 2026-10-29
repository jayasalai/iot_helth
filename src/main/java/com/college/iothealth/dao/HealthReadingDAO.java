package com.college.iothealth.dao;

import com.college.iothealth.config.DBConnection;
import com.college.iothealth.model.HealthReading;
import com.college.iothealth.service.VitalsClinicalRules;

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
 * Data Access Object for IoT Health Telemetry Streams.
 * Executes Oracle JDBC queries when connected, with seed fallback when Oracle listener is offline.
 */
public class HealthReadingDAO {

    private static final List<HealthReading> offlineReadings = new CopyOnWriteArrayList<>();

    static {
        initOfflineSeeds();
    }

    private static void initOfflineSeeds() {
        // Seed readings for Patient 1
        offlineReadings.add(createSeedReading(1, 1, "Rajesh Kumar", 36.8, 74, 98.5, 122, 78, 16, 6 * 3600000L));
        offlineReadings.add(createSeedReading(2, 1, "Rajesh Kumar", 37.1, 82, 97.0, 134, 86, 17, 3 * 3600000L));
        offlineReadings.add(createSeedReading(3, 1, "Rajesh Kumar", 37.0, 78, 98.0, 128, 82, 16, 1800000L));

        // Seed readings for Patient 2
        offlineReadings.add(createSeedReading(4, 2, "Ananya Sen", 36.6, 68, 99.0, 115, 74, 15, 5 * 3600000L));
        offlineReadings.add(createSeedReading(5, 2, "Ananya Sen", 36.7, 72, 98.5, 118, 76, 16, 3600000L));

        // Seed readings for Patient 3 (Critical hypoxia)
        offlineReadings.add(createSeedReading(6, 3, "Vikram Malhotra", 37.4, 106, 92.0, 142, 92, 23, 4 * 3600000L));
        offlineReadings.add(createSeedReading(7, 3, "Vikram Malhotra", 38.6, 128, 88.5, 155, 98, 27, 900000L));

        // Seed readings for Patient 4, 5, 6
        offlineReadings.add(createSeedReading(8, 4, "Meera Nair", 37.2, 92, 93.5, 126, 82, 21, 2 * 3600000L));
        offlineReadings.add(createSeedReading(9, 5, "Arjun Reddy", 36.5, 62, 99.5, 112, 70, 14, 2700000L));
        offlineReadings.add(createSeedReading(10, 6, "Sunita Gupta", 36.9, 132, 96.0, 148, 94, 19, 600000L));
    }

    private static HealthReading createSeedReading(int id, int patientId, String name, double temp, int hr, double spo2, int sys, int dia, int resp, long ageMs) {
        HealthReading r = new HealthReading(id, patientId, temp, hr, spo2, sys, dia, resp, "", new Timestamp(System.currentTimeMillis() - ageMs), "SIMULATED_IOT_TELEMETRY");
        r.setPatientName(name);
        VitalsClinicalRules.evaluateReading(r);
        return r;
    }

    public int insertReading(HealthReading reading) throws SQLException {
        VitalsClinicalRules.evaluateReading(reading);

        String sql = "INSERT INTO HEALTH_READINGS (PATIENT_ID, TEMPERATURE, HEART_RATE, SPO2, " +
                     "SYSTOLIC_BP, DIASTOLIC_BP, RESPIRATORY_RATE, OVERALL_STATUS, RECORDED_AT, SOURCE_TYPE) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, new String[]{"READING_ID"})) {

            ps.setInt(1, reading.getPatientId());
            ps.setDouble(2, reading.getTemperature());
            ps.setInt(3, reading.getHeartRate());
            ps.setDouble(4, reading.getSpo2());
            ps.setInt(5, reading.getSystolicBp());
            ps.setInt(6, reading.getDiastolicBp());
            ps.setInt(7, reading.getRespiratoryRate());
            ps.setString(8, reading.getOverallStatus());
            
            Timestamp ts = (reading.getRecordedAt() != null) ? reading.getRecordedAt() : new Timestamp(System.currentTimeMillis());
            ps.setTimestamp(9, ts);
            ps.setString(10, reading.getSourceType() != null ? reading.getSourceType() : "SIMULATED_IOT_TELEMETRY");

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        reading.setReadingId(id);
                        return id;
                    }
                }
            }
            return 1;
        } catch (SQLException e) {
            if (isOfflineConnectionError(e)) {
                int nextId = offlineReadings.stream().mapToInt(HealthReading::getReadingId).max().orElse(0) + 1;
                reading.setReadingId(nextId);
                if (reading.getRecordedAt() == null) {
                    reading.setRecordedAt(new Timestamp(System.currentTimeMillis()));
                }
                offlineReadings.add(0, reading);
                return nextId;
            }
            throw e;
        }
    }

    public List<HealthReading> getReadingsByPatient(int patientId, String fromDate, String toDate) throws SQLException {
        try {
            List<HealthReading> list = new ArrayList<>();
            StringBuilder sql = new StringBuilder(
                    "SELECT h.READING_ID, h.PATIENT_ID, p.FULL_NAME, h.TEMPERATURE, h.HEART_RATE, h.SPO2, " +
                    "h.SYSTOLIC_BP, h.DIASTOLIC_BP, h.RESPIRATORY_RATE, h.OVERALL_STATUS, h.RECORDED_AT, h.SOURCE_TYPE " +
                    "FROM HEALTH_READINGS h " +
                    "JOIN PATIENTS p ON h.PATIENT_ID = p.PATIENT_ID " +
                    "WHERE h.PATIENT_ID = ? "
            );

            boolean hasFrom = (fromDate != null && !fromDate.trim().isEmpty());
            boolean hasTo = (toDate != null && !toDate.trim().isEmpty());

            if (hasFrom) sql.append("AND TRUNC(h.RECORDED_AT) >= TO_DATE(?, 'YYYY-MM-DD') ");
            if (hasTo) sql.append("AND TRUNC(h.RECORDED_AT) <= TO_DATE(?, 'YYYY-MM-DD') ");

            sql.append("ORDER BY h.RECORDED_AT DESC");

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql.toString())) {

                int paramIndex = 1;
                ps.setInt(paramIndex++, patientId);
                if (hasFrom) ps.setString(paramIndex++, fromDate.trim());
                if (hasTo) ps.setString(paramIndex++, toDate.trim());

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapResultSetToReading(rs));
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            if (isOfflineConnectionError(e)) {
                return offlineReadings.stream()
                        .filter(r -> r.getPatientId() == patientId)
                        .collect(Collectors.toList());
            }
            throw e;
        }
    }

    public List<HealthReading> getAbnormalReadings(int limit) throws SQLException {
        try {
            List<HealthReading> list = new ArrayList<>();
            String sql = "SELECT h.READING_ID, h.PATIENT_ID, p.FULL_NAME, h.TEMPERATURE, h.HEART_RATE, h.SPO2, " +
                         "h.SYSTOLIC_BP, h.DIASTOLIC_BP, h.RESPIRATORY_RATE, h.OVERALL_STATUS, h.RECORDED_AT, h.SOURCE_TYPE " +
                         "FROM HEALTH_READINGS h " +
                         "JOIN PATIENTS p ON h.PATIENT_ID = p.PATIENT_ID " +
                         "WHERE h.OVERALL_STATUS IN ('WARNING', 'CRITICAL') " +
                         "ORDER BY h.RECORDED_AT DESC " +
                         "FETCH FIRST ? ROWS ONLY";

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, limit > 0 ? limit : 10);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapResultSetToReading(rs));
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            if (isOfflineConnectionError(e)) {
                return offlineReadings.stream()
                        .filter(r -> "WARNING".equals(r.getOverallStatus()) || "CRITICAL".equals(r.getOverallStatus()))
                        .limit(limit > 0 ? limit : 10)
                        .collect(Collectors.toList());
            }
            throw e;
        }
    }

    public HealthReading getLatestReadingForPatient(int patientId) throws SQLException {
        try {
            String sql = "SELECT h.READING_ID, h.PATIENT_ID, p.FULL_NAME, h.TEMPERATURE, h.HEART_RATE, h.SPO2, " +
                         "h.SYSTOLIC_BP, h.DIASTOLIC_BP, h.RESPIRATORY_RATE, h.OVERALL_STATUS, h.RECORDED_AT, h.SOURCE_TYPE " +
                         "FROM HEALTH_READINGS h " +
                         "JOIN PATIENTS p ON h.PATIENT_ID = p.PATIENT_ID " +
                         "WHERE h.PATIENT_ID = ? " +
                         "ORDER BY h.RECORDED_AT DESC " +
                         "FETCH FIRST 1 ROWS ONLY";

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, patientId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToReading(rs);
                    }
                }
            }
        } catch (SQLException e) {
            if (isOfflineConnectionError(e)) {
                return offlineReadings.stream()
                        .filter(r -> r.getPatientId() == patientId)
                        .findFirst()
                        .orElse(null);
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

    private HealthReading mapResultSetToReading(ResultSet rs) throws SQLException {
        HealthReading r = new HealthReading();
        r.setReadingId(rs.getInt("READING_ID"));
        r.setPatientId(rs.getInt("PATIENT_ID"));
        r.setPatientName(rs.getString("FULL_NAME"));
        r.setTemperature(rs.getDouble("TEMPERATURE"));
        r.setHeartRate(rs.getInt("HEART_RATE"));
        r.setSpo2(rs.getDouble("SPO2"));
        r.setSystolicBp(rs.getInt("SYSTOLIC_BP"));
        r.setDiastolicBp(rs.getInt("DIASTOLIC_BP"));
        r.setRespiratoryRate(rs.getInt("RESPIRATORY_RATE"));
        r.setOverallStatus(rs.getString("OVERALL_STATUS"));
        r.setRecordedAt(rs.getTimestamp("RECORDED_AT"));
        r.setSourceType(rs.getString("SOURCE_TYPE"));
        VitalsClinicalRules.evaluateReading(r);
        return r;
    }
}
