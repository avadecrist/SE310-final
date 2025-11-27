package com.se310.store.dto;

/**
 * Interface for objects that can be serialized to JSON.
 * This interface enables the DTO pattern by allowing BaseServlet
 * to work with any DTO without knowing about specific implementations.
 *
 * This follows the Open/Closed Principle: BaseServlet is open for extension
 * (new DTOs can be added) but closed for modification.
 *
 * @author  Sergey L. Sundukovskiy
 * @version 1.0
 * @since   2025-11-13
 */ 
public interface JsonSerializable {

    //TODO: Implement Strategy Pattern for JSON serialization
    /**
     * Convert this object to JSON string representation.
     *
     * @return JSON string representation of this object
     */
    default String toJson(){
        return JsonHelper.toJson(this); //Delegates to the shared Gson configuration
    }

}