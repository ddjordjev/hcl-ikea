package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

    @Override
    public List<Warehouse> getAll() {
        return list("archivedAt is null").stream().map(DbWarehouse::toWarehouse).toList();
    }

    @Override
    public void create(Warehouse warehouse) {
        if (warehouse == null) {
            throw new WebApplicationException("Warehouse payload is required", 422);
        }

        DbWarehouse db = new DbWarehouse();
        db.businessUnitCode = warehouse.businessUnitCode;
        db.location = warehouse.location;
        db.capacity = warehouse.capacity;
        db.stock = warehouse.stock;
        db.createdAt = warehouse.createdAt != null ? warehouse.createdAt : LocalDateTime.now();
        db.archivedAt = warehouse.archivedAt;

        persist(db);
    }

    @Override
    public void update(Warehouse warehouse) {
        if (warehouse == null
                || warehouse.businessUnitCode == null
                || warehouse.businessUnitCode.isBlank()) {
            throw new WebApplicationException("Warehouse businessUnitCode is required", 422);
        }

        DbWarehouse db =
                find("businessUnitCode = ?1 and archivedAt is null", warehouse.businessUnitCode)
                        .firstResult();

        if (db == null) {
            throw new WebApplicationException(
                    "Active warehouse with businessUnitCode "
                            + warehouse.businessUnitCode
                            + " not found",
                    404);
        }

        db.location = warehouse.location;
        db.capacity = warehouse.capacity;
        db.stock = warehouse.stock;
        db.archivedAt = warehouse.archivedAt;
    }

    @Override
    public void remove(Warehouse warehouse) {
        if (warehouse == null
                || warehouse.businessUnitCode == null
                || warehouse.businessUnitCode.isBlank()) {
            throw new WebApplicationException("Warehouse businessUnitCode is required", 422);
        }

        DbWarehouse db =
                find("businessUnitCode = ?1 and archivedAt is null", warehouse.businessUnitCode)
                        .firstResult();

        if (db == null) {
            throw new WebApplicationException(
                    "Active warehouse with businessUnitCode "
                            + warehouse.businessUnitCode
                            + " not found",
                    404);
        }

        delete(db);
    }

    @Override
    public Warehouse findByBusinessUnitCode(String buCode) {
        if (buCode == null || buCode.isBlank()) {
            return null;
        }

        DbWarehouse db = find("businessUnitCode = ?1 and archivedAt is null", buCode).firstResult();
        return db == null ? null : db.toWarehouse();
    }

    @Override
    public Warehouse getById(Long id) {
        if (id == null) {
            return null;
        }

        DbWarehouse db = find("id = ?1 and archivedAt is null", id).firstResult();
        return db == null ? null : db.toWarehouse();
    }
}
