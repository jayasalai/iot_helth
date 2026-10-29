package com.college.iothealth.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates aggregated metrics and clinical activity for the main healthcare dashboard.
 * Calculated purely from real Oracle database records via aggregate queries (COUNT, AVG, MIN, MAX).
 */
public class DashboardStats {
    private int totalPatients;
    private int totalReadings;
    private int normalPatientsCount;
    private int abnormalPatientsCount;
    private int criticalPatientsCount;
    private double avgTemperature;
    private double avgHeartRate;
    private double avgSpo2;

    private List<HealthReading> recentActivity = new ArrayList<>();
    private List<HealthReading> abnormalAlerts = new ArrayList<>();

    public DashboardStats() {}

    public int getTotalPatients() {
        return totalPatients;
    }

    public void setTotalPatients(int totalPatients) {
        this.totalPatients = totalPatients;
    }

    public int getTotalReadings() {
        return totalReadings;
    }

    public void setTotalReadings(int totalReadings) {
        this.totalReadings = totalReadings;
    }

    public int getNormalPatientsCount() {
        return normalPatientsCount;
    }

    public void setNormalPatientsCount(int normalPatientsCount) {
        this.normalPatientsCount = normalPatientsCount;
    }

    public int getAbnormalPatientsCount() {
        return abnormalPatientsCount;
    }

    public void setAbnormalPatientsCount(int abnormalPatientsCount) {
        this.abnormalPatientsCount = abnormalPatientsCount;
    }

    public int getCriticalPatientsCount() {
        return criticalPatientsCount;
    }

    public void setCriticalPatientsCount(int criticalPatientsCount) {
        this.criticalPatientsCount = criticalPatientsCount;
    }

    public double getAvgTemperature() {
        return avgTemperature;
    }

    public void setAvgTemperature(double avgTemperature) {
        this.avgTemperature = avgTemperature;
    }

    public double getAvgHeartRate() {
        return avgHeartRate;
    }

    public void setAvgHeartRate(double avgHeartRate) {
        this.avgHeartRate = avgHeartRate;
    }

    public double getAvgSpo2() {
        return avgSpo2;
    }

    public void setAvgSpo2(double avgSpo2) {
        this.avgSpo2 = avgSpo2;
    }

    public List<HealthReading> getRecentActivity() {
        return recentActivity;
    }

    public void setRecentActivity(List<HealthReading> recentActivity) {
        this.recentActivity = recentActivity;
    }

    public List<HealthReading> getAbnormalAlerts() {
        return abnormalAlerts;
    }

    public void setAbnormalAlerts(List<HealthReading> abnormalAlerts) {
        this.abnormalAlerts = abnormalAlerts;
    }
}
