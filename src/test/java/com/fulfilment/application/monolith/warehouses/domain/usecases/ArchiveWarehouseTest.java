package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArchiveWarehouseTest {

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
    void archiveSetsArchivedAt() {
        InMemoryWarehouseStore store = new InMemoryWarehouseStore();
        Warehouse created = buildWarehouse("BU-1", "AMSTERDAM-001", 10, 5);
        store.create(created);

        new ArchiveWarehouse(store).archive(created.id);

        Warehouse archived =
                store.getAll().stream()
                        .filter(w -> "BU-1".equals(w.businessUnitCode))
                        .findFirst()
                        .orElseThrow();
        assertNotNull(archived.archivedAt);
    }

    @Test
    void archiveRejectsMissingWarehouse() {
        ArchiveWarehouse uc = new ArchiveWarehouse(new InMemoryWarehouseStore());

        WebApplicationException ex =
                assertThrows(WebApplicationException.class, () -> uc.archive(9999L));
        assertEquals(404, ex.getResponse().getStatus());
    }

    @Test
    void archiveRejectsNullId() {
        ArchiveWarehouse uc = new ArchiveWarehouse(new InMemoryWarehouseStore());

        assertEquals(
                422,
                assertThrows(WebApplicationException.class, () -> uc.archive(null))
                        .getResponse()
                        .getStatus());
    }
}
