package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArchiveWarehouseTest {

    @Test
    void archive_setsArchivedAt() {
        InMemoryStore store = new InMemoryStore();
        Warehouse existing = new Warehouse();
        existing.businessUnitCode = "BU-1";
        existing.location = "AMSTERDAM-001";
        existing.capacity = 10;
        existing.stock = 5;
        store.create(existing);

        ArchiveWarehouse uc = new ArchiveWarehouse(store);

        Warehouse request = new Warehouse();
        request.businessUnitCode = "BU-1";

        uc.archive(request);

        Warehouse updated =
                store.getAll().stream()
                        .filter(w -> "BU-1".equals(w.businessUnitCode))
                        .findFirst()
                        .orElseThrow();

        assertNotNull(updated.archivedAt);
    }

    @Test
    void archive_rejectsMissingWarehouse() {
        InMemoryStore store = new InMemoryStore();
        ArchiveWarehouse uc = new ArchiveWarehouse(store);

        Warehouse request = new Warehouse();
        request.businessUnitCode = "BU-404";

        WebApplicationException ex =
                assertThrows(WebApplicationException.class, () -> uc.archive(request));
        assertEquals(404, ex.getResponse().getStatus());
    }

    @Test
    void archive_rejectsNullPayload() {
        ArchiveWarehouse uc = new ArchiveWarehouse(new InMemoryStore());
        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> uc.archive(null));
        assertEquals(422, ex.getResponse().getStatus());
    }

    @Test
    void archive_rejectsBlankBusinessUnitCode() {
        ArchiveWarehouse uc = new ArchiveWarehouse(new InMemoryStore());
        Warehouse request = new Warehouse();
        request.businessUnitCode = "   ";

        WebApplicationException ex =
                assertThrows(WebApplicationException.class, () -> uc.archive(request));
        assertEquals(422, ex.getResponse().getStatus());
    }

    static class InMemoryStore implements WarehouseStore {
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
                    .filter(w -> w.archivedAt == null) // active-only lookup
                    .filter(w -> buCode.equals(w.businessUnitCode))
                    .findFirst()
                    .orElse(null);
        }
    }
}