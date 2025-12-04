package com.se310.store.controller;

import com.se310.store.dto.StoreMapper;
import com.se310.store.dto.StoreMapper.StoreDTO;
import com.se310.store.service.AuthenticationService;
import com.se310.store.model.Store;
import com.se310.store.model.User;
import com.se310.store.model.StoreException;
import com.se310.store.service.StoreService;
import com.se310.store.servlet.BaseServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * REST API controller for Store operations
 * Implements full CRUD operations using DTO Pattern
 *
 * DTOs are used to:
 * - Simplify API responses by excluding complex nested collections
 * - Provide a clean separation between internal domain models and external API contracts
 * - Improve JSON serialization performance by excluding transient fields
 *
 * @author  Sergey L. Sundukovskiy
 * @version 1.0
 * @since   2025-11-11
 */
public class StoreController extends BaseServlet {

    //TODO: Implement Controller for Store operations, part of the MVC Pattern

    private final StoreService storeService;
    private final AuthenticationService authenticationService;

    public StoreController(StoreService storeService, AuthenticationService authenticationService) {
        this.storeService = storeService;
        this.authenticationService = authenticationService;
    }

    /**
     * Handle GET requests - Returns StoreDTO objects
     * - GET /api/v1/stores (no parameters) - Get all stores
     * - GET /api/v1/stores/{storeId} - Get store by ID
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // Authenticate
                Optional<User> userOpt = authenticate(request, response);
                if (userOpt.isEmpty()) {
                    return; // 401 already sent
                }

            User user = userOpt.get();

            String storeId = extractResourceId(request);
            
            if (storeId == null) {
                // Authphorize
                if (!authenticationService.canViewStores(user)) {
                    sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                            "Not authorized to view stores");
                    return;
                }

                // Get all stores
                Collection<Store> stores = storeService.getAllStores();
                Collection<StoreDTO> storeDTOs = stores.stream()
                    .map(StoreMapper::toDTO)
                    .collect(Collectors.toList());
                sendJsonResponse(response, storeDTOs);
            } else {
                // Authorize single store view
                if (!authenticationService.canViewStore(user, storeId)) {
                    sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                            "Not authorized to view store: " + storeId);
                    return;
                }

                // Get specific store
                Store store = storeService.showStore(storeId, null);
                if (store == null) {
                    sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, 
                        "Store not found: " + storeId);
                    return;
                }
                StoreDTO storeDTO = StoreMapper.toDTO(store);
                sendJsonResponse(response, storeDTO);
            }
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    /**
     * Handle POST requests - Create new store, returns StoreDTO
     * POST /api/v1/stores?storeId=xxx&name=xxx&address=xxx
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // Authenticate
            Optional<User> userOpt = authenticate(request, response);
            if (userOpt.isEmpty()) {
                return; // 401 already sent
            }
            User user = userOpt.get();

            // Authorize 
            if (!authenticationService.canCreateStore(user)) {
                sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                        "Not authorized to create stores");
                return;
            }

            // Get parameters from request
            String storeId = request.getParameter("storeId");
            String name = request.getParameter("name");
            String address = request.getParameter("address");
            
            // Basic validation
            if (storeId == null || name == null || address == null) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "Missing required parameters: storeId, name, address");
                return;
            }
            
            // Create store through service
            Store store = storeService.provisionStore(storeId, name, address, null);
            StoreDTO storeDTO = StoreMapper.toDTO(store);
            sendJsonResponse(response, storeDTO, HttpServletResponse.SC_CREATED);
            
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    /**
     * Handle PUT requests - Update existing store, returns StoreDTO
     * PUT /api/v1/stores/{storeId}?description=xxx&address=xxx
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // Authenticate
            Optional<User> userOpt = authenticate(request, response);
            if (userOpt.isEmpty()) {
                return; // 401 already sent
            }
            User user = userOpt.get();

            String storeId = extractResourceId(request);
            if (storeId == null) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "Store ID is required in path");
                return;
            }

            // Authorize
            if (!authenticationService.canUpdateStore(user, storeId)) {
                sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                        "Not authorized to update store: " + storeId);
                return;
            }
            
            // Get update parameters
            String description = request.getParameter("description");
            String address = request.getParameter("address");
            
            // Update store through service
            Store updatedStore = storeService.updateStore(storeId, description, address);
            if (updatedStore == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, 
                    "Store not found: " + storeId);
                return;
            }
            
            StoreDTO storeDTO = StoreMapper.toDTO(updatedStore);
            sendJsonResponse(response, storeDTO);
            
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    /**
     * Handle DELETE requests - Delete store
     * DELETE /api/v1/stores/{storeId}
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // Authenticate
            Optional<User> userOpt = authenticate(request, response);
            if (userOpt.isEmpty()) {
                return; // 401 already sent
            }
            User user = userOpt.get();

            String storeId = extractResourceId(request);
            if (storeId == null) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "Store ID is required in path");
                return;
            }

            // Authorize
            if (!authenticationService.canDeleteStore(user, storeId)) {
                sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                        "Not authorized to delete store: " + storeId);
                return;
            }
            
            // Delete through service
            storeService.deleteStore(storeId);
            
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