package com.college.iothealth.model;

import java.sql.Timestamp;

/**
 * Model class for In-Patient record.
 * Matches the Oracle PATIENTS table schema.
 */
public class Patient {
    private int patientId;
    private String fullName;
    private int age;
    private String gender;
    private String contactNumber;
    private String email;
    private String bloodGroup;
    private String medicalHistory;
    private Timestamp createdAt;

    // Supplementary field for dashboard/list summaries
    private HealthReading latestReading;

    public Patient() {}

    public Patient(int patientId, String fullName, int age, String gender, String contactNumber, 
                   String email, String bloodGroup, String medicalHistory, Timestamp createdAt) {
        this.patientId = patientId;
        this.fullName = fullName;
        this.age = age;
        this.gender = gender;
        this.contactNumber = contactNumber;
        this.email = email;
        this.bloodGroup = bloodGroup;
        this.medicalHistory = medicalHistory;
        this.createdAt = createdAt;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getMedicalHistory() {
        return medicalHistory;
    }

    public void setMedicalHistory(String medicalHistory) {
        this.medicalHistory = medicalHistory;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public HealthReading getLatestReading() {
        return latestReading;
    }

    public void setLatestReading(HealthReading latestReading) {
        this.latestReading = latestReading;
    }
}
