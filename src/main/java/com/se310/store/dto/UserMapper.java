package com.se310.store.dto;

import com.se310.store.model.User;
import com.se310.store.model.UserRole;

/**
 * UserMapper implements the DTO Pattern for User entities.
 * Provides transformation between User domain objects and DTOs to separate
 * internal representation from API responses (e.g., hiding sensitive data like passwords).
 *
 * @author  Sergey L. Sundukovskiy
 * @version 1.0
 * @since   2025-11-11
 */
public class UserMapper {

    //TODO: Implement Data Transfer Object for User entity
    //TODO: Implement Factory methods for User DTOs

    // Private constructor to prevent instantiation
    private UserMapper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * UserDTO - Data Transfer Object for User
     */
    public static class UserDTO implements JsonSerializable {
        private String email;
        private String name;
        private String role;

        public UserDTO() {
        }

        public UserDTO(String email, String name, String role) {
            this.email = email;
            this.name = name;
            this.role = role;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    
    /**
     * Factory method that creates a User domain object from a UserDTO.
     * Converts UserDTO to User for internal use.
     * 
     * - Password should be handled elsewhere in the registration/auth flows.
     *
     * @param dto UserDTO 
     * @return a populated User domain object
     */
    public static User toUser(UserDTO dto) {
        // UserDTO must not be null
        if (dto == null) {
            return null;
        }

        // Create User and map all fields except password
        User user = new User(); // only initializes role (USER by default)
        user.setEmail(dto.getEmail());
        user.setName(dto.getName());

        // Map role from String to UserRole enum field in User
        String roleString = dto.getRole();
        if (roleString != null && !roleString.isEmpty()) {
            try {
                UserRole role = UserRole.valueOf(roleString);
                user.setRole(role);
            } catch (IllegalArgumentException e) {
                // Invalid role string; keep default role (USER)
                System.err.println("Invalid role '" + roleString + "'... using default USER role.");
            }
        }

        return user;
    }

    
    /**
     * Factory method that creates a UserDTO from a User domain object.
     * Converts User to UserDTO for API responses.
     * 
     * @param user User domain object
     * @return a populated UserDTO, or null if user is null
     */
    public static UserDTO fromUser(User user) {
        // User domain object must not be null
        if (user == null) {
            return null;
        }

        String roleString = null;
        UserRole role = user.getRole();
        if (role != null) {
            // Export role as String ("ADMIN", "MANAGER", "USER")
            roleString = role.name();
        }

        return new UserDTO(
                user.getEmail(),
                user.getName(),
                roleString);
    }
    
}
