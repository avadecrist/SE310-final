package com.se310.store.controller;

import com.se310.store.dto.UserMapper;
import com.se310.store.dto.UserMapper.UserDTO;
import com.se310.store.model.User;
import com.se310.store.model.UserRole;
import com.se310.store.service.AuthenticationService;
import com.se310.store.servlet.BaseServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.Optional;        

/**
 * REST API controller for User operations
 * Implements full CRUD operations using DTO Pattern
 *
 * DTOs are used to:
 * - Hide sensitive information (passwords) from API responses
 * - Provide a clean separation between internal domain models and external API contracts
 * - Allow API responses to evolve independently from internal data structures
 *
 * @author  Sergey L. Sundukovskiy
 * @version 1.0
 * @since   2025-11-11
 */
public class UserController extends BaseServlet {

    //TODO: Implement Controller for User operations, part of the MVC Pattern

    private final AuthenticationService authenticationService;

    public UserController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Handle GET requests - Returns UserDTO objects (without passwords)
     * - GET /api/v1/users (no parameters) - Get all users
     * - GET /api/v1/users/{email} - Get user by email
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // Authenticate
            Optional<User> userOpt = authenticate(request, response);
            if (userOpt.isEmpty()) {
                return; // 401 already sent
            }
            User currentUser = userOpt.get();

            String email = extractResourceId(request);
            
            if (email == null) {
                // Get all users
                Collection<User> users = authenticationService.getAllUsers();
                Collection<UserDTO> userDTOs = users.stream()
                    .map(UserMapper::toDTO)
                    .collect(Collectors.toList());
                sendJsonResponse(response, userDTOs);
            } else {
                // Get specific user
                User user = authenticationService.getUserByEmail(email);
                if (user == null) {
                    sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, 
                        "User not found: " + email);
                    return;
                }
                UserDTO userDTO = UserMapper.toDTO(user);
                sendJsonResponse(response, userDTO);
            }
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    /**
     * Handle POST requests - Register new user, returns UserDTO (without password)
     * POST /api/v1/users?email=xxx&password=xxx&name=xxx&role=xxx
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // Get parameters from request
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String name = request.getParameter("name");
            String roleStr = request.getParameter("role");

            // Basic validation
            if (email == null || password == null || name == null) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Missing required parameters: email, password, name");
                return;
            }

            // Convert role to UserRole enum (default to USER if not provided)
            UserRole role;
            if (roleStr != null && !roleStr.isBlank()) {
                try {
                    role = UserRole.valueOf(roleStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                            "Invalid role: " + roleStr + ". Allowed: ADMIN, MANAGER, USER");
                    return;
                }
            } else {
                role = UserRole.USER;
            }

            // Create user through authentication service
            User user = authenticationService.registerUser(email, password, name, role);
            UserDTO userDTO = UserMapper.toDTO(user);
            sendJsonResponse(response, userDTO, HttpServletResponse.SC_CREATED);

        } catch (Exception e) {
            handleException(response, e);
        }
    }

    /**
     * Handle PUT requests - Update user information, returns UserDTO (without password)
     * PUT /api/v1/users/{email}?password=xxx&name=xxx
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // Authenticate
            Optional<User> userOpt = authenticate(request, response);
            if (userOpt.isEmpty()) {
                return; // 401 already sent
            }
            User currentUser = userOpt.get();

            String email = extractResourceId(request);
            if (email == null) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "Email is required in path");
                return;
            }

            // ADMIN can update anyone, normal users can only update themselves
            if (currentUser.getRole() != UserRole.ADMIN &&
                    !currentUser.getEmail().equalsIgnoreCase(email)) {
                sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                        "Not authorized to update this user");
                return;
            }
            
            // Get update parameters
            String password = request.getParameter("password");
            String name = request.getParameter("name");
            
            // Update user through authentication service
            User updatedUser = authenticationService.updateUser(email, password, name);
            if (updatedUser == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, 
                    "User not found: " + email);
                return;
            }
            
            UserDTO userDTO = UserMapper.toDTO(updatedUser);
            sendJsonResponse(response, userDTO);
            
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    /**
     * Handle DELETE requests - Delete user
     * DELETE /api/v1/users/{email}
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // Authenticate
            Optional<User> userOpt = authenticate(request, response);
            if (userOpt.isEmpty()) {
                return; // 401 already sent
            }
            User currentUser = userOpt.get();

            String email = extractResourceId(request);
            if (email == null) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "Email is required in path");
                return;
            }
            
            // ADMIN can delete any user; a user can delete themselves
            if (currentUser.getRole() != UserRole.ADMIN &&
                    !currentUser.getEmail().equalsIgnoreCase(email)) {
                sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                        "Not authorized to delete this user");
                return;
            }

            // Delete through authentication service
            boolean deleted = authenticationService.deleteUser(email);
            if (!deleted) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, 
                    "User not found: " + email);
                return;
            }
            
            // Send success response with no content
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    /**
     * Helper: authenticate the user using HTTP Basic auth.
     * If authentication fails, sends 401 and returns Optional.empty().
     */
    private Optional<User> authenticate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String authHeader = request.getHeader("Authorization");
        Optional<User> userOpt = authenticationService.authenticateBasic(authHeader);

        if (userOpt.isEmpty()) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing credentials");
        }

        return userOpt;
    }
}