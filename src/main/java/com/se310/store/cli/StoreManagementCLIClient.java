package com.se310.store.cli;

import com.se310.store.config.ConfigLoader;
import com.se310.store.data.DataManager;
import com.se310.store.model.*;
import com.se310.store.repository.UserRepository;
import com.se310.store.service.AuthenticationService;
import com.se310.store.service.StoreService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.Scanner;

/**
 * StoreManagementCLIClient - Command-line interface for Store Management System
 *
 * This CLI client provides a hybrid interface:
 * - Authentication uses AuthenticationService (direct service access)
 * - Store and User operations use REST API (via HTTP)
 * - All other operations (Products, Customers, etc.) use StoreService directly
 *
 * This demonstrates both API-based and direct service access patterns.
 *
 * @author  Sergey L. Sundukovskiy
 * @version 1.0
 * @since   2025-11-11
 */
public class StoreManagementCLIClient {

    //TODO: Implement View for Store operations, part of the MVC Pattern

    private static final String BASE_URL = ConfigLoader.getApiBaseUrl();
    private static String authHeader = null;
    private static final Scanner scanner = new Scanner(System.in);

    // Direct service access
    private static StoreService storeService;
    private static AuthenticationService authenticationService;

    public static void main(String[] args) {
        System.out.println("===============================================");
        System.out.println("|  Smart Store Management CLI Client        |");
        System.out.println("|  (Hybrid: API + Direct Service Access)    |");
        System.out.println("===============================================");
        System.out.println();

        // Initialize local services
        initializeServices();

        // Authenticate user using AuthenticationService
        if (!authenticate()) {
            System.out.println("Authentication failed. Exiting...");
            return;
        }

        // Main menu loop
        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    manageStores();
                    break;
                case "2":
                    manageUsers();
                    break;
                case "3":
                    manageProducts();
                    break;
                case "4":
                    manageCustomers();
                    break;
                case "5":
                    viewDocumentation();
                    break;
                case "6":
                    logout();
                    break;
                case "0":
                    running = false;
                    System.out.println("Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }

        scanner.close();
    }

    /**
     * Initialize local services for direct access
     */
    private static void initializeServices() {
        try {
            // Initialize DataManager and repositories
            DataManager dataManager = DataManager.getInstance();
            UserRepository userRepository = new UserRepository(dataManager);

            // Initialize services
            storeService = new StoreService();
            authenticationService = new AuthenticationService(userRepository);

            System.out.println("[OK] Local services initialized");
            System.out.println();
        } catch (Exception e) {
            System.err.println("Error initializing services: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Authenticate user using AuthenticationService
     */
    private static boolean authenticate() {
        System.out.println("Please login");
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        // Create Basic Auth header for both service and API usage
        String credentials = email + ":" + password;
        authHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        // Authenticate using AuthenticationService
        try {
            Optional<User> userOpt = authenticationService.authenticateBasic(authHeader);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                System.out.println("[OK] Authentication successful!");
                System.out.println("  Welcome, " + user.getName() + " (" + user.getRole() + ")");
                System.out.println();
                return true;
            } else {
                System.out.println("[X] Authentication failed: Invalid credentials");
                return false;
            }
        } catch (Exception e) {
            System.out.println("[X] Authentication failed: " + e.getMessage());
            return false;
        }
    }

    private static void printMainMenu() {
        System.out.println("\n===============================================");
        System.out.println("|            MAIN MENU                      |");
        System.out.println("===============================================");
        System.out.println("|  1. Manage Stores (via API)               |");
        System.out.println("|  2. Manage Users (via API)                |");
        System.out.println("|  3. Manage Products (via Service)         |");
        System.out.println("|  4. Manage Customers (via Service)        |");
        System.out.println("|  5. View API Documentation                |");
        System.out.println("|  6. Logout (Switch User)                  |");
        System.out.println("|  0. Exit                                  |");
        System.out.println("===============================================");
        System.out.print("Enter your choice: ");
    }

    // ==================== STORE OPERATIONS ====================

    /**
     * Store operations - Uses REST API
     */
    private static void manageStores() {
        System.out.println("\n=== Store Management (via REST API) ===");
        System.out.println("1. List all stores");
        System.out.println("2. View store details");
        System.out.println("3. Create new store");
        System.out.println("4. Update store");
        System.out.println("5. Delete store");
        System.out.println("0. Back to main menu");
        System.out.print("Enter your choice: ");

        String choice = scanner.nextLine().trim();

        try {
            switch (choice) {
                case "1":
                    listStores();
                    break;
                case "2":
                    viewStoreDetails();
                    break;
                case "3":
                    createStore();
                    break;
                case "4":
                    updateStore();
                    break;
                case "5":
                    deleteStore();
                    break;
                case "0":
                    printMainMenu();
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
            
        } catch (Exception e) {
            System.out.println("[X] Error during store operation: " + e.getMessage());
        }
    }

    private static void listStores() throws Exception {
        System.out.println("\n--- List of Stores ---");

        String json = sendRequest("GET", "/stores", null);

        System.out.println(json);
    }

    private static void viewStoreDetails() throws Exception {
        System.out.print("Enter Store ID: ");
        String storeId = scanner.nextLine().trim();

        if (storeId.isEmpty()) {
            System.out.println("[X] Store ID cannot be empty");
            return;
        }

        String json = sendRequest("GET", "/stores/" + storeId, null);

        System.out.println("\n--- Store Details ---");
        System.out.println(json);
    }

    private static void createStore() throws Exception {
        System.out.print("Enter Store ID: ");
        String storeId = scanner.nextLine().trim();

        System.out.print("Enter Store Name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Enter Store Address: ");
        String address = scanner.nextLine().trim();

        if (storeId.isEmpty() || name.isEmpty() || address.isEmpty()) {
            System.out.println("[X] Store ID, Name, and Address cannot be empty");
            return;
        }


        // Build endpoint: /stores?storeId=xxx&name=xxx&address=xxx
        String endpoint = "/stores"
                + "?storeId=" + encode(storeId)
                + "&name=" + encode(name)
                + "&address=" + encode(address);

        // Controller reads parameters via getParameter(), so body must be null
        String json = sendRequest("POST", endpoint, null);

        System.out.println("\n--- Created Store ---");
        System.out.println(json);
    }

    private static void updateStore() throws Exception {
        System.out.println("\n=== Update Store ===");

        System.out.print("Enter Store ID to update: ");
        String storeId = scanner.nextLine().trim();

        if (storeId.isEmpty()) {
            System.out.println("[X] Store ID cannot be empty");
            return;
        }

        System.out.print("Enter new description (or leave blank to keep current description): ");
        String description = scanner.nextLine().trim();

        System.out.print("Enter new address (leave blank to keep current address): ");
        String address = scanner.nextLine().trim();

        if (description.isEmpty() && address.isEmpty()) {
            System.out.println("[X] Nothing to update. Both fields are blank.");
            return;
        }

        // Start with path: /stores/{storeId}
        String path = "/stores/"
                + encode(storeId);
        // For any blank address or description parameter
        boolean hasQueryParam = false;
        String endpoint = path;

        if (!description.isEmpty()) {
            endpoint = endpoint + "?description=" + encode(description);
            hasQueryParam = true;
        }

        if (!address.isEmpty()) {
            endpoint = endpoint + (hasQueryParam ? "&" : "?") + "address=" + encode(address);
        }

        // PUT with query params, body should be null
        String json = sendRequest("PUT", endpoint, null);

        System.out.println("\n--- Updated Store ---");
        System.out.println(json);
    }

    private static void deleteStore() throws Exception {
        System.out.print("Enter Store ID to delete: ");
        String storeId = scanner.nextLine().trim();

        if (storeId.isEmpty()) {
            System.out.println("[X] Store ID cannot be empty");
            return;
        }

        String json = sendRequest("DELETE", "/stores/" + storeId, null);

        System.out.println("\n--- Deleted Store ---");
        System.out.println(json);
    }


    // ==================== USER OPERATIONS ====================

    /**
     * User operations - Uses REST API
     */
    private static void manageUsers() {
        System.out.println("\n=== User Management (via REST API) ===");
        System.out.println("1. List all users");
        System.out.println("2. View user details");
        System.out.println("3. Register new user");
        System.out.println("0. Back to main menu");
        System.out.print("Enter your choice: ");

        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1":
                listUsers();
                break;
            case "2":
                viewUserDetails();
                break;
            case "3":
                registerUser();
                break;
            case "0":
                printMainMenu();
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
        }
    }

    private static void listUsers() {
        System.out.println("\n--- List of Users ---");

        String json = sendRequest("GET", "/users", null);

        System.out.println(json);
    }

    private static void viewUserDetails() {
        System.out.print("Enter User Email: ");
        String email = scanner.nextLine().trim();

        if (email.isEmpty()) {
            System.out.println("[X] User email cannot be empty");
            return;
        }

        String json = sendRequest("GET", "/users/" + email, null);

        System.out.println("\n--- User Details ---");
        System.out.println(json);
    }

//this uses a non-null body??
    private static void registerUser() {
        System.out.print("Enter User Email: ");
        String email = scanner.nextLine().trim();

        System.out.print("Enter User Password: ");
        String password = scanner.nextLine().trim();

        System.out.print("Enter User Name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Enter User Role (ADMIN/USER): ");
        String role = scanner.nextLine().trim();

        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            System.out.println("[X] Email, Password, and Name cannot be empty");
            return;
        }

        String body = String.format("?email=xxx&password=xxx&name=xxx&role=xxx",
                urlEncode(email), urlEncode(password), urlEncode(name), urlEncode(role));

        String json = sendRequest("POST", "/users" + body, null);

        System.out.println("\n--- Registered User ---");
        System.out.println(json);
    }

    // Helper method to ensure endpoint parameters are in URL format
    private static String encode(String parameter) {
        if (parameter == null) {
            return "";
        }
        parameter = parameter.replace(" ", "+")
                             .replace("&", "%26")
                             .replace("#", "%23");
        return parameter;
    }


    // ==================== PRODUCT OPERATIONS ====================
    /**
     * Product operations - Uses StoreService directly
     */
    private static void manageProducts() {
        System.out.println("\n=== Product Management (via StoreService) ===");
        System.out.println("1. View product");
        System.out.println("2. Create new product");
        System.out.println("0. Back to main menu");
        System.out.print("Enter your choice: ");

        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1":
                viewProduct();
                break;
            case "2":
                createProduct();
                break;
            case "0":
                printMainMenu();
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
        }
    }
//TEMPORARY METHOD
    private static void viewProduct() {
        System.out.print("Enter Product ID: ");
        String productId = scanner.nextLine().trim();

        if (productId.isEmpty()) {
            System.out.println("[X] Product ID cannot be empty");
            return;
        }

        Optional<Product> productOpt = storeService.getProductById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            System.out.println("\n--- Product Details ---");
            System.out.println(product);
        } else {
            System.out.println("[X] Product not found with ID: " + productId);
        }
    }
//TEMPORARY METHOD
    private static void createProduct() {
        System.out.print("Enter Product ID: ");
        String productId = scanner.nextLine().trim();
        System.out.print("Enter Product Name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Enter Product Description: ");
        String description = scanner.nextLine().trim();
        System.out.print("Enter Product Size: ");
        String size = scanner.nextLine().trim();
        System.out.print("Enter Product Category: ");
        String category = scanner.nextLine().trim();
        System.out.print("Enter Product Price: ");
        String priceStr = scanner.nextLine().trim();
        System.out.print("Enter Product Temperature (COLD/ROOM/HEATED): ");
        String tempStr = scanner.nextLine().trim();
        if (productId.isEmpty() || name.isEmpty() || priceStr.isEmpty() || tempStr.isEmpty()) {
            System.out.println("[X] Product ID, Name, Price, and Temperature cannot be empty");
            return;
        }
        Double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            System.out.println("[X] Invalid price format");
            return;
        }
        Temperature temperature;
        try {
            temperature = Temperature.valueOf(tempStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("[X] Invalid temperature value");
            return;
        }
        Product product = new Product(productId, name, description, size, category, price, temperature);
        storeService.addProduct(product);
        System.out.println("\n--- Created Product ---");
        System.out.println(product);
    }


