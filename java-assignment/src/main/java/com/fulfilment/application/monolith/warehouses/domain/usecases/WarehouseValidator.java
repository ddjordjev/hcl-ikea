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
}
