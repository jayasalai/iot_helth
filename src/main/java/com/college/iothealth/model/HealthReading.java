package com.college.iothealth.model;

import java.sql.Timestamp;

/**
 * Model class for simulated IoT telemetry readings.
 * Matches Oracle HEALTH_READINGS table schema and supports JOIN queries with PATIENTS.
 */
public class HealthReading {
    private int readingId;
    private int patientId;
    private String patientName; // populated via SQL JOIN with PATIENTS
    private double temperature;
    private int heartRate;
    private double spo2;
    private int systolicBp;
    private int diastolicBp;
    private int respiratoryRate;
    private String overallStatus; // NORMAL, WARNING, CRITICAL
    private Timestamp recordedAt;
    private String sourceType;

    // Computed vitals status breakdown for detailed clinical UI indicators
    private String tempStatus;
    private String hrStatus;
    private String spo2Status;
    private String bpStatus;
    private String respStatus;

    public HealthReading() {
        this.sourceType = "SIMULATED_IOT_TELEMETRY";
    }

    public HealthReading(int readingId, int patientId, double temperature, int heartRate, double spo2, 
                         int systolicBp, int diastolicBp, int respiratoryRate, String overallStatus, 
                         Timestamp recordedAt, String sourceType) {
        this.readingId = readingId;
        this.patientId = patientId;
        this.temperature = temperature;
        this.heartRate = heartRate;
        this.spo2 = spo2;
        this.systolicBp = systolicBp;
        this.diastolicBp = diastolicBp;
        this.respiratoryRate = respiratoryRate;
        this.overallStatus = overallStatus;
        this.recordedAt = recordedAt;
        this.sourceType = (sourceType != null) ? sourceType : "SIMULATED_IOT_TELEMETRY";
    }

    public int getReadingId() {
        return readingId;
    }

    public void setReadingId(int readingId) {
        this.readingId = readingId;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(int heartRate) {
        this.heartRate = heartRate;
    }

    public double getSpo2() {
        return spo2;
    }

    public void setSpo2(double spo2) {
        this.spo2 = spo2;
    }

    public int getSystolicBp() {
        return systolicBp;
    }

    public void setSystolicBp(int systolicBp) {
        this.systolicBp = systolicBp;
    }

    public int getDiastolicBp() {
        return diastolicBp;
    }

    public void setDiastolicBp(int diastolicBp) {
        this.diastolicBp = diastolicBp;
    }

    public int getRespiratoryRate() {
        return respiratoryRate;
    }

    public void setRespiratoryRate(int respiratoryRate) {
        this.respiratoryRate = respiratoryRate;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public Timestamp getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Timestamp recordedAt) {
        this.recordedAt = recordedAt;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getTempStatus() {
        return tempStatus;
    }

    public void setTempStatus(String tempStatus) {
        this.tempStatus = tempStatus;
    }

    public String getHrStatus() {
        return hrStatus;
    }

    public void setHrStatus(String hrStatus) {
        this.hrStatus = hrStatus;
    }

    public String getSpo2Status() {
        return spo2Status;
    }

    public void setSpo2Status(String spo2Status) {
        this.spo2Status = spo2Status;
    }

    public String getBpStatus() {
        return bpStatus;
    }

    public void setBpStatus(String bpStatus) {
        this.bpStatus = bpStatus;
    }

    public String getRespStatus() {
        return respStatus;
    }

    public void setRespStatus(String respStatus) {
        this.respStatus = respStatus;
    }
}
