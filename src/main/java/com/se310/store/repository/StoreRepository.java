package com.se310.store.repository;

import com.se310.store.data.DataManager;
import com.se310.store.model.Store;

import java.util.*;

/**
 * Store Repository implements Repository Pattern for store data access layer
 * Uses DataManager for persistent storage
 *
 * This repository is completely database-agnostic - it has no knowledge of SQL,
 * ResultSets, or SQLExceptions. All database-specific logic is encapsulated in DataManager.
 *
 * @author  Sergey L. Sundukovskiy
 * @version 1.0
 * @since   2025-11-06
 */
public class StoreRepository  {

    //TODO: Implement Store persistence layer using Repository Pattern

    private final DataManager dataManager;

    public StoreRepository(DataManager dataManager) {
        if (dataManager == null) {
            throw new NullPointerException("DataManager must not be null");
        }
        this.dataManager = dataManager;
    }

    public Optional<Store> findById(String storeId) {
        if (storeId == null || storeId.isBlank()) {
            return Optional.empty();
        }
        return dataManager.getStoreById(storeId);
    }

    public boolean existsById(String storeId) {
        if (storeId == null || storeId.isBlank()) {
            return false;
        }
        return dataManager.doesStoreExist(storeId);
    }

    public Store save(Store store) {
        if (store == null) {
            throw new NullPointerException("Store must not be null");
        }

        if (store.getId() == null || store.getId().isBlank()) {
            throw new IllegalArgumentException("Store id must not be null or blank");
        }
        return dataManager.persistStore(store);
    }

    public boolean delete(String storeId) {
        if (storeId == null || storeId.isBlank()) {
            return false;
        }
        return dataManager.removeStore(storeId);
    }

    public List<Store> findAll() {
        return dataManager.getAllStores();
    }

}