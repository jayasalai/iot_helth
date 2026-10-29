package com.college.iothealth.servlet;

import com.college.iothealth.dao.HealthReadingDAO;
import com.college.iothealth.dao.PatientDAO;
import com.college.iothealth.model.HealthReading;
import com.college.iothealth.model.Patient;
import com.college.iothealth.service.VitalsClinicalRules;
import com.college.iothealth.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller Servlet for Simulated IoT Vitals Telemetry and Historical Clinical Reports.
 * Configured via WEB-INF/web.xml
 */
public class HealthReadingServlet extends HttpServlet {

    private final HealthReadingDAO healthReadingDAO = new HealthReadingDAO();
    private final PatientDAO patientDAO = new PatientDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            pathInfo = "";
        }

        if ("/abnormal".equals(pathInfo)) {
            handleGetAbnormalReadings(req, resp);
            return;
        }

        String patientIdParam = req.getParameter("patientId");
        if (patientIdParam == null || patientIdParam.trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Parameter 'patientId' is required.");
            return;
        }

        try {
            int patientId = Integer.parseInt(patientIdParam.trim());
            Patient patient = patientDAO.getPatientById(patientId);
            if (patient == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Patient #" + patientId + " not found.");
                return;
            }

            String fromDate = req.getParameter("from");
            String toDate = req.getParameter("to");

            List<HealthReading> history = healthReadingDAO.getReadingsByPatient(patientId, fromDate, toDate);
            HealthReading latest = healthReadingDAO.getLatestReadingForPatient(patientId);

            Map<String, Object> data = new HashMap<>();
            data.put("patient", patient);
            data.put("history", history);
            data.put("latestReading", latest);
            data.put("totalReadings", history.size());

            JsonUtil.sendJsonResponse(resp, HttpServletResponse.SC_OK, 
                    new JsonUtil.ApiResponse(true, "Vitals telemetry history retrieved", data));

        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid patientId format.");
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to query vitals history: " + e.getMessage());
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    private void handleGetAbnormalReadings(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String limitParam = req.getParameter("limit");
        int limit = 10;
        if (limitParam != null) {
            try {
                limit = Math.min(50, Math.max(1, Integer.parseInt(limitParam.trim())));
            } catch (NumberFormatException ignored) {}
        }

        try {
            List<HealthReading> abnormalList = healthReadingDAO.getAbnormalReadings(limit);
            JsonUtil.sendJsonResponse(resp, HttpServletResponse.SC_OK, 
                    new JsonUtil.ApiResponse(true, "Abnormal health readings retrieved", abnormalList));
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to query abnormal vitals: " + e.getMessage());
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        HealthReading reading = JsonUtil.fromJson(req, HealthReading.class);
        String validationError = validateReading(reading);
        if (validationError != null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, validationError);
            return;
        }

        try {
            Patient patient = patientDAO.getPatientById(reading.getPatientId());
            if (patient == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, 
                        "Cannot record telemetry: Patient ID #" + reading.getPatientId() + " does not exist.");
                return;
            }

            if (reading.getRecordedAt() == null) {
                reading.setRecordedAt(new Timestamp(System.currentTimeMillis()));
            }

            VitalsClinicalRules.evaluateReading(reading);
            reading.setPatientName(patient.getFullName());

            int readingId = healthReadingDAO.insertReading(reading);
            reading.setReadingId(readingId);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("reading", reading);
            responseData.put("calculatedStatus", reading.getOverallStatus());
            responseData.put("triageBreakdown", Map.of(
                    "temperature", reading.getTempStatus(),
                    "heartRate", reading.getHrStatus(),
                    "spO2", reading.getSpo2Status(),
                    "bloodPressure", reading.getBpStatus(),
                    "respiratoryRate", reading.getRespStatus()
            ));

            JsonUtil.sendJsonResponse(resp, HttpServletResponse.SC_CREATED, 
                    new JsonUtil.ApiResponse(true, "Simulated IoT sensor reading logged successfully (Status: " + 
                            reading.getOverallStatus() + ")", responseData));

        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to insert simulated vitals: " + e.getMessage());
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    private String validateReading(HealthReading r) {
        if (r == null) {
            return "Telemetry payload is empty.";
        }
        if (r.getPatientId() <= 0) {
            return "Valid registered Patient ID is required.";
        }
        if (r.getTemperature() < 30.0 || r.getTemperature() > 45.0) {
            return "Temperature must be between 30.0 °C and 45.0 °C.";
        }
        if (r.getHeartRate() < 30 || r.getHeartRate() > 250) {
            return "Heart rate must be between 30 and 250 BPM.";
        }
        if (r.getSpo2() < 50.0 || r.getSpo2() > 100.0) {
            return "SpO2 percentage must be between 50.0% and 100.0%.";
        }
        if (r.getSystolicBp() < 50 || r.getSystolicBp() > 260) {
            return "Systolic BP must be between 50 and 260 mmHg.";
        }
        if (r.getDiastolicBp() < 30 || r.getDiastolicBp() > 160) {
            return "Diastolic BP must be between 30 and 160 mmHg.";
        }
        if (r.getDiastolicBp() >= r.getSystolicBp()) {
            return "Diastolic BP must be strictly less than Systolic BP.";
        }
        if (r.getRespiratoryRate() < 5 || r.getRespiratoryRate() > 60) {
            return "Respiratory rate must be between 5 and 60 breaths/min.";
        }
        return null;
    }
}
