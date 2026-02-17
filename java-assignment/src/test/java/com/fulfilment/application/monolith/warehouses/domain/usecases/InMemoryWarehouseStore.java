package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.ArrayList;
import java.util.List;

class InMemoryWarehouseStore implements WarehouseStore {

    private final List<Warehouse> warehouses = new ArrayList<>();

    @Override
    public List<Warehouse> getAll() {
        return List.copyOf(warehouses);
    }

    @Override
    public void create(Warehouse warehouse) {
        warehouses.add(warehouse);
    }

    @Override
    public void update(Warehouse warehouse) {
        for (int i = 0; i < warehouses.size(); i++) {
            if (warehouse.businessUnitCode != null
                    && warehouse.businessUnitCode.equals(warehouses.get(i).businessUnitCode)) {
                warehouses.set(i, warehouse);
                return;
            }
        }
    }

    @Override
    public void remove(Warehouse warehouse) {
        warehouses.remove(warehouse);
    }

    @Override
    public Warehouse findByBusinessUnitCode(String buCode) {
        return warehouses.stream()
                .filter(w -> w.archivedAt == null)
                .filter(w -> buCode.equals(w.businessUnitCode))
                .findFirst()
                .orElse(null);
    }
}
