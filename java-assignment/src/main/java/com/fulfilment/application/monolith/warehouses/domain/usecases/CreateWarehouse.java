package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class CreateWarehouse implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouse(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    validateRequiredFields(warehouse);

    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new WebApplicationException("businessUnitCode already exists", 409);
    }

    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new WebApplicationException("Invalid location", 422);
    }

    List<Warehouse> all = warehouseStore.getAll();
    List<Warehouse> activeAtLocation =
        all.stream()
            .filter(w -> w != null)
            .filter(w -> w.archivedAt == null)
            .filter(w -> warehouse.location.equals(w.location))
            .toList();

    if (activeAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new WebApplicationException("Max number of warehouses reached for location", 422);
    }

    int currentTotalCapacity =
        activeAtLocation.stream()
            .map(w -> w.capacity == null ? 0 : w.capacity)
            .reduce(0, Integer::sum);

    if (currentTotalCapacity + warehouse.capacity > location.maxCapacity) {
      throw new WebApplicationException("Location max capacity exceeded", 422);
    }

    if (warehouse.stock > warehouse.capacity) {
      throw new WebApplicationException("Stock cannot exceed capacity", 422);
    }

    warehouse.createdAt = warehouse.createdAt != null ? warehouse.createdAt : LocalDateTime.now();
    warehouse.archivedAt = null;

    warehouseStore.create(warehouse);
  }

  private static void validateRequiredFields(Warehouse warehouse) {
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
