package com.se310.store.servlet;

import com.se310.store.dto.JsonHelper;
import com.se310.store.dto.JsonSerializable;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Base servlet providing common functionality for all API servlets.
 *
 * This class demonstrates the Template Method Pattern and provides reusable
 * methods for handling HTTP requests and responses in a RESTful manner.
 *
 * Key Design Principles:
 * - Template Method Pattern: Defines the skeleton of HTTP handling
 * - DRY Principle: Common JSON/HTTP logic centralized here
 * - Single Responsibility: Handles HTTP communication concerns
 * - Open/Closed Principle: Open for extension (new DTOs), closed for modification
 * - Dependency Inversion: Depends on JsonSerializable abstraction, not concrete DTOs
 *
 * @author  Sergey L. Sundukovskiy
 * @version 1.0
 * @since   2025-11-11
 */
public abstract class BaseServlet extends HttpServlet {

    //TODO: Implement Template Method Pattern for handling HTTP requests and responses

    /**
     * Helper method to get JSON body from request and parse it.
     * Centralizes request body reading and basic error handling.
     * 
     * @param request The HTTP request
     * @return JSON string from request body
     * @throws IOException If reading fails
     */
    protected String getJsonBody(HttpServletRequest request) throws IOException {
        return readRequestBody(request);
    }
    
    /**
     * Helper method to handle common servlet exceptions.
     * Maps exceptions to appropriate HTTP status codes.
     * 
     * @param response The HTTP response
     * @param e The exception to handle
     * @throws IOException If writing response fails
     */
    protected void handleException(HttpServletResponse response, Exception e) throws IOException {
        // Default to 500 Internal Server Error
        int statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        String message = "Internal server error";
        
        // Map specific exceptions to appropriate status codes
        if (e instanceof IllegalArgumentException) {
            statusCode = HttpServletResponse.SC_BAD_REQUEST;
            message = "Invalid request: " + e.getMessage();
        } else if (e.getMessage() != null && e.getMessage().contains("not found")) {
            statusCode = HttpServletResponse.SC_NOT_FOUND;
            message = e.getMessage();
        }
        
        sendErrorResponse(response, statusCode, message);
    }

    /**
     * Read the request body as a string.
     * Used for parsing JSON payloads from POST/PUT requests.
     *
     * @param request The HTTP request
     * @return The request body as a string
     * @throws IOException If reading fails
     */
    protected String readRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        return buffer.toString();
    }

    /**
     * Send a JSON response with HTTP 200 OK status.
     *
     * @param response The HTTP response
     * @param object The object to serialize to JSON
     * @throws IOException If writing fails
     */
    protected void sendJsonResponse(HttpServletResponse response, Object object) throws IOException {
        sendJsonResponse(response, object, HttpServletResponse.SC_OK);
    }

    /**
     * Send a JSON response with a specified HTTP status code.
     *
     * This method follows the Open/Closed Principle and Dependency Inversion Principle:
     * - It depends on the JsonSerializable abstraction, not concrete DTO implementations
     * - New DTOs can be added without modifying this code
     * - BaseServlet has no knowledge of specific DTO types
     *
     * @param response The HTTP response
     * @param object The object to serialize to JSON (preferably a JsonSerializable)
     * @param statusCode The HTTP status code (200, 201, 400, etc.)
     * @throws IOException If writing fails
     */
    protected void sendJsonResponse(HttpServletResponse response, Object object, int statusCode) throws IOException {
        // Set response type and encoding
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(statusCode);
        
        // Convert to JSON string
        String jsonString;
        if (object instanceof JsonSerializable) {
            jsonString = ((JsonSerializable) object).toJson();
        } else {
            jsonString = JsonHelper.toJson(object);
        }
        
        // Write response
        PrintWriter writer = response.getWriter();
        writer.write(jsonString);
        writer.flush();
    }

    /**
     * Send an error response with a message.
     * Uses ErrorResponse's own toJson() method, following the DTO pattern.
     *
     * @param response The HTTP response
     * @param statusCode The HTTP status code (400, 404, 500, etc.)
     * @param message The error message
     * @throws IOException If writing fails
     */
    protected void sendErrorResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
        // Create error object and send as JSON
        ErrorResponse error = new ErrorResponse(statusCode, message);
        sendJsonResponse(response, error, statusCode);
    }

    /**
     * Extract the resource ID from the request path.
     * For example, "/api/v1/deals/DEAL-001" returns "DEAL-001"
     *
     * @param request The HTTP request
     * @return The resource ID, or null if not present
     */
    protected String extractResourceId(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            return null;
        }

        // Remove leading slash and extract ID
        String[] parts = pathInfo.substring(1).split("/");
        return parts.length > 0 ? parts[0] : null;
    }

    /**
     * Simple error response object for consistent error formatting.
     */
    @Getter
    private static class ErrorResponse implements JsonSerializable {
        private final int status;
        private final String message;
        private final long timestamp;

        public ErrorResponse(int status, String message) {
            this.status = status;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }
}