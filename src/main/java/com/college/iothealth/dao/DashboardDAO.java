package com.college.iothealth.dao;

import com.college.iothealth.config.DBConnection;
import com.college.iothealth.model.DashboardStats;
import com.college.iothealth.model.HealthReading;
import com.college.iothealth.service.VitalsClinicalRules;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Healthcare Dashboard Aggregates and Telemetry Insights.
 * Executes real Oracle SQL aggregates (COUNT, AVG, MIN, MAX) when connected,
 * with seed preview fallback if Oracle listener is temporarily offline.
 */
public class DashboardDAO {

    public DashboardStats getDashboardStatistics() throws SQLException {
        try {
            return queryOracleDatabaseStats();
        } catch (SQLException e) {
            if (isOfflineConnectionError(e)) {
                return getOfflineSeedStats();
            }
            throw e;
        }
    }

    private DashboardStats queryOracleDatabaseStats() throws SQLException {
        DashboardStats stats = new DashboardStats();

        try (Connection conn = DBConnection.getConnection()) {
            // 1. Total registered patients
            String countPatientsSql = "SELECT COUNT(*) AS TOTAL_PATIENTS FROM PATIENTS";
            try (PreparedStatement ps = conn.prepareStatement(countPatientsSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setTotalPatients(rs.getInt("TOTAL_PATIENTS"));
                }
            }

            // 2. Total telemetry readings and averages (COUNT, AVG, ROUND)
            String vitalsAggSql = "SELECT COUNT(*) AS TOTAL_READINGS, " +
                                  "NVL(ROUND(AVG(TEMPERATURE), 1), 0.0) AS AVG_TEMP, " +
                                  "NVL(ROUND(AVG(HEART_RATE), 0), 0) AS AVG_HR, " +
                                  "NVL(ROUND(AVG(SPO2), 1), 0.0) AS AVG_SPO2 " +
                                  "FROM HEALTH_READINGS";
            try (PreparedStatement ps = conn.prepareStatement(vitalsAggSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setTotalReadings(rs.getInt("TOTAL_READINGS"));
                    stats.setAvgTemperature(rs.getDouble("AVG_TEMP"));
                    stats.setAvgHeartRate(rs.getDouble("AVG_HR"));
                    stats.setAvgSpo2(rs.getDouble("AVG_SPO2"));
                }
            }

            // 3. Triage breakdown based on each patient's LATEST recorded status
            String triageSql = 
                    "WITH LatestVitals AS ( " +
                    "    SELECT h.OVERALL_STATUS, " +
                    "           ROW_NUMBER() OVER (PARTITION BY h.PATIENT_ID ORDER BY h.RECORDED_AT DESC) as rn " +
                    "    FROM HEALTH_READINGS h " +
                    ") " +
                    "SELECT OVERALL_STATUS, COUNT(*) AS STATUS_COUNT " +
                    "FROM LatestVitals " +
                    "WHERE rn = 1 " +
                    "GROUP BY OVERALL_STATUS";

            try (PreparedStatement ps = conn.prepareStatement(triageSql);
                 ResultSet rs = ps.executeQuery()) {
                int normal = 0;
                int warning = 0;
                int critical = 0;

                while (rs.next()) {
                    String status = rs.getString("OVERALL_STATUS");
                    int count = rs.getInt("STATUS_COUNT");
                    if ("NORMAL".equalsIgnoreCase(status)) {
                        normal += count;
                    } else if ("WARNING".equalsIgnoreCase(status)) {
                        warning += count;
                    } else if ("CRITICAL".equalsIgnoreCase(status)) {
                        critical += count;
                    }
                }
                stats.setNormalPatientsCount(normal);
                stats.setAbnormalPatientsCount(warning + critical);
                stats.setCriticalPatientsCount(critical);
            }

            // 4. Recent telemetry activity
            String recentSql = 
                    "SELECT h.READING_ID, h.PATIENT_ID, p.FULL_NAME, h.TEMPERATURE, h.HEART_RATE, h.SPO2, " +
                    "h.SYSTOLIC_BP, h.DIASTOLIC_BP, h.RESPIRATORY_RATE, h.OVERALL_STATUS, h.RECORDED_AT, h.SOURCE_TYPE " +
                    "FROM HEALTH_READINGS h " +
                    "JOIN PATIENTS p ON h.PATIENT_ID = p.PATIENT_ID " +
                    "ORDER BY h.RECORDED_AT DESC " +
                    "FETCH FIRST 8 ROWS ONLY";

            List<HealthReading> recentList = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(recentSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    recentList.add(mapResultSetToReading(rs));
                }
            }
            stats.setRecentActivity(recentList);

            // 5. Abnormal readings requiring clinical attention
            String abnormalSql = 
                    "SELECT h.READING_ID, h.PATIENT_ID, p.FULL_NAME, h.TEMPERATURE, h.HEART_RATE, h.SPO2, " +
                    "h.SYSTOLIC_BP, h.DIASTOLIC_BP, h.RESPIRATORY_RATE, h.OVERALL_STATUS, h.RECORDED_AT, h.SOURCE_TYPE " +
                    "FROM HEALTH_READINGS h " +
                    "JOIN PATIENTS p ON h.PATIENT_ID = p.PATIENT_ID " +
                    "WHERE h.OVERALL_STATUS IN ('WARNING', 'CRITICAL') " +
                    "ORDER BY h.RECORDED_AT DESC " +
                    "FETCH FIRST 6 ROWS ONLY";

            List<HealthReading> abnormalList = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(abnormalSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    abnormalList.add(mapResultSetToReading(rs));
                }
            }
            stats.setAbnormalAlerts(abnormalList);
        }

        return stats;
    }

    private DashboardStats getOfflineSeedStats() {
        DashboardStats s = new DashboardStats();
        s.setTotalPatients(6);
        s.setTotalReadings(10);
        s.setNormalPatientsCount(3);
        s.setAbnormalPatientsCount(3);
        s.setCriticalPatientsCount(2);
        s.setAvgTemperature(37.1);
        s.setAvgHeartRate(88.0);
        s.setAvgSpo2(95.8);

        List<HealthReading> recent = new ArrayList<>();
        HealthReading r1 = new HealthReading(10, 6, 36.9, 132, 96.0, 148, 94, 19, "CRITICAL", new Timestamp(System.currentTimeMillis() - 600000), "SIMULATED_IOT_TELEMETRY");
        r1.setPatientName("Sunita Gupta");
        VitalsClinicalRules.evaluateReading(r1);
        recent.add(r1);

        HealthReading r2 = new HealthReading(9, 5, 36.5, 62, 99.5, 112, 70, 14, "NORMAL", new Timestamp(System.currentTimeMillis() - 2700000), "SIMULATED_IOT_TELEMETRY");
        r2.setPatientName("Arjun Reddy");
        VitalsClinicalRules.evaluateReading(r2);
        recent.add(r2);

        HealthReading r3 = new HealthReading(8, 4, 37.2, 92, 93.5, 126, 82, 21, "WARNING", new Timestamp(System.currentTimeMillis() - 7200000), "SIMULATED_IOT_TELEMETRY");
        r3.setPatientName("Meera Nair");
        VitalsClinicalRules.evaluateReading(r3);
        recent.add(r3);

        HealthReading r4 = new HealthReading(7, 3, 38.6, 128, 88.5, 155, 98, 27, "CRITICAL", new Timestamp(System.currentTimeMillis() - 900000), "SIMULATED_IOT_TELEMETRY");
        r4.setPatientName("Vikram Malhotra");
        VitalsClinicalRules.evaluateReading(r4);
        recent.add(r4);

        s.setRecentActivity(recent);

        List<HealthReading> abnormal = new ArrayList<>();
        abnormal.add(r1);
        abnormal.add(r4);
        abnormal.add(r3);
        s.setAbnormalAlerts(abnormal);

        return s;
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
