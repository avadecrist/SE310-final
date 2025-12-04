package com.se310.store;

import com.se310.store.controller.StoreController;
import com.se310.store.controller.UserController;
import com.se310.store.config.ConfigLoader;
import com.se310.store.data.DataManager;
import com.se310.store.config.SampleDataLoader;
import com.se310.store.repository.StoreRepository;
import com.se310.store.repository.UserRepository;
import com.se310.store.security.AuthenticationFilter;
import com.se310.store.security.AuthorizationFilter;
import com.se310.store.service.AuthenticationService;
import com.se310.store.service.StoreService;
import com.se310.store.servlet.SwaggerServlet;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.h2.server.web.JakartaWebServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Main class responsible for starting and managing the Smart Store Application.
 * Provides multiple application launch methods and sets up the server lifecycle, dependency injection,
 * and application-layer configurations for proper operation of the entire system.
 *
 * @author  Sergey L. Sundukovskiy
 * @version 1.0
 * @since   2025-11-11
 */

public class SmartStoreApplication {

    private static final Logger logger = LoggerFactory.getLogger(SmartStoreApplication.class);
    private static final int PORT = ConfigLoader.getServerPort();

    private Tomcat tomcat;

    public static void main(String[] args) {
        SmartStoreApplication app = new SmartStoreApplication();
        try {
            app.start();
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            System.exit(1);
        }
    }

    /**
     * Starts the application.
     * Demonstrates the complete application lifecycle.
     * Blocks until server shutdown (suitable for main application).
     */
    public void start() throws LifecycleException {
        startServer(true); // blocking mode
    }

    /**
     * Starts the application without blocking.
     * Suitable for testing where you need the server running but want to continue execution.
     */
    public void startNonBlocking() throws LifecycleException {
        startServer(false); // non-blocking mode
    }

