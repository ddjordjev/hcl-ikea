package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReplaceWarehouseTest {

    @Test
    void replace_rejectsStockMismatch() {
        InMemoryStore store = new InMemoryStore();
        Warehouse existing = new Warehouse();
        existing.businessUnitCode = "BU-1";
        existing.location = "AMSTERDAM-001";
        existing.capacity = 10;
        existing.stock = 5;
        store.create(existing);

        ReplaceWarehouse uc = new ReplaceWarehouse(store);

        Warehouse replacement = new Warehouse();
        replacement.businessUnitCode = "BU-1";
        replacement.location = "AMSTERDAM-001";
        replacement.capacity = 10;
        replacement.stock = 4;

        WebApplicationException ex =
                assertThrows(WebApplicationException.class, () -> uc.replace(replacement));
        assertEquals(422, ex.getResponse().getStatus());
    }

    @Test
    void replace_rejectsCapacityTooSmall() {
        InMemoryStore store = new InMemoryStore();
        Warehouse existing = new Warehouse();
        existing.businessUnitCode = "BU-1";
        existing.location = "AMSTERDAM-001";
        existing.capacity = 10;
        existing.stock = 9;
        store.create(existing);

        ReplaceWarehouse uc = new ReplaceWarehouse(store);

        Warehouse replacement = new Warehouse();
        replacement.businessUnitCode = "BU-1";
        replacement.location = "AMSTERDAM-001";
        replacement.capacity = 8;
        replacement.stock = 9;

        WebApplicationException ex =
                assertThrows(WebApplicationException.class, () -> uc.replace(replacement));
        assertEquals(422, ex.getResponse().getStatus());
    }

    @Test
    void replace_rejectsDifferentLocation() {
        InMemoryStore store = new InMemoryStore();
        Warehouse existing = new Warehouse();
        existing.businessUnitCode = "BU-1";
        existing.location = "AMSTERDAM-001";
        existing.capacity = 10;
        existing.stock = 5;
        store.create(existing);

        ReplaceWarehouse uc = new ReplaceWarehouse(store);

        Warehouse replacement = new Warehouse();
        replacement.businessUnitCode = "BU-1";
        replacement.location = "ZWOLLE-001";
        replacement.capacity = 10;
        replacement.stock = 5;

        WebApplicationException ex =
                assertThrows(WebApplicationException.class, () -> uc.replace(replacement));
        assertEquals(422, ex.getResponse().getStatus());
    }

    @Test
    void replace_returns404WhenWarehouseToReplaceDoesNotExist() {
        InMemoryStore store = new InMemoryStore();
        ReplaceWarehouse uc = new ReplaceWarehouse(store);

        Warehouse replacement = new Warehouse();
        replacement.businessUnitCode = "BU-404";
        replacement.location = "AMSTERDAM-001";
        replacement.capacity = 10;
        replacement.stock = 5;

        WebApplicationException ex =
                assertThrows(WebApplicationException.class, () -> uc.replace(replacement));
        assertEquals(404, ex.getResponse().getStatus());
    }

    @Test
    void replace_rejectsNullLocation() {
        InMemoryStore store = new InMemoryStore();
        Warehouse existing = new Warehouse();
        existing.businessUnitCode = "BU-1";
        existing.location = "AMSTERDAM-001";
        existing.capacity = 10;
        existing.stock = 5;
        store.create(existing);

        ReplaceWarehouse uc = new ReplaceWarehouse(store);

        Warehouse replacement = new Warehouse();
        replacement.businessUnitCode = "BU-1";
        replacement.location = null;
        replacement.capacity = 10;
        replacement.stock = 5;

        WebApplicationException ex =
                assertThrows(WebApplicationException.class, () -> uc.replace(replacement));
        assertEquals(422, ex.getResponse().getStatus());
    }

    @Test
    void replace_rejectsNullPayload() {
        ReplaceWarehouse uc = new ReplaceWarehouse(new InMemoryStore());
        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> uc.replace(null));
        assertEquals(422, ex.getResponse().getStatus());
    }

    @Test
    void replace_rejectsMissingBusinessUnitCode() {
        ReplaceWarehouse uc = new ReplaceWarehouse(new InMemoryStore());

        Warehouse replacement = new Warehouse();
        replacement.businessUnitCode = "  ";
        replacement.location = "AMSTERDAM-001";
        replacement.capacity = 10;
        replacement.stock = 5;

        WebApplicationException ex =
                assertThrows(WebApplicationException.class, () -> uc.replace(replacement));
        assertEquals(422, ex.getResponse().getStatus());
    }

    @Test
    void replace_rejectsNonPositiveCapacity() {
        ReplaceWarehouse uc = new ReplaceWarehouse(new InMemoryStore());

        Warehouse replacement = new Warehouse();
        replacement.businessUnitCode = "BU-1";
        replacement.location = "AMSTERDAM-001";
        replacement.capacity = 0;
        replacement.stock = 5;

        WebApplicationException ex =
                assertThrows(WebApplicationException.class, () -> uc.replace(replacement));
        assertEquals(422, ex.getResponse().getStatus());
    }

    @Test
    void replace_rejectsNegativeStock() {
        ReplaceWarehouse uc = new ReplaceWarehouse(new InMemoryStore());

        Warehouse replacement = new Warehouse();
        replacement.businessUnitCode = "BU-1";
        replacement.location = "AMSTERDAM-001";
        replacement.capacity = 10;
        replacement.stock = -1;

        WebApplicationException ex =
                assertThrows(WebApplicationException.class, () -> uc.replace(replacement));
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
            // no-op for unit tests
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
}
