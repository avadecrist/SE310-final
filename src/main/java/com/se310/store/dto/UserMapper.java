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

        @Override
        public String toJson() {
            UserDTO copy = new UserDTO(email, name,
            role != null ? role.toLowerCase() : null);

            return JsonHelper.toJson(copy);
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
}
