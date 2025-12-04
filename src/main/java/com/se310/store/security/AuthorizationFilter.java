package com.se310.store.security;

import com.se310.store.model.User;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * AuthorizationFilter - Servlet filter that enforces role-based access control
 *
 * NOTE: This filter is NO LONGER registered for Store and User API endpoints.
 * Store and User APIs now only require authentication, not ADMIN role.
 *
 * This filter was previously used to ensure that only ADMIN users could perform
 * Store and User management operations, but this requirement has been removed.
 *
 * @author  Sergey L. Sundukovskiy
 * @version 1.0
 * @since   2025-11-11
 */
public class AuthorizationFilter implements Filter { 

    //TODO: Only ADMIN users can perform operations associated with Store building

    /**
     * Initialize filter
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization needed
    }

    /**
     * Filter method that checks if the authenticated user has ADMIN role
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Retrieve authenticated user from request attribute
        // (should be set by AuthenticationFilter)
        User user = (User) request.getAttribute("authenticatedUser");

        if (user == null) {
            // No authenticated user - should not happen if AuthenticationFilter runs first
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json");
            httpResponse.setCharacterEncoding("UTF-8");
            httpResponse.getWriter().write("{\"error\": \"Unauthorized - Authentication required\"}");
            return;
        }

        //TODO: Check if user has ADMIN role

        // User is authorized - continue with the filter chain
        chain.doFilter(request, response);
    }

    /**
     * Cleanup method called when filter is destroyed
     */
    @Override
    public void destroy() {
        // No cleanup needed
    }
}
