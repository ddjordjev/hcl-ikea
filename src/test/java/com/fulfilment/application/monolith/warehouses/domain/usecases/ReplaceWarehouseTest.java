package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReplaceWarehouseTest {

    private Warehouse buildWarehouse(String buCode, String location, int capacity, int stock) {
        Warehouse w = new Warehouse();
        w.businessUnitCode = buCode;
        w.location = location;
        w.capacity = capacity;
        w.stock = stock;
        w.archivedAt = null;
        return w;
    }

    @Test
    void replaceArchivesOldAndCreatesNew() {
        InMemoryWarehouseStore store = new InMemoryWarehouseStore();
        store.create(buildWarehouse("BU-1", "AMSTERDAM-001", 10, 5));
        ReplaceWarehouse uc = new ReplaceWarehouse(store);

        uc.replace(buildWarehouse("BU-1", "AMSTERDAM-001", 20, 5));

        Warehouse current = store.findByBusinessUnitCode("BU-1");
        assertNotNull(current);
        assertEquals(20, current.capacity);
        assertEquals(5, current.stock);
        assertNull(current.archivedAt);
        assertNotNull(current.createdAt);

        List<Warehouse> all = store.getAll();
        Warehouse archived =
                all.stream()
                        .filter(w -> w.archivedAt != null && "BU-1".equals(w.businessUnitCode))
                        .findFirst()
                        .orElse(null);
        assertNotNull(archived);
        assertEquals(10, archived.capacity);
    }

    @Test
    void replaceRejectsStockMismatch() {
        InMemoryWarehouseStore store = new InMemoryWarehouseStore();
        store.create(buildWarehouse("BU-1", "AMSTERDAM-001", 10, 5));
        ReplaceWarehouse uc = new ReplaceWarehouse(store);

        WebApplicationException ex =
                assertThrows(
                        WebApplicationException.class,
                        () -> uc.replace(buildWarehouse("BU-1", "AMSTERDAM-001", 10, 4)));
        assertEquals(422, ex.getResponse().getStatus());
    }

    @Test
    void replaceRejectsCapacityTooSmall() {
        InMemoryWarehouseStore store = new InMemoryWarehouseStore();
        store.create(buildWarehouse("BU-1", "AMSTERDAM-001", 10, 9));
        ReplaceWarehouse uc = new ReplaceWarehouse(store);

        WebApplicationException ex =
                assertThrows(
                        WebApplicationException.class,
                        () -> uc.replace(buildWarehouse("BU-1", "AMSTERDAM-001", 8, 9)));
        assertEquals(422, ex.getResponse().getStatus());
    }

    @Test
    void replaceRejectsDifferentLocation() {
        InMemoryWarehouseStore store = new InMemoryWarehouseStore();
        store.create(buildWarehouse("BU-1", "AMSTERDAM-001", 10, 5));
        ReplaceWarehouse uc = new ReplaceWarehouse(store);

        WebApplicationException ex =
                assertThrows(
                        WebApplicationException.class,
                        () -> uc.replace(buildWarehouse("BU-1", "ZWOLLE-001", 10, 5)));
        assertEquals(422, ex.getResponse().getStatus());
    }

    @Test
    void replaceReturns404WhenWarehouseDoesNotExist() {
        ReplaceWarehouse uc = new ReplaceWarehouse(new InMemoryWarehouseStore());

        WebApplicationException ex =
                assertThrows(
                        WebApplicationException.class,
                        () -> uc.replace(buildWarehouse("BU-404", "AMSTERDAM-001", 10, 5)));
        assertEquals(404, ex.getResponse().getStatus());
    }

    @Test
    void replaceRejectsInvalidInput() {
        ReplaceWarehouse uc = new ReplaceWarehouse(new InMemoryWarehouseStore());

        assertEquals(
                422,
                assertThrows(WebApplicationException.class, () -> uc.replace(null))
                        .getResponse()
                        .getStatus());

        assertEquals(
                422,
                assertThrows(
                        WebApplicationException.class,
                        () -> uc.replace(buildWarehouse("  ", "AMSTERDAM-001", 10, 5)))
                        .getResponse()
                        .getStatus());

        assertEquals(
                422,
                assertThrows(
                        WebApplicationException.class,
                        () -> uc.replace(buildWarehouse("BU-1", null, 10, 5)))
                        .getResponse()
                        .getStatus());

        assertEquals(
                422,
                assertThrows(
                        WebApplicationException.class,
                        () -> uc.replace(buildWarehouse("BU-1", "AMSTERDAM-001", 0, 5)))
                        .getResponse()
                        .getStatus());

        assertEquals(
                422,
                assertThrows(
                        WebApplicationException.class,
                        () -> uc.replace(buildWarehouse("BU-1", "AMSTERDAM-001", 10, -1)))
                        .getResponse()
                        .getStatus());
    }
}
