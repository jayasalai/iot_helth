package com.college.iothealth.filter;

import com.college.iothealth.util.JsonUtil;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Security Filter for enforcing HTTP session authentication across protected endpoints.
 * Configured via WEB-INF/web.xml
 */
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        String relativePath = path.substring(contextPath.length());

        // Allow public resources: Login page, static assets, and auth/health endpoints
        if (isPublicPath(relativePath)) {
            chain.doFilter(req, res);
            return;
        }

        // Verify active HTTP session
        HttpSession session = request.getSession(false);
        boolean isAuthenticated = (session != null && session.getAttribute("user") != null);

        if (isAuthenticated) {
            chain.doFilter(req, res);
        } else {
            if (relativePath.startsWith("/api/")) {
                JsonUtil.sendError(response, HttpServletResponse.SC_UNAUTHORIZED, 
                        "Authentication required. Please log in to access this healthcare endpoint.");
            } else {
                response.sendRedirect(contextPath + "/login.html");
            }
        }
    }

    private boolean isPublicPath(String path) {
        return path.equals("/") ||
               path.equals("/login.html") ||
               path.startsWith("/login") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/images/") ||
               path.startsWith("/favicon.ico") ||
               path.equals("/api/auth/login") ||
               path.equals("/api/health");
    }

    @Override
    public void destroy() {}
}
