package com.se310.store.service;

import com.se310.store.data.DataManager;
import com.se310.store.model.*;
import com.se310.store.model.AisleLocation;
import com.se310.store.repository.*;

import java.sql.Timestamp;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

/**
 * This is the main service of the system implementing Command API for processing CLI commands and
 * Service API for processing Store events
 *
 * @author  Sergey L. Sundukovskiy
 * @version 1.0
 * @since 2025-09-25
 **/
public class StoreService {

    //TODO: Implement management of Store related information in the persistent storage
    //TODO: Implement Service Layer Pattern

    private static final Map<String, Store> storeMap;
    private static final Map<String, Customer> customerMap;
    private static final Map<String, Product> productMap;
    private static final Map<String, Inventory> inventoryMap;
    private static final Map<String, Basket> basketMap;
    private static final Map<String, Device> deviceMap;

    // Initialize maps
    static {
        storeMap = new HashMap<>();
        customerMap = new HashMap<>();
        productMap = new HashMap<>();
        inventoryMap = new HashMap<>();
        basketMap = new HashMap<>();
        deviceMap = new HashMap<>();
    }

    private final StoreRepository storeRepository;
    private final DataManager dataManager;

    public StoreService() {
        this.storeRepository = null;
        this.dataManager = DataManager.getInstance();
    }