    /**
     * Internal method to start the server with optional blocking.
     *
     * @param block if true, blocks until server shutdown; if false, returns immediately
     */
    private void startServer(boolean block) throws LifecycleException {
        logger.info("Starting Smart Store Application...");

        // Step 1: Initialize database
        logger.info("Initializing database...");
        DataManager dataManager = DataManager.getInstance();

        // Step 2: Create repositories (Data Access Layer)
        logger.info("Creating repositories...");
        StoreRepository storeRepository = new StoreRepository(dataManager);
        UserRepository userRepository = new UserRepository(dataManager);

        // Step 3: Create services (Business Logic Layer)
        logger.info("Creating services...");
        StoreService storeService = new StoreService(storeRepository);
        AuthenticationService userService = new AuthenticationService(userRepository, storeRepository);

        // Step 4: Create controllers (Presentation Layer)
        logger.info("Creating controllers...");
        StoreController storeController = new StoreController(storeService, userService);
        UserController userController = new UserController(userService);

        // Step 5: Configure and start Tomcat
        logger.info("Configuring Tomcat server...");
        tomcat = new Tomcat();
        tomcat.setPort(PORT);
        tomcat.getConnector(); // Initialize default connector

        // Create context
        String contextPath = "";
        String docBase = new File(".").getAbsolutePath();
        Context context = tomcat.addContext(contextPath, docBase);

        // Register Store Controller servlet
        Tomcat.addServlet(context, "storeController", storeController);
        context.addServletMappingDecoded("/api/v1/stores/*", "storeController");

        // Register User Controller servlet
        Tomcat.addServlet(context, "userController", userController);
        context.addServletMappingDecoded("/api/v1/users/*", "userController");

        // Register H2 Console servlet (web interface for database)
        // Accessible at: http://localhost:8080/h2-console
        JakartaWebServlet h2Servlet = new JakartaWebServlet();
        Tomcat.addServlet(context, "h2Console", h2Servlet);
        context.addServletMappingDecoded("/h2-console/*", "h2Console");

        // Register Swagger UI servlet
        SwaggerServlet swaggerServlet = new SwaggerServlet();
        Tomcat.addServlet(context, "swagger", swaggerServlet);
        context.addServletMappingDecoded("/swagger-ui/*", "swagger");
        context.addServletMappingDecoded("/api-docs", "swagger");

        // Register WebJars servlet for serving static resources (CSS, JS from dependencies)
        org.apache.catalina.servlets.DefaultServlet webJarsServlet = new org.apache.catalina.servlets.DefaultServlet();
        Tomcat.addServlet(context, "webJarsServlet", webJarsServlet);
        context.addServletMappingDecoded("/webjars/*", "webJarsServlet");

        // Configure Authentication Filter for H2 Console and Swagger UI
        // These development tools should only be accessible to authenticated users
        logger.info("Configuring authentication filters...");

        // Store the authentication service in servlet context for filter access
        context.getServletContext().setAttribute("authenticationService", userService);

        // Create and register authentication filter
        AuthenticationFilter authFilter = new AuthenticationFilter(userService);
        FilterDef authFilterDef = new FilterDef();
        authFilterDef.setFilterName("authenticationFilter");
        authFilterDef.setFilter(authFilter);
        context.addFilterDef(authFilterDef);

        // Map authentication filter to H2 Console
        FilterMap h2FilterMap = new FilterMap();
        h2FilterMap.setFilterName("authenticationFilter");
        h2FilterMap.addURLPattern("/h2-console/*");
        context.addFilterMap(h2FilterMap);

        // Map authentication filter to Swagger UI
        FilterMap swaggerFilterMap = new FilterMap();
        swaggerFilterMap.setFilterName("authenticationFilter");
        swaggerFilterMap.addURLPattern("/swagger-ui/*");
        swaggerFilterMap.addURLPattern("/api-docs");
        context.addFilterMap(swaggerFilterMap);

        logger.info("Authentication required for H2 Console and Swagger UI");

        //TODO: Configure Authentication Filter for Store API
        logger.info("Configuring authentication filter for Store API...");

        // Map authentication filter to Store API
        FilterMap storeAuthFilterMap = new FilterMap();
        storeAuthFilterMap.setFilterName("authenticationFilter");
        storeAuthFilterMap.addURLPattern("/api/v1/stores/*");
        context.addFilterMap(storeAuthFilterMap);

        //TODO: Create and register Authorization Filter

        //TODO: Configure Authentication Filter for User API
        logger.info("Configuring authentication filter for User API...");

        // Map authentication filter to User API
        FilterMap userAuthFilterMap = new FilterMap();
        userAuthFilterMap.setFilterName("authenticationFilter");
        userAuthFilterMap.addURLPattern("/api/v1/users/*");
        context.addFilterMap(userAuthFilterMap);

        //TODO: Map Authorization filter to User API

        // Step 6: Load sample data
        logger.info("Loading sample data...");
        loadSampleData(userService, storeService);

        // Step 7: Start Tomcat
        tomcat.start();

        logger.info("=".repeat(80));
        logger.info("Smart Store Application started successfully!");
        logger.info("=".repeat(80));
        logger.info("Server running on: http://localhost:{}", PORT);
        logger.info("");
        logger.info("Available endpoints:");
        logger.info("  - Store API:      http://localhost:{}/api/v1/stores (Authentication Required)", PORT);
        logger.info("  - User API:       http://localhost:{}/api/v1/users (Authentication Required)", PORT);
        logger.info("  - Swagger UI:     http://localhost:{}/swagger-ui/ (Authentication Required)", PORT);
        logger.info("  - API Docs:       http://localhost:{}/api/docs/ (Authentication Required)", PORT);
        logger.info("  - H2 Console:     http://localhost:{}/h2-console/ (Authentication Required)", PORT);
        logger.info("");
        logger.info("H2 Database Connection Info:");
        logger.info("  - JDBC URL:       {}", ConfigLoader.getDbUrl());
        logger.info("  - Username:       {}", ConfigLoader.getDbUser());
        logger.info("  - Password:       {}", ConfigLoader.getDbPassword().isEmpty() ? "(leave blank)" : "***");
        logger.info("");
        logger.info("AUTHENTICATION & AUTHORIZATION:");
        logger.info("  H2 Console and Swagger UI require HTTP Basic Authentication");
        logger.info("  Store API and User API require HTTP Basic Authentication only");
        logger.info("  Your browser will prompt for username and password");
        logger.info("");
        logger.info("Test Credentials (from application.properties):");
        logger.info("  ADMIN:    {} / {} (Full access)", ConfigLoader.getAdminEmail(), ConfigLoader.getAdminPassword());
        logger.info("  USER:     {} / {}", ConfigLoader.getUserEmail(), ConfigLoader.getUserPassword());
        logger.info("=".repeat(80));

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        // Keep the server running (only if blocking mode)
        if (block) {
            tomcat.getServer().await();
        }
    }

    /**
     * Stops the server.
     * Useful for testing scenarios where you need to explicitly stop the server.
     */
    public void stop() throws LifecycleException {
        if (tomcat != null) {
            tomcat.stop();
        }
    }

    /**
     * Loads sample data into the system for demonstration and testing.
     * Uses SampleDataLoader to populate comprehensive data including:
     * - Users, Stores, Aisles, Shelves, Products, Inventory
     * - Customers, Baskets, IoT Devices
     */
    private void loadSampleData(AuthenticationService userService, StoreService storeService) {
        logger.info("Loading sample data using SampleDataLoader...");

        try {
            // Create data loader instance
            SampleDataLoader dataLoader = new SampleDataLoader(storeService, userService);

            // Load all sample data (comprehensive dataset)
            dataLoader.loadAllSampleData();

            // Print summary
            dataLoader.printDataSummary();

            logger.info("Sample data loaded successfully");
        } catch (Exception e) {
            logger.warn("Error loading sample data: {}", e.getMessage());
            // Continue even if sample data fails to load
        }
    }

    /**
     * Shuts down the application gracefully.
     */
    private void shutdown() {
        logger.info("Shutting down Commission Calculator Integration Application...");

        try {
            if (tomcat != null) {
                tomcat.stop();
                tomcat.destroy();
            }

            logger.info("Application shut down successfully");
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }
    }
}
