package com.college.iothealth.servlet;

import com.college.iothealth.dao.HealthReadingDAO;
import com.college.iothealth.dao.PatientDAO;
import com.college.iothealth.model.HealthReading;
import com.college.iothealth.model.Patient;
import com.college.iothealth.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller Servlet for Patient Demographic Management (CRUD).
 * Configured via WEB-INF/web.xml
 */
public class PatientServlet extends HttpServlet {

    private final PatientDAO patientDAO = new PatientDAO();
    private final HealthReadingDAO healthReadingDAO = new HealthReadingDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String idParam = req.getParameter("id");
        if (idParam != null && !idParam.trim().isEmpty()) {
            handleGetPatientById(req, resp, idParam.trim());
            return;
        }

        handleListPatients(req, resp);
    }

    private void handleGetPatientById(HttpServletRequest req, HttpServletResponse resp, String idStr) 
            throws IOException {
        try {
            int patientId = Integer.parseInt(idStr);
            Patient patient = patientDAO.getPatientById(patientId);
            if (patient == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Patient with ID " + patientId + " not found.");
                return;
            }

            HealthReading latest = healthReadingDAO.getLatestReadingForPatient(patientId);
            patient.setLatestReading(latest);

            JsonUtil.sendJsonResponse(resp, HttpServletResponse.SC_OK, 
                    new JsonUtil.ApiResponse(true, "Patient retrieved successfully", patient));

        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid patient ID format.");
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to retrieve patient: " + e.getMessage());
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    private void handleListPatients(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String search = req.getParameter("search");
        int page = 1;
        int pageSize = 10;

        String pageParam = req.getParameter("page");
        String sizeParam = req.getParameter("pageSize");

        if (pageParam != null) {
            try {
                page = Math.max(1, Integer.parseInt(pageParam));
            } catch (NumberFormatException ignored) {}
        }
        if (sizeParam != null) {
            try {
                pageSize = Math.min(100, Math.max(1, Integer.parseInt(sizeParam)));
            } catch (NumberFormatException ignored) {}
        }

        try {
            List<Patient> patients = patientDAO.getPatients(search, page, pageSize);
            int total = patientDAO.countPatients(search);
            int totalPages = (int) Math.ceil((double) total / pageSize);

            for (Patient p : patients) {
                p.setLatestReading(healthReadingDAO.getLatestReadingForPatient(p.getPatientId()));
            }

            Map<String, Object> result = new HashMap<>();
            result.put("patients", patients);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("total", total);
            result.put("totalPages", totalPages);

            JsonUtil.sendJsonResponse(resp, HttpServletResponse.SC_OK, 
                    new JsonUtil.ApiResponse(true, "Patients retrieved successfully", result));

        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to list patients: " + e.getMessage());
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        Patient patient = JsonUtil.fromJson(req, Patient.class);
        String validationError = validatePatient(patient, false);
        if (validationError != null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, validationError);
            return;
        }

        try {
            int newId = patientDAO.createPatient(patient);
            patient.setPatientId(newId);
            JsonUtil.sendJsonResponse(resp, HttpServletResponse.SC_CREATED, 
                    new JsonUtil.ApiResponse(true, "Patient registered successfully with ID #" + newId, patient));
        } catch (SQLException e) {
            System.err.println("[ERROR] Patient registration failed: " + e.getMessage());
            String msg = e.getMessage();
            if (e.getErrorCode() == 1 || msg.contains("unique constraint") || msg.contains("UQ_PATIENTS_EMAIL")) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_CONFLICT, "A patient with this email already exists.");
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + msg);
            }
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        Patient patient = JsonUtil.fromJson(req, Patient.class);
        String validationError = validatePatient(patient, true);
        if (validationError != null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, validationError);
            return;
        }

        try {
            boolean updated = patientDAO.updatePatient(patient);
            if (updated) {
                JsonUtil.sendJsonResponse(resp, HttpServletResponse.SC_OK, 
                        new JsonUtil.ApiResponse(true, "Patient updated successfully", patient));
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Patient ID " + patient.getPatientId() + " not found.");
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Patient update failed: " + e.getMessage());
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String idParam = req.getParameter("id");
        if (idParam == null || idParam.trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing required parameter 'id'.");
            return;
        }

        try {
            int patientId = Integer.parseInt(idParam.trim());
            boolean deleted = patientDAO.deletePatient(patientId);
            if (deleted) {
                JsonUtil.sendJsonResponse(resp, HttpServletResponse.SC_OK, 
                        new JsonUtil.ApiResponse(true, "Patient and associated telemetry deleted successfully."));
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Patient with ID " + patientId + " not found.");
            }
        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid patient ID format.");
        } catch (SQLException e) {
            System.err.println("[ERROR] Patient deletion failed: " + e.getMessage());
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database transaction error: " + e.getMessage());
        }
    }

    private String validatePatient(Patient p, boolean requireId) {
        if (p == null) {
            return "Patient data payload is empty.";
        }
        if (requireId && p.getPatientId() <= 0) {
            return "Valid patient ID is required for update.";
        }
        if (p.getFullName() == null || p.getFullName().trim().length() < 2) {
            return "Full name must be at least 2 characters long.";
        }
        if (p.getAge() <= 0 || p.getAge() > 125) {
            return "Age must be between 1 and 125.";
        }
        if (p.getGender() == null || (!p.getGender().equals("Male") && !p.getGender().equals("Female") && !p.getGender().equals("Other"))) {
            return "Gender must be 'Male', 'Female', or 'Other'.";
        }
        if (p.getContactNumber() == null || p.getContactNumber().trim().length() < 7) {
            return "Valid contact number is required.";
        }
        if (p.getEmail() == null || !p.getEmail().contains("@") || !p.getEmail().contains(".")) {
            return "Valid email address is required.";
        }
        if (p.getBloodGroup() == null || p.getBloodGroup().trim().isEmpty()) {
            return "Blood group is required.";
        }
        return null;
    }
}