    public StoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
        this.dataManager = DataManager.getInstance();
        this.loadAllDataFromDatabase();
    }

    /**
     * Load all data from database into memory maps
     */
    private void loadAllDataFromDatabase() {
        //TODO: Load store data into the maps
        clearAllMaps();

        if (storeRepository != null) {
            for (Store store : storeRepository.findAll()) {
                if (store != null && store.getId() != null && !store.getId().isBlank()) {
                    storeMap.put(store.getId(), store);
                }
            }
        } else {
            // load directly from DataManager if repository is not wired
            for (Store store : dataManager.getAllStores()) {
                if (store != null && store.getId() != null && !store.getId().isBlank()) {
                    storeMap.put(store.getId(), store);
                }
            }
        }

        // ---- PRODUCTS ----
        // Load all products from the database and populate productMap
        try (ResultSet rs = dataManager.findAllProducts()) {

            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                String description = rs.getString("description");
                String size = rs.getString("size");
                String category = rs.getString("category");
                double price = rs.getDouble("price");
                Temperature temperature = Temperature.valueOf(rs.getString("temperature"));

                Product product = new Product(id, name, description, size, category, price, temperature);

                if (id != null && !id.isBlank()) {
                    productMap.put(id, product);
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load products from database", e);
        }

        // ---- CUSTOMERS ----
        try (ResultSet rs = dataManager.findAllCustomers()) {

        while (rs.next()) {
            String id = rs.getString("id");
            String firstName = rs.getString("first_name");
            String lastName = rs.getString("last_name");
            String customerType = rs.getString("customer_type");
            String email = rs.getString("email");
            String accountAddress = rs.getString("account_address");
            String storeId = rs.getString("store_id");
            String aisleNumber = rs.getString("aisle_number");
            Timestamp lastSeenTs = rs.getTimestamp("last_seen");

            // Build domain Customer
            CustomerType type = CustomerType.valueOf(customerType);
            Customer customer = new Customer(id, firstName, lastName, type, email, accountAddress);

            // Restore lastSeen if present
            if (lastSeenTs != null) {
                customer.setLastSeen(new java.util.Date(lastSeenTs.getTime()));
            }

            // Restore store location + attach to Store if we have a storeId
            if (storeId != null && !storeId.isBlank()) {
                Store store = storeMap.get(storeId);
                if (store != null) {
                    StoreLocation location = new StoreLocation(storeId, aisleNumber);
                    customer.setStoreLocation(location);
                    try {
                        store.addCustomer(customer);   
                    } catch (StoreException e) {
                        System.err.println("Warning: failed to attach customer " + id +
                                       " to store " + storeId + ": " + e.getMessage());
                    }
                }
            }

            // Add to customer map
            if (id != null && !id.isBlank()) {
                customerMap.put(id, customer);
            }
        }

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load customers from database", e);
        }
        
    }

    /**
     * Clear all static maps - useful for testing
     */
    public static void clearAllMaps() {
        storeMap.clear();
        customerMap.clear();
        productMap.clear();
        inventoryMap.clear();
        basketMap.clear();
        deviceMap.clear();
    }


    public Store provisionStore(String storeId, String name, String address, String token) throws StoreException {

        Store store = new Store(storeId, address, name);

        //If Store already exists throw and exception
        if (storeMap.putIfAbsent(storeId, store) != null) {
            throw new StoreException("Provision Store", "Store Already Exists");
        }

        //TODO: Persist Store to database
        try {
            if (storeRepository != null) {
                storeRepository.save(store);
            } else {
                dataManager.persistStore(store);
            }
        } catch (Exception e) {
            throw new StoreException("Provision Store",
                    "Failed to save Store to database: " + e.getMessage());
        }
        return store;
    }

    public Store showStore(String storeId, String token) throws StoreException {

        //If Store does not exist throw and exception
        Store store = storeMap.get(storeId);
        if(store == null)
            throw new StoreException("Show Store", "Store Does Not Exist");

        return store;
    }

    public Aisle provisionAisle(String storeId, String aisleNumber, String name, String description,
                                AisleLocation location, String token) throws StoreException {

        Store store = storeMap.get(storeId);
        Aisle aisle;

        //Check to see if Store already exists;
        if(store == null){
            throw new StoreException("Provision Aisle", "Store Does Not Exist");
        } else {
            aisle = store.addAisle(aisleNumber, name, description, location);
        }

        return aisle;
    }

    public Aisle showAisle(String storeId, String aisleNumber, String token) throws StoreException {
        Store store = storeMap.get(storeId);
        Aisle aisle;
        //Check to see if Store exists
        if(store == null){
            throw new StoreException("Show Aisle", "Store Does Not Exist");
        } else {
            //Check to see if Aisle already exists
            aisle = store.getAisle(aisleNumber);
            if (aisle == null) {
                throw new StoreException("Show Aisle", "Aisle Does Not Exist");
            }
        }
        return aisle;
    }

    public Shelf provisionShelf(String storeId, String aisleNumber, String shelfId, String name,
                                ShelfLevel level, String description, Temperature temperature, String token) throws StoreException {

        Store store = storeMap.get(storeId);
        Shelf shelf;

        //Check to see if Store exists
        if(store == null){
            throw new StoreException("Provision Shelf", "Store Does Not Exist");
        } else {
            Aisle aisle = store.getAisle(aisleNumber);
            //Check to see if Aisle exists
            if (aisle == null){
                throw new StoreException("Provision Shelf", "Aisle Does Not Exist");
            } else {
                shelf = aisle.getShelf(shelfId);
                //Check to see if Shelf exists
                if(shelf != null){
                    throw new StoreException("Provision Shelf", "Shelf Already Exists");
                }

                //Add Shelf to the Aisle
                shelf = aisle.addShelf(shelfId, name, level, description, temperature);
            }
        }
        return shelf;
    }

    public Shelf showShelf(String storeId, String aisleNumber, String shelfId, String token) throws StoreException {
        Store store = storeMap.get(storeId);
        Shelf shelf;

        //Check to see if Store exists
        if(store == null){
            throw new StoreException("Show Shelf", "Store Does Not Exist");
        } else {
            //Check to see if Aisle exists
            Aisle aisle = store.getAisle(aisleNumber);
            if (aisle == null){
                throw new StoreException("Show Shelf", "Aisle Does Not Exist");
            } else {
                //Check to see if Shelf exists
                shelf = aisle.getShelf(shelfId);
                if(shelf == null){
                    throw new StoreException("Show Shelf", "Shelf Does Not Exist");
                }
            }
        }
        return shelf;
    }

    public Inventory provisionInventory(String inventoryId, String storeId, String aisleNumber, String shelfId,
                                        int capacity, int count, String productId, InventoryType type, String token) throws StoreException {

        Store store = storeMap.get(storeId);
        Product product = productMap.get(productId);
        Inventory inventory;

        //Check to see if Store exists
        if(store == null){
            throw new StoreException("Provision Inventory", "Store Does Not Exist");
        } else {
            //Check to see if Aisle exists
            Aisle aisle = store.getAisle(aisleNumber);
            if (aisle == null){
                throw new StoreException("Provision Inventory", "Aisle Does Not Exist");
            } else {
                //Check to see if Shelf exists
                Shelf shelf = aisle.getShelf(shelfId);
                if(shelf == null){
                    throw new StoreException("Provision Inventory", "Shelf Does Not Exist");
                } else if(product == null){
                    //Check to see if Product exists
                    throw new StoreException("Provision Inventory", "Product Does Not Exist");
                } else if(!shelf.getTemperature().equals(product.getTemperature())){
                    //Make sure that Product Temperature and Shelf Temperature are consistent
                    throw new StoreException("Provision Inventory", "Product and Shelf Temperature " +
                            "Is Not Consistent");
                }

                //Add Inventory to the Shelf
                inventory = shelf.addInventory(inventoryId, storeId, aisleNumber, shelfId,
                        capacity, count, productId, type);

                //Add Inventory to the global Inventory Map
                inventoryMap.put(inventoryId, inventory);

                //Add Inventory to the Store
                store.addInventory(inventory);

                // Persist to database
                try {
                    dataManager.saveInventory(inventoryId, storeId, aisleNumber, shelfId,
                                             capacity, count, productId, type.name());
                } catch (Exception e) {
                    throw new StoreException("Provision Inventory", "Failed to save inventory to database: " + e.getMessage());
                }
            }
        }

        return inventory;
    }

    public Inventory showInventory(String inventoryId, String token) throws StoreException {

        Inventory inventory = inventoryMap.get(inventoryId);
        //Check to see if Inventory exists
        if (inventory == null)
            throw new StoreException("Show Inventory", "Inventory Does Not Exist");
        return inventory;
    }

    public Inventory updateInventory(String inventoryId, int count, String token) throws StoreException {
        Inventory inventory = inventoryMap.get(inventoryId);
        //Check to see if Inventory exists
        if (inventory == null)
            throw new StoreException("Update Inventory", "Inventory Does Not Exist");

        //Update Inventory count
        inventory.updateInventory(count);

        //TODO: Persist inventory update to database
        try {
            InventoryLocation location = inventory.getInventoryLocation();
            String storeId = (location != null) ? location.getStoreId() : null;
            String aisleNumber = (location != null) ? location.getAisleId() : null;
            String shelfId = (location != null) ? location.getShelfId() : null;
            dataManager.saveInventory(
            inventory.getId(),
            storeId,
            aisleNumber,
            shelfId,
            inventory.getCapacity(),
            inventory.getCount(), // updated count
            inventory.getProductId(),
            inventory.getType().name());
        } catch (Exception e) {
            throw new StoreException("Update Inventory",
                    "Failed to update Inventory in database: " + e.getMessage());
        }
        return inventory;
    }

    public Product provisionProduct(String productId, String name, String description, String size, String category,
                                    double price, Temperature temperature, String token) throws StoreException {
        Product product = new Product(productId, name, description, size, category, price, temperature);

        //Check to see if Product already exists
        if (productMap.putIfAbsent(productId, product) != null)
            throw new StoreException("Provision Product", "Product Already Exists");

        //TODO: Persist to database
        try {
            dataManager.saveProduct(
                    product.getId(),                 
                    product.getName(),               
                    product.getDescription(),        
                    product.getSize(),               
                    product.getCategory(),           
                    product.getPrice(),              
                    product.getTemperature().name());
        } catch (SQLException e) {
            throw new StoreException("Provision Product",
                    "Failed to save Product to database: " + e.getMessage());
        }

        return product;
    }

    public Product showProduct(String productId, String token) throws StoreException {
        Product product = productMap.get(productId);
        //Check to see if Product exists
        if (product == null)
            throw new StoreException("Show Product", "Product Does Not Exist");
        return product;
    }


    public Customer provisionCustomer(String customerId, String firstName, String lastName,
                                      CustomerType type, String email, String address, String token)
            throws StoreException {

        Customer customer = new Customer(customerId, firstName, lastName, type, email, address);
        //Check to see if the Customer already exists
        if(customerMap.putIfAbsent(customerId, customer) != null)
            throw new StoreException("Provision Customer", "Customer Already Exists");

        //TODO: Persist to database
        // Persist to database
    try {
        dataManager.saveCustomer(
                customer.getId(),                        
                customer.getFirstName(),                 
                customer.getLastName(),                  
                customer.getType().name(),               
                customer.getEmail(),                     
                customer.getAccountAddress(),                   
                null,                                    
                null,                                    
                null);
    } catch (SQLException e) {
        throw new StoreException("Provision Customer",
                "Failed to save Customer to database: " + e.getMessage());
    }

        return customer;
    }

    public Customer updateCustomer(String customerId, String storeId, String aisleNumber, String token)
            throws StoreException {
        Store store = storeMap.get(storeId);
        Customer customer;

        //Check to see if the Store exists
        if(store == null){
            throw new StoreException("Update Customer", "Store Does Not Exist");
        } else {
            //Check to see if Aisle exists
            Aisle aisle = store.getAisle(aisleNumber);
            if (aisle == null){
                throw new StoreException("Update Customer", "Aisle Does Not Exist");
            } else {
                //Check to see if Customer exists
                customer = customerMap.get(customerId);
                if(customer == null){
                    throw new StoreException("Update Customer", "Customer Does Not Exist");
                }
            }
        }

        //Check to see if Customer changing Stores
        if(customer.getStoreLocation() != null && !customer.getStoreLocation().getStoreId().equals(storeId)){
            //Check to see if Customer already exists in other Stores
            Map<Store, Customer> customerStores  = storeMap.entrySet()
                    .stream()
                    .filter(tempStore -> (tempStore.getValue().getCustomer(customerId) != null && tempStore.getValue().getCustomer(customerId).getId().equals(customerId)))
                    .collect(Collectors.toMap(Map.Entry::getValue, tempStore -> tempStore.getValue().
                            getCustomer(customerId)));

            //If Customer exist in other stores remove him/her
            customerStores.forEach((key, value) -> key.removeCustomer(customer));

            //Before Customer can change the Store he/she must clear the Basket
            if(customer.getBasket() != null)
                customer.getBasket().clearBasket();

            //If the Customer moves to a different Store clear out the basket and the time seen
            customer.assignBasket(null);
            customer.setLastSeen(null);

            //Add Customer to another store
            store.addCustomer(customer);
        } else {

            customer.setStoreLocation(new StoreLocation(storeId, aisleNumber));
            customer.setLastSeen(new Date(System.currentTimeMillis()));
        }

        //TODO: Persist customer location update to database
        try {
            StoreLocation location = customer.getStoreLocation();
            String currentStoreId = (location != null) ? location.getStoreId() : null;
            String currentAisleNumber = (location != null) ? location.getAisleId() : null;

            java.sql.Timestamp lastSeenTs = null;
            if (customer.getLastSeen() != null) {
                lastSeenTs = new java.sql.Timestamp(customer.getLastSeen().getTime());
            }

            dataManager.saveCustomer(
                    customer.getId(),
                    customer.getFirstName(),
                    customer.getLastName(),
                    customer.getType().name(),
                    customer.getEmail(),
                    customer.getAccountAddress(),
                    currentStoreId,
                    currentAisleNumber,
                    lastSeenTs);
        } catch (SQLException e) {
            throw new StoreException("Update Customer",
                    "Failed to update Customer in database: " + e.getMessage());
        }

        return customer;
    }

    public Customer showCustomer(String customerId, String token) throws StoreException {

        //Check to see if the Customer exists
        Customer customer = customerMap.get(customerId);
        if(customer == null)
            throw new StoreException("Show Customer", "Customer Does Not Exist");

        return customer;
    }

    public Basket provisionBasket(String basketId, String token) throws StoreException {

        Basket basket = new Basket(basketId);
        //Check if Basket already exists
        if(basketMap.putIfAbsent(basketId, basket) != null)
            throw new StoreException("Provision Basket", "Basket Already Exists");

        //TODO: Persist Basket to database
        try {
            dataManager.saveBasket(
                    basket.getId(),  
                    null,             // customer id
                    null              // store id
            );
        } catch (SQLException e) {
            throw new StoreException("Provision Basket",
                    "Failed to save Basket to database: " + e.getMessage());
        }

        return basket;
    }

    public Basket assignCustomerBasket(String customerId, String basketId, String token) throws StoreException {

        Customer customer = customerMap.get(customerId);
        Basket basket = basketMap.get(basketId);

        //Check to see Customer and the Basket already exist
        if(customer == null){
            throw new StoreException("Assign Customer Basket", "Customer Does Not Exist");
        } else {
            if (basket == null){
                throw new StoreException("Assign Customer Basket", "Basket Does Not Exist");
            }
        }

        //Assign Basket to the Customer
        customer.assignBasket(basket);
        //Keep the global copy of all the baskets
        basketMap.put(basketId, basket);

        Store store = storeMap.get(customerMap.get(customerId).getStoreLocation().getStoreId());

        //Associate basket with the customer
        basket.setCustomer(customer);
        //Create bidirectional association between Store and the Basket
        basket.setStore(store);
        store.addBasket(basket);

        return basket;
    }

    public Basket getCustomerBasket(String customerId, String token) throws StoreException {
        Customer customer = customerMap.get(customerId);
        Basket basket;

        //Check if Customer exists
        if(customer == null){
            throw new StoreException("Get Customer Basket", "Customer Does Not Exist");
        } else {
            basket = customer.getBasket();
            //Check to see if Customer has been assigned the Basket
            if (basket == null) {
                throw new StoreException("Get Customer Basket", "Customer Does Not Have a Basket");
            }
        }
        return basket;
    }

    public Basket addBasketProduct(String basketId, String productId, int count, String token)
            throws StoreException {
        Basket basket = basketMap.get(basketId);
        Product product = productMap.get(productId);

        //Check to see if basket already exists product we are trying to add to the basket
        //exists as well and basket has been assigned to the customer
        if(basket == null){
            throw new StoreException("Add Basket Product", "Basket Does Not Exist");
        } else if(product == null){
            throw new StoreException("Add Basket Product", "Product Does Not Exist");
        } else if(basket.getCustomer() == null){
            throw new StoreException("Add Basket Product", "Basket Has Not Being Assigned");
        }
        //Add a product to the basket
        basket.addProduct(productId, count);

        return basket;
    }

    public Basket removeBasketProduct(String basketId, String productId, int count, String token) throws StoreException {
        Basket basket = basketMap.get(basketId);
        Product product = productMap.get(productId);

        //Check to see if basket already exists product we are trying to add to the basket
        //exists as well and basket has been assigned to the customer
        if(basket == null){
            throw new StoreException("Remove Basket Product", "Basket Does Not Exist");
        } else if(product == null){
            throw new StoreException("Remove Basket Product", "Product Does Not Exist");
        } else if(basket.getCustomer() == null){
            throw new StoreException("Remove Basket Product", "Basket Has Not Being Assigned");
        }
        //Remove product from the basket
        basket.removeProduct(productId, count);

        return basket;
    }

    public Basket clearBasket(String basketId, String token) throws StoreException {

        Basket basket = basketMap.get(basketId);

        //Check to see if basket already exists and basket has been assigned to the customer
        if(basket == null){
            throw new StoreException("Clear Basket", "Basket Does Not Exist");
        } else if(basket.getCustomer() == null){
            throw new StoreException("Clear Basket", "Basket Has Not Being Assigned");
        }
        basket.clearBasket();

        return basket;
    }

    public Basket showBasket(String basketId, String token) throws StoreException {
        Basket basket = basketMap.get(basketId);

        //Check to see if basket already exists
        if(basket == null){
            throw new StoreException("Show Basket Product", "Basket Does Not Exist");
        }
        
        // Return basket even if not assigned to a customer
        // This allows viewing empty/unassigned baskets
        return basket;
    }

    public Device provisionDevice(String deviceId, String name, String deviceType, String storeId,
                                  String aisleNumber, String token) throws StoreException {

        Store store = storeMap.get(storeId);
        Device device;
        StoreLocation storeLocation;

        //Check to see if store exists
        if(store == null){
            throw new StoreException("Provision Device", "Store Does Not Exist");
        } else {

            //Check to see if aisle exists
            Aisle aisle = store.getAisle(aisleNumber);
            if (aisle == null){
                throw new StoreException("Provision Device", "Aisle Does Not Exist");
            } else {
                storeLocation = new StoreLocation(storeId, aisleNumber);

                //Check to see if device already exists
                device = deviceMap.get(deviceId);
                if(device != null){
                    throw new StoreException("Provision Device", "Device Already Exists");
                }

                //Determine wha type of device we are trying to add
                for (SensorType sensor : SensorType.values()) {
                    if (sensor.name().equals(deviceType)){
                        device = new Sensor (deviceId, name, storeLocation, deviceType);
                    }
                }
                for (ApplianceType appliance : ApplianceType.values()) {
                    if (appliance.name().equals(deviceType)){
                        device = new Appliance(deviceId, name, storeLocation, deviceType);
                    }
                }

                //Add device to the global map
                deviceMap.put(deviceId,device);
                //Add device to the local store
                store.addDevice(device);

                //TODO: Persist Device to database
                try {
                    dataManager.saveDevice(
                            deviceId,    
                            name,        
                            deviceType,  
                            storeId,     
                            aisleNumber);
                } catch (SQLException e) {
                    throw new StoreException("Provision Device",
                            "Failed to save Device to database: " + e.getMessage());
                }
            }
        }
        return device;
    }

    public Device showDevice(String deviceId, String token) throws StoreException {
        Device device = deviceMap.get(deviceId);

        //Check to see if device exists
        if(device == null)
            throw new StoreException("Show Device", "Device Does Not Exist");

        return device;
    }

    public void raiseEvent(String deviceId, String event, String token) throws StoreException {
        Device device = deviceMap.get(deviceId);

        //Check to see if a device exists
        if(device == null){
            throw new StoreException("Raise Event", "Device Does Not Exist");
        }
        device.processEvent(event);

    }

    public void issueCommand(String deviceId, String command, String token) throws StoreException {

        Appliance appliance = (Appliance) deviceMap.get(deviceId);

        //Check to see if the appliance exists
        if(appliance == null){
            throw new StoreException("Issue Command", "Device Does Not Exist");
        }
        appliance.processCommand(command);
    }

    /**
     * Get all stores
     */
    public Collection<Store> getAllStores() {
        return storeMap.values();
    }

    /**
     * Update store information
     */
    public Store updateStore(String storeId, String description, String address) throws StoreException {
        Store store = storeMap.get(storeId);
        if (store == null) {
            throw new StoreException("Update Store", "Store Does Not Exist");
        }

        if (description != null) {
            store.setDescription(description);
        }
        if (address != null) {
            store.setAddress(address);
        }

        //TODO: Update Store data in database
        try {
            dataManager.saveStore(
                    store.getId(),        
                    store.getAddress(),   
                    store.getDescription());
        } catch (SQLException e) {
            throw new StoreException("Update Store",
                    "Failed to update Store in database: " + e.getMessage());
        }

        return store;
    }

    /**
     * Delete a store
     */
    public void deleteStore(String storeId) throws StoreException {
        Store store = storeMap.remove(storeId);
        if (store == null) {
            throw new StoreException("Delete Store", "Store Does Not Exist");
        }

        //TODO: Delete data from database
        try {
        boolean deleted = dataManager.deleteStore(storeId);

        if (!deleted) {
            storeMap.put(storeId, store);
            throw new StoreException("Delete Store",
                    "Failed to delete store from database (store not found in DB)");
            }

        } catch (SQLException e) {
            storeMap.put(storeId, store);
            throw new StoreException("Delete Store",
                    "Database error while deleting store: " + e.getMessage());
        }

    }
}
