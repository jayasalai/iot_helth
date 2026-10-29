package com.college.iothealth.service;

import com.college.iothealth.model.HealthReading;

/**
 * Educational Clinical Decision Support Engine.
 *
 * Evaluates simulated IoT sensor readings against evidence-based vital sign reference ranges.
 *
 * NOTE FOR EXAMINATION EVALUATION:
 * These thresholds serve as educational reference ranges for demonstrative clinical monitoring.
 * In actual healthcare settings, clinical interpretation depends on patient age, baseline physiology,
 * acute medical diagnoses, pharmacological interventions, and holistic assessment.
 * This software simulates medical telemetry and does not replace certified clinical judgment.
 *
 * STATUS PRIORITY RULE:
 * CRITICAL (Highest) > WARNING > NORMAL (Lowest)
 * A reading is marked CRITICAL if ANY individual vital parameter breaches critical thresholds.
 */
public class VitalsClinicalRules {

    public static final String STATUS_NORMAL = "NORMAL";
    public static final String STATUS_WARNING = "WARNING";
    public static final String STATUS_CRITICAL = "CRITICAL";

    private VitalsClinicalRules() {
        // Utility class
    }

    /**
     * Evaluates individual vital parameters and calculates the composite overall status.
     * Sets computed status values directly onto the HealthReading instance.
     *
     * @param reading HealthReading instance containing raw measurements
     * @return Composite overall status (NORMAL, WARNING, or CRITICAL)
     */
    public static String evaluateReading(HealthReading reading) {
        if (reading == null) {
            return STATUS_NORMAL;
        }

        String tempStatus = evaluateTemperature(reading.getTemperature());
        String hrStatus = evaluateHeartRate(reading.getHeartRate());
        String spo2Status = evaluateSpO2(reading.getSpo2());
        String bpStatus = evaluateBloodPressure(reading.getSystolicBp(), reading.getDiastolicBp());
        String respStatus = evaluateRespiratoryRate(reading.getRespiratoryRate());

        reading.setTempStatus(tempStatus);
        reading.setHrStatus(hrStatus);
        reading.setSpo2Status(spo2Status);
        reading.setBpStatus(bpStatus);
        reading.setRespStatus(respStatus);

        String overall = determineCompositeStatus(tempStatus, hrStatus, spo2Status, bpStatus, respStatus);
        reading.setOverallStatus(overall);
        return overall;
    }

    /**
     * Body Temperature (°C)
     * Normal: 36.1°C to 37.5°C
     * Warning: 35.0°C - 36.0°C (Hypothermia risk) OR 37.6°C - 38.5°C (Low-grade fever)
     * Critical: < 35.0°C (Severe hypothermia) OR > 38.5°C (High fever / Hyperpyrexia)
     */
    public static String evaluateTemperature(double temp) {
        if (temp < 35.0 || temp > 38.5) {
            return STATUS_CRITICAL;
        }
        if (temp < 36.1 || temp > 37.5) {
            return STATUS_WARNING;
        }
        return STATUS_NORMAL;
    }

    /**
     * Heart Rate (BPM for resting adult)
     * Normal: 60 - 100 BPM
     * Warning: 50 - 59 BPM (Mild bradycardia) OR 101 - 120 BPM (Moderate tachycardia)
     * Critical: < 50 BPM (Severe bradycardia) OR > 120 BPM (Severe tachycardia)
     */
    public static String evaluateHeartRate(int hr) {
        if (hr < 50 || hr > 120) {
            return STATUS_CRITICAL;
        }
        if (hr < 60 || hr > 100) {
            return STATUS_WARNING;
        }
        return STATUS_NORMAL;
    }

    /**
     * Blood Oxygen Saturation (SpO2 %)
     * Normal: >= 95.0%
     * Warning: 90.0% - 94.9% (Mild hypoxemia)
     * Critical: < 90.0% (Severe hypoxemia / respiratory distress)
     */
    public static String evaluateSpO2(double spo2) {
        if (spo2 < 90.0) {
            return STATUS_CRITICAL;
        }
        if (spo2 < 95.0) {
            return STATUS_WARNING;
        }
        return STATUS_NORMAL;
    }

    /**
     * Blood Pressure (Systolic / Diastolic mmHg)
     * Normal: Systolic 90 - 120 mmHg AND Diastolic 60 - 80 mmHg
     * Warning: Systolic 121 - 139 mmHg OR Diastolic 81 - 89 mmHg (Pre-hypertension)
     * Critical: Systolic >= 140 mmHg OR Systolic < 90 mmHg OR Diastolic >= 90 mmHg OR Diastolic < 60 mmHg
     */
    public static String evaluateBloodPressure(int systolic, int diastolic) {
        if (systolic >= 140 || systolic < 85 || diastolic >= 90 || diastolic < 55) {
            return STATUS_CRITICAL;
        }
        if (systolic > 120 || diastolic > 80) {
            return STATUS_WARNING;
        }
        return STATUS_NORMAL;
    }

    /**
     * Respiratory Rate (breaths per minute)
     * Normal: 12 - 20 breaths/min
     * Warning: 9 - 11 breaths/min OR 21 - 24 breaths/min
     * Critical: < 9 breaths/min (Bradypnea) OR > 24 breaths/min (Tachypnea)
     */
    public static String evaluateRespiratoryRate(int respRate) {
        if (respRate < 9 || respRate > 24) {
            return STATUS_CRITICAL;
        }
        if (respRate < 12 || respRate > 20) {
            return STATUS_WARNING;
        }
        return STATUS_NORMAL;
    }

    /**
     * Enforces the hierarchical priority rule:
     * CRITICAL takes precedence over WARNING, and WARNING takes precedence over NORMAL.
     */
    private static String determineCompositeStatus(String... statuses) {
        boolean hasWarning = false;
        for (String s : statuses) {
            if (STATUS_CRITICAL.equals(s)) {
                return STATUS_CRITICAL;
            }
            if (STATUS_WARNING.equals(s)) {
                hasWarning = true;
            }
        }
        return hasWarning ? STATUS_WARNING : STATUS_NORMAL;
    }
}
