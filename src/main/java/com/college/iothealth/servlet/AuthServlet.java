package com.college.iothealth.servlet;

import com.college.iothealth.dao.AuthDAO;
import com.college.iothealth.model.User;
import com.college.iothealth.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles user authentication, session initiation, and secure logout.
 * Configured via WEB-INF/web.xml
 */
public class AuthServlet extends HttpServlet {

    private final AuthDAO authDAO = new AuthDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            pathInfo = "";
        }

        switch (pathInfo) {
            case "/login":
                handleLogin(req, resp);
                break;
            case "/logout":
                handleLogout(req, resp);
                break;
            default:
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Auth action not supported: " + pathInfo);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String pathInfo = req.getPathInfo();
        if ("/me".equals(pathInfo)) {
            handleGetCurrentUser(req, resp);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Auth endpoint not found: " + pathInfo);
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LoginRequest credentials = JsonUtil.fromJson(req, LoginRequest.class);

        if (credentials == null || credentials.username == null || credentials.password == null ||
            credentials.username.trim().isEmpty() || credentials.password.trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
            return;
        }

        try {
            User user = authDAO.authenticate(credentials.username, credentials.password);
            if (user != null) {
                HttpSession oldSession = req.getSession(false);
                if (oldSession != null) {
                    oldSession.invalidate();
                }

                HttpSession session = req.getSession(true);
                session.setAttribute("user", user);
                session.setMaxInactiveInterval(60 * 60);

                Map<String, Object> data = new HashMap<>();
                data.put("userId", user.getUserId());
                data.put("username", user.getUsername());
                data.put("email", user.getEmail());
                data.put("role", user.getRole());

                JsonUtil.sendJsonResponse(resp, HttpServletResponse.SC_OK, 
                        new JsonUtil.ApiResponse(true, "Login successful. Welcome, " + user.getUsername(), data));
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid username, email, or password.");
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Login error: " + e.getMessage());
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "Database error during authentication: " + e.getMessage());
        }
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        JsonUtil.sendJsonResponse(resp, HttpServletResponse.SC_OK, 
                new JsonUtil.ApiResponse(true, "Logged out successfully."));
    }

    private void handleGetCurrentUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            User user = (User) session.getAttribute("user");
            Map<String, Object> data = new HashMap<>();
            data.put("userId", user.getUserId());
            data.put("username", user.getUsername());
            data.put("email", user.getEmail());
            data.put("role", user.getRole());
            JsonUtil.sendJsonResponse(resp, HttpServletResponse.SC_OK, 
                    new JsonUtil.ApiResponse(true, "Active session authenticated", data));
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "No active authenticated session.");
        }
    }

    private static class LoginRequest {
        String username;
        String password;
    }
}
