package com.college.iothealth;

import com.college.iothealth.model.HealthReading;
import com.college.iothealth.service.VitalsClinicalRules;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for clinical decision threshold rules and priority logic.
 */
public class VitalsClinicalRulesTest {

    @Test
    @DisplayName("Should classify all normal vitals as NORMAL")
    void testNormalVitals() {
        HealthReading r = new HealthReading();
        r.setTemperature(36.8);
        r.setHeartRate(72);
        r.setSpo2(98.5);
        r.setSystolicBp(115);
        r.setDiastolicBp(75);
        r.setRespiratoryRate(16);

        String status = VitalsClinicalRules.evaluateReading(r);
        assertEquals(VitalsClinicalRules.STATUS_NORMAL, status);
        assertEquals(VitalsClinicalRules.STATUS_NORMAL, r.getTempStatus());
        assertEquals(VitalsClinicalRules.STATUS_NORMAL, r.getHrStatus());
        assertEquals(VitalsClinicalRules.STATUS_NORMAL, r.getSpo2Status());
        assertEquals(VitalsClinicalRules.STATUS_NORMAL, r.getBpStatus());
        assertEquals(VitalsClinicalRules.STATUS_NORMAL, r.getRespStatus());
    }

    @Test
    @DisplayName("Should classify mild tachycardia as WARNING")
    void testWarningHeartRate() {
        HealthReading r = new HealthReading();
        r.setTemperature(37.0);
        r.setHeartRate(110); // Warning range (101-120)
        r.setSpo2(98.0);
        r.setSystolicBp(120);
        r.setDiastolicBp(78);
        r.setRespiratoryRate(16);

        String status = VitalsClinicalRules.evaluateReading(r);
        assertEquals(VitalsClinicalRules.STATUS_WARNING, status);
        assertEquals(VitalsClinicalRules.STATUS_WARNING, r.getHrStatus());
    }

    @Test
    @DisplayName("Critical vital parameter must trigger CRITICAL overall status even if others are normal")
    void testCriticalPriorityRule() {
        HealthReading r = new HealthReading();
        r.setTemperature(36.6); // Normal
        r.setHeartRate(72);     // Normal
        r.setSpo2(87.0);        // CRITICAL (< 90%)
        r.setSystolicBp(118);   // Normal
        r.setDiastolicBp(76);   // Normal
        r.setRespiratoryRate(16); // Normal

        String status = VitalsClinicalRules.evaluateReading(r);
        assertEquals(VitalsClinicalRules.STATUS_CRITICAL, status);
        assertEquals(VitalsClinicalRules.STATUS_CRITICAL, r.getSpo2Status());
    }

    @Test
    @DisplayName("High fever must be detected as CRITICAL")
    void testHighFeverCritical() {
        HealthReading r = new HealthReading();
        r.setTemperature(39.2); // Critical (> 38.5)
        r.setHeartRate(130);    // Critical (> 120)
        r.setSpo2(95.0);
        r.setSystolicBp(135);
        r.setDiastolicBp(85);
        r.setRespiratoryRate(26); // Critical (> 24)

        String status = VitalsClinicalRules.evaluateReading(r);
        assertEquals(VitalsClinicalRules.STATUS_CRITICAL, status);
    }
}
