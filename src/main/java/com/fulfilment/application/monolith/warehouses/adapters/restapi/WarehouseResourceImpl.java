package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.usecases.ArchiveWarehouse;
import com.fulfilment.application.monolith.warehouses.domain.usecases.CreateWarehouse;
import com.fulfilment.application.monolith.warehouses.domain.usecases.ReplaceWarehouse;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import org.jboss.logging.Logger;

import java.util.List;

@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

    private static final Logger LOGGER = Logger.getLogger(WarehouseResourceImpl.class);

    @Inject
    WarehouseRepository warehouseRepository;
    @Inject
    CreateWarehouse createWarehouse;
    @Inject
    ReplaceWarehouse replaceWarehouse;
    @Inject
    ArchiveWarehouse archiveWarehouse;

    @Override
    public List<Warehouse> listAllWarehousesUnits() {
        return warehouseRepository.getAll().stream().map(this::toWarehouseResponse).toList();
    }

    @Override
    @Transactional
    public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
        LOGGER.infof("Creating warehouse %s", data.getBusinessUnitCode());
        var domain = toDomain(data);
        createWarehouse.create(domain);

        var stored = warehouseRepository.findByBusinessUnitCode(domain.businessUnitCode);
        if (stored == null) {
            throw new WebApplicationException("Warehouse was not created", 500);
        }
        return toWarehouseResponse(stored);
    }

    @Override
    public Warehouse getAWarehouseUnitByID(String id) {
        LOGGER.debugf("Fetching warehouse id=%s", id);
        var found = warehouseRepository.getById(parseId(id));
        if (found == null) {
            throw new WebApplicationException("Warehouse not found", 404);
        }
        return toWarehouseResponse(found);
    }

    @Override
    @Transactional
    public void archiveAWarehouseUnitByID(String id) {
        LOGGER.infof("Archiving warehouse id=%s", id);
        archiveWarehouse.archive(parseId(id));
    }

    @Override
    @Transactional
    public Warehouse replaceTheCurrentActiveWarehouse(
            String businessUnitCode, @NotNull Warehouse data) {
        LOGGER.infof("Replacing warehouse %s", businessUnitCode);
        var domain = toDomain(data);
        domain.businessUnitCode = businessUnitCode;

        replaceWarehouse.replace(domain);

        var current = warehouseRepository.findByBusinessUnitCode(businessUnitCode);
        if (current == null) {
            throw new WebApplicationException("Warehouse not found after replacement", 500);
        }
        return toWarehouseResponse(current);
    }

    private Long parseId(String id) {
        try {
            return Long.valueOf(id);
        } catch (NumberFormatException e) {
            throw new WebApplicationException("Invalid warehouse id: " + id, 400);
        }
    }

    private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse toDomain(
            Warehouse data) {
        if (data == null) {
            throw new WebApplicationException("Warehouse payload is required", 422);
        }
        var domain = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
        domain.businessUnitCode = data.getBusinessUnitCode();
        domain.location = data.getLocation();
        domain.capacity = data.getCapacity();
        domain.stock = data.getStock();
        return domain;
    }

    private Warehouse toWarehouseResponse(
            com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
        var response = new Warehouse();
        response.setId(warehouse.id != null ? warehouse.id.toString() : null);
        response.setBusinessUnitCode(warehouse.businessUnitCode);
        response.setLocation(warehouse.location);
        response.setCapacity(warehouse.capacity);
        response.setStock(warehouse.stock);
        return response;
    }
}
