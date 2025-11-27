package com.se310.store.dto;

import com.se310.store.model.Store;

/**
 * StoreMapper implements the DTO Pattern for Store entities.
 * Provides transformation between Store domain objects and DTOs to separate
 * internal representation from API responses (excludes transient collections for cleaner JSON).
 *
 * @author  Sergey L. Sundukovskiy
 * @version 1.0
 * @since   2025-11-11
 */
public class StoreMapper {

    //TODO: Implement Data Transfer Object for Store entity
    //TODO: Implement Factory methods for Store DTOs

    /**
     * StoreDTO - Data Transfer Object for Store
     */
    public static class StoreDTO implements JsonSerializable {
        private String id;
        private String address;
        private String description;

        public StoreDTO() {
        }

        public StoreDTO(String id, String address, String description) {
            this.id = id;
            this.address = address;
            this.description = description;
        }

        @Override
        public String toJson() {
            StoreDTO copy = new StoreDTO(id, address,
            description != null ? description.toLowerCase() : null);
            return JsonHelper.toJson(copy);
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
