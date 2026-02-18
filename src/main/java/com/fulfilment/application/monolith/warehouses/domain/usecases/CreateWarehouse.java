package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class CreateWarehouse implements CreateWarehouseOperation {

    private static final Logger LOGGER = Logger.getLogger(CreateWarehouse.class);

    private final WarehouseStore warehouseStore;
    private final LocationResolver locationResolver;

    public CreateWarehouse(WarehouseStore warehouseStore, LocationResolver locationResolver) {
        this.warehouseStore = warehouseStore;
        this.locationResolver = locationResolver;
    }

    @Override
    public void create(Warehouse warehouse) {
        WarehouseValidator.validateRequiredFields(warehouse);

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

        WarehouseValidator.validateStockWithinCapacity(warehouse);

        warehouse.createdAt =
                warehouse.createdAt != null ? warehouse.createdAt : LocalDateTime.now();
        warehouse.archivedAt = null;

        warehouseStore.create(warehouse);

        LOGGER.infof("Warehouse %s created at %s", warehouse.businessUnitCode, warehouse.location);
    }
}
