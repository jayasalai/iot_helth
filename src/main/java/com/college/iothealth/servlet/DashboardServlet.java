package com.college.iothealth.servlet;

import com.college.iothealth.dao.DashboardDAO;
import com.college.iothealth.model.DashboardStats;
import com.college.iothealth.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Controller Servlet for Dashboard Analytics & Triage Aggregates.
 * Configured via WEB-INF/web.xml
 */
public class DashboardServlet extends HttpServlet {

    private final DashboardDAO dashboardDAO = new DashboardDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        try {
            DashboardStats stats = dashboardDAO.getDashboardStatistics();
            JsonUtil.sendJsonResponse(resp, HttpServletResponse.SC_OK, 
                    new JsonUtil.ApiResponse(true, "Dashboard statistics retrieved successfully", stats));
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to calculate dashboard stats: " + e.getMessage());
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "Database error while querying dashboard aggregates: " + e.getMessage());
        }
    }
}
