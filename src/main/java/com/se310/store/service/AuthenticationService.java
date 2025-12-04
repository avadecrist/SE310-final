package com.se310.store.service;


import com.se310.store.model.User;
import com.se310.store.model.UserRole;
import com.se310.store.repository.UserRepository;
import com.se310.store.repository.StoreRepository;
import com.se310.store.security.PasswordEncryption;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Optional;
import java.util.List;


/**
 * This class is responsible for authenticating users and managing user data.
 * Handles password encryption/decryption for secure password storage.
 *
 * @author  Sergey L. Sundukovskiy
 * @version 1.0
 * @since 2025-09-25
 **/
public class AuthenticationService {

    //TODO: Implement authentication service for User operations
    //TODO: Implement authorizations service for Store operations
    //TODO: Implement management of User related data in the persistent storage
    //TODO: Implement Service Layer Pattern

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;

    public AuthenticationService(UserRepository userRepository, StoreRepository storeRepository) {
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
    }
    

    /**
     * Authenticates a user using HTTP Basic Authentication.
     *
     * @param authHeader The Authorization header value (e.g., "Basic base64(email:password)")
     * @return Optional containing the authenticated User, or empty if authentication fails
     */
    public Optional<User> authenticateBasic(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return Optional.empty();
        }

        try {
            //TODO: Implement User Authentication logic
            String base64Credentials = authHeader.substring("Basic ".length()).trim();
            byte[] decodedBytes = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(decodedBytes, StandardCharsets.UTF_8);

            // Split credentials into email and password
            String[] parts = credentials.split(":", 2);
            if (parts.length != 2) {
                return Optional.empty();
            }

            String email = parts[0];
            String password = parts[1];

            if (email == null || email.isBlank() || password == null || password.isBlank()) {
                return Optional.empty();
            }

            //TODO: Implement User Repository retrieval logic
            Optional<User> user = userRepository.findByEmail(email);
            if (user.isEmpty()) {
                return Optional.empty();
            }

            User retrievedUser = user.get();
            String encryptedPassword = retrievedUser.getPassword();

            // Verify password using encryption helper
            boolean valid = PasswordEncryption.verify(password, encryptedPassword);
            if (!valid) {
                return Optional.empty();
            }

            return Optional.of(retrievedUser);

        } catch (Exception e) {
            // Invalid format or decoding error
            return Optional.empty();
        }
    }

    /**
     * Register a new user with a specific role.
     * Password is encrypted before storage for security.
     *
     * @param email User's email address
     * @param password User's password (plain text, will be encrypted)
     * @param name User's display name
     * @param role User's role (ADMIN, MANAGER, or USER)
     * @return The created User object
     */
    public User registerUser(String email, String password, String name, UserRole role) {
        if (email == null || email.isBlank()
                || password == null || password.isBlank()
                || name == null || name.isBlank()
                || role == null) {
            throw new IllegalArgumentException("Email, password, name, and role must not be blank");
        }

        // Ensure no duplicate users
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("User with email " + email + " already exists");
        }

        // Ensure password is encrypted before storing
        String encryptedPassword = PasswordEncryption.isEncrypted(password) ? password: PasswordEncryption.encrypt(password);

        User user = new User(email, encryptedPassword, name, role);

        return userRepository.save(user);
    }

    /**
     * Register a new user with default USER role
     *
     * @param email User's email address
     * @param password User's password
     * @param name User's display name
     * @return The created User object
     */
    public User registerUser(String email, String password, String name) {
        return registerUser(email, password, name, UserRole.USER); // Utilize registerUser method above
    }

    /**
     * Check if user exists
     */
    public boolean userExists(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Get all users
     */
    public Collection<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Get user by email
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    /**
     * Update user information.
     * Password is encrypted before storage if provided.
     *
     * @param email User's email address
     * @param password New password (plain text, will be encrypted), or null to keep current
     * @param name New name, or null to keep current
     * @return The updated User object, or null if user not found
     */
    public User updateUser(String email, String password, String name) {
        if (email == null || email.isBlank()) {
            return null;
        }
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return null;
        }

        User user = userOpt.get();

        // Update password if provided
        if (password != null && !password.isBlank()) {
            String encryptedPassword = PasswordEncryption.isEncrypted(password) ? password : PasswordEncryption.encrypt(password);
            user.setPassword(encryptedPassword);
        }

        // Update name if provided
        if (name != null && !name.isBlank()) {
            user.setName(name);
        }

        return userRepository.save(user);
    }

    /**
     * Delete user by email
     */
    public boolean deleteUser(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return userRepository.delete(email);
    }

    // ============= STORE AUTHORIZATION ============= //

    private boolean hasRole(User user, List<UserRole> allowedRoles) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        for (UserRole role : allowedRoles) {
            if (user.getRole() == role) {
                return true;
            }
        }
        return false;
    }

    /**
     * Anyone with a valid role can view stores.
     */
    public boolean canViewStores(User user) {
        return hasRole(user, List.of(UserRole.ADMIN, UserRole.MANAGER, UserRole.USER));
    }

    /**
     * Anyone with a valid role can view a specific store,
     * but the store must actually exist.
     */
    public boolean canViewStore(User user, String storeId) {
        if (!hasRole(user, List.of(UserRole.ADMIN, UserRole.MANAGER, UserRole.USER))) {
            return false;
        }
        return storeRepository.existsById(storeId);
    }


    /**
     * Only ADMIN and MANAGER can create stores.
     */
    public boolean canCreateStore(User user) {
        return hasRole(user, List.of(UserRole.ADMIN, UserRole.MANAGER));
    }

    /**
     * Only ADMIN and MANAGER can update stores.
     * Store must exist.
     */
    public boolean canUpdateStore(User user, String storeId) {
        if (!hasRole(user, List.of(UserRole.ADMIN, UserRole.MANAGER))) {
            return false;
        }
        return storeRepository.existsById(storeId);
    }

    /**
     * Only ADMIN can delete stores.
     * Store must exist.
     */
    public boolean canDeleteStore(User user, String storeId) {
        if (!hasRole(user, List.of(UserRole.ADMIN))) {
            return false;
        }
        return storeRepository.existsById(storeId);
    }
}