    // ==================== CUSTOMER OPERATIONS ====================
    /**
     * Customer operations - Uses StoreService directly
     */
    private static void manageCustomers() {
        System.out.println("\n=== Customer Management (via StoreService) ===");
        System.out.println("1. View customer");
        System.out.println("2. Register new customer");
        System.out.println("0. Back to main menu");
        System.out.print("Enter your choice: ");

        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1":
                viewCustomer();
                break;
            case "2":
                registerCustomer();
                break;
            case "0":
                printMainMenu();
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
        }
    }

//TEMPORARY METHOD
    private static void viewCustomer() {
        System.out.print("Enter Customer ID: ");
        String customerId = scanner.nextLine().trim();

        if (customerId.isEmpty()) {
            System.out.println("[X] Customer ID cannot be empty");
            return;
        }

        Optional<Customer> customerOpt = storeService.getCustomerById(customerId);
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            System.out.println("\n--- Customer Details ---");
            System.out.println(customer);
        } else {
            System.out.println("[X] Customer not found with ID: " + customerId);
        }
    }
//TEMPORARY METHOD
    private static void registerCustomer() {
        System.out.print("Enter Customer ID: ");
        String customerId = scanner.nextLine().trim();
        System.out.print("Enter Customer Name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Enter Customer Email: ");
        String email = scanner.nextLine().trim();

        if (customerId.isEmpty() || name.isEmpty() || email.isEmpty()) {
            System.out.println("[X] Customer ID, Name, and Email cannot be empty");
            return;
        }

        Customer customer = new Customer(customerId, name, email);
        storeService.addCustomer(customer);
        System.out.println("\n--- Registered Customer ---");
        System.out.println(customer);
    }

    private static void viewDocumentation() {
        System.out.println("\n=== API Documentation ===");
        int port = ConfigLoader.getServerPort();
        System.out.println("API Documentation URL: http://localhost:" + port + "/api/docs");
        System.out.println("OpenAPI Spec: http://localhost:" + port + "/api/docs/openapi.yaml");
        System.out.println("\nArchitecture:");
        System.out.println("  - Store/User operations: REST API (HTTP)");
        System.out.println("  - Other operations: Direct StoreService access");
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }

    /**
     * Logout and switch to a different user
     */
    private static void logout() {
        System.out.println("\n=== Logout ===");
        System.out.println("Logging out current user...");

        // Clear authentication
        authHeader = null;

        System.out.println("[OK] Logged out successfully\n");

        // Re-authenticate with new credentials
        while (!authenticate()) {
            System.out.println("\nAuthentication failed. Please try again.\n");
        }
    }

    /**
     * Send HTTP request to REST API (for Store and User operations)
     */
    private static String sendRequest(String method, String endpoint, String body) throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Authorization", authHeader);
        conn.setRequestProperty("Content-Type", "application/json");

        if (body != null) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = body.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }

        StringBuilder response = getStringBuilder(conn);

        return response.toString();
    }

    private static StringBuilder getStringBuilder(HttpURLConnection conn) throws Exception {
        int responseCode = conn.getResponseCode();

        BufferedReader reader;
        if (responseCode >= 200 && responseCode < 300) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
        }

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        if (responseCode < 200 || responseCode >= 300) {
            throw new Exception("HTTP " + responseCode + ": " + response);
        }
        return response;
    }
}