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

    // Private constructor to prevent instantiation
    private StoreMapper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

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

    /**
     * Factory method that creates a Store domain object from a StoreDTO.
     * Converts StoreDTO to Store for internal use.
     * 
     *
     * @param dto StoreDTO 
     * @return a populated Store domain object, or null if dto is null
     */
    public static Store toStore(StoreDTO dto) {
        // StoreDTO must not be null
        if (dto == null) {
            return null;
        }

        // Create Store from DTO fields (collection maps are initialized empty inside Store)
        Store store = new Store(dto.getId(), dto.getAddress(), dto.getDescription()); 

        return store;
    }

    
    /**
     * Factory method that creates a StoreDTO from a Store domain object.
     * Converts Store to StoreDTO for API responses.
     * 
     * @param store Store domain object
     * @return a populated StoreDTO, or null if store is null
     */
    public static StoreDTO fromStore(Store store) {
        // Store domain object must not be null
        if (store == null) {
            return null;
        }
        return new StoreDTO(
                store.getId(),
                store.getAddress(),
                store.getDescription());
    }

}
