package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ReplaceWarehouse implements ReplaceWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(ReplaceWarehouse.class);

  private final WarehouseStore warehouseStore;

  public ReplaceWarehouse(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    WarehouseValidator.validateRequiredFields(newWarehouse);

    Warehouse current = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (current == null) {
      throw new WebApplicationException("Warehouse to replace not found", 404);
    }

    if (newWarehouse.stock.intValue() != current.stock.intValue()) {
      throw new WebApplicationException("Replacement warehouse stock must match existing stock", 422);
    }

    if (newWarehouse.capacity < current.stock) {
      throw new WebApplicationException("Replacement warehouse capacity must accommodate existing stock", 422);
    }

    if (!newWarehouse.location.equals(current.location)) {
      throw new WebApplicationException("Replacement warehouse must be in the same location", 422);
    }

    current.archivedAt = LocalDateTime.now();
    warehouseStore.update(current);

    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = current.businessUnitCode;
    replacement.location = current.location;
    replacement.capacity = newWarehouse.capacity;
    replacement.stock = current.stock;
    replacement.createdAt = LocalDateTime.now();
    replacement.archivedAt = null;

    warehouseStore.create(replacement);

    LOGGER.infof("Replaced warehouse %s, new capacity %d", current.businessUnitCode, newWarehouse.capacity);
  }
}