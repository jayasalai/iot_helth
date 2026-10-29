package com.college.iothealth.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Utility class for JSON formatting and HTTP request/response handling.
 */
public class JsonUtil {

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .serializeNulls()
            .create();

    private JsonUtil() {
        // Prevent instantiation
    }

    public static Gson getGson() {
        return gson;
    }

    /**
     * Reads JSON request body and parses into the specified class.
     */
    public static <T> T fromJson(HttpServletRequest request, Class<T> classOfT) throws IOException, JsonSyntaxException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return gson.fromJson(sb.toString(), classOfT);
    }

    /**
     * Writes an object as JSON to the HttpServletResponse with appropriate headers.
     */
    public static void sendJsonResponse(HttpServletResponse response, int statusCode, Object data) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(statusCode);
        try (PrintWriter out = response.getWriter()) {
            out.print(gson.toJson(data));
            out.flush();
        }
    }

    /**
     * Sends a standardised JSON error message.
     */
    public static void sendError(HttpServletResponse response, int statusCode, String message) throws IOException {
        sendJsonResponse(response, statusCode, new ErrorResponse(false, message));
    }

    /**
     * Standard response container for simple boolean/message operations.
     */
    public static class ApiResponse {
        private final boolean success;
        private final String message;
        private final Object data;

        public ApiResponse(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public ApiResponse(boolean success, String message) {
            this(success, message, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Object getData() {
            return data;
        }
    }

    public static class ErrorResponse {
        private final boolean success;
        private final String error;

        public ErrorResponse(boolean success, String error) {
            this.success = success;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getError() {
            return error;
        }
    }
}
