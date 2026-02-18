package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import jakarta.ws.rs.WebApplicationException;

class WarehouseValidator {

    static void validateRequiredFields(Warehouse warehouse) {
        if (warehouse == null) {
            throw new WebApplicationException("Warehouse payload is required", 422);
        }
        if (warehouse.businessUnitCode == null || warehouse.businessUnitCode.isBlank()) {
            throw new WebApplicationException("businessUnitCode is required", 422);
        }
        if (warehouse.location == null || warehouse.location.isBlank()) {
            throw new WebApplicationException("location is required", 422);
        }
        if (warehouse.capacity == null || warehouse.capacity <= 0) {
            throw new WebApplicationException("capacity must be > 0", 422);
        }
        if (warehouse.stock == null || warehouse.stock < 0) {
            throw new WebApplicationException("stock must be >= 0", 422);
        }
    }

    static void validateStockWithinCapacity(Warehouse warehouse) {
        if (warehouse.stock > warehouse.capacity) {
            throw new WebApplicationException("Stock cannot exceed capacity", 422);
        }
    }

    static void validateReplacement(Warehouse replacement, Warehouse current) {
        if (!replacement.location.equals(current.location)) {
            throw new WebApplicationException(
                    "Replacement warehouse must be in the same location", 422);
        }
        if (replacement.stock.intValue() != current.stock.intValue()) {
            throw new WebApplicationException(
                    "Replacement warehouse stock must match existing stock", 422);
        }
        if (replacement.capacity < current.stock) {
            throw new WebApplicationException(
                    "Replacement warehouse capacity must accommodate existing stock", 422);
        }
    }
}
