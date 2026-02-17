package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ArchiveWarehouse implements ArchiveWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(ArchiveWarehouse.class);

  private final WarehouseStore warehouseStore;

  public ArchiveWarehouse(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  @Override
  public void archive(String businessUnitCode) {
    if (businessUnitCode == null || businessUnitCode.isBlank()) {
      throw new WebApplicationException("businessUnitCode is required", 422);
    }

    Warehouse existing = warehouseStore.findByBusinessUnitCode(businessUnitCode);
    if (existing == null) {
      throw new WebApplicationException("Warehouse not found", 404);
    }

    existing.archivedAt = LocalDateTime.now();
    warehouseStore.update(existing);

    LOGGER.infof("Archived warehouse %s", businessUnitCode);
  }
}
