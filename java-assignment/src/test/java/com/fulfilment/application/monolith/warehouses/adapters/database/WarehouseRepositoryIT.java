package com.fulfilment.application.monolith.warehouses.adapters.database;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WarehouseRepositoryIT {

    @Inject WarehouseRepository repo;

    @BeforeEach
    @Transactional
    void cleanUp() {
        repo.deleteAll();
    }

    @Test
    @Transactional
    public void testCreateAndFindByBusinessUnitCode() {
        Warehouse w = warehouse("REPO-01", "ZWOLLE-001", 30, 8);
        repo.create(w);

        Warehouse found = repo.findByBusinessUnitCode("REPO-01");
        assertNotNull(found);
        assertEquals("ZWOLLE-001", found.location);
        assertEquals(30, found.capacity);
    }

    @Test
    @Transactional
    public void testGetAllExcludesArchived() {
        repo.create(warehouse("ACTIVE-1", "AMSTERDAM-001", 20, 5));

        Warehouse archived = warehouse("OLD-ONE", "AMSTERDAM-001", 10, 2);
        archived.archivedAt = LocalDateTime.now();
        repo.create(archived);

        List<Warehouse> all = repo.getAll();
        assertTrue(all.stream().anyMatch(wh -> "ACTIVE-1".equals(wh.businessUnitCode)));
        assertTrue(all.stream().noneMatch(wh -> "OLD-ONE".equals(wh.businessUnitCode)));
    }

    @Test
    @Transactional
    public void testUpdateSetsFields() {
        repo.create(warehouse("UPD-1", "TILBURG-001", 20, 5));

        Warehouse update = warehouse("UPD-1", "TILBURG-001", 40, 15);
        update.archivedAt = LocalDateTime.now();
        repo.update(update);

        Warehouse found = repo.findByBusinessUnitCode("UPD-1");
        assertNull(found, "archived warehouse should not be found by findByBusinessUnitCode");
    }

    @Test
    @Transactional
    public void testFindByBusinessUnitCodeReturnsNullForMissing() {
        assertNull(repo.findByBusinessUnitCode("NOPE-999"));
        assertNull(repo.findByBusinessUnitCode(null));
        assertNull(repo.findByBusinessUnitCode("  "));
    }

    @Test
    @Transactional
    public void testRemoveDeletesWarehouse() {
        repo.create(warehouse("DEL-ME", "AMSTERDAM-001", 20, 5));
        assertNotNull(repo.findByBusinessUnitCode("DEL-ME"));

        repo.remove(warehouse("DEL-ME", "AMSTERDAM-001", 20, 5));
        assertNull(repo.findByBusinessUnitCode("DEL-ME"));
    }

    @Test
    @Transactional
    public void testRemoveThrows404ForMissing() {
        Warehouse ghost = warehouse("GHOST", "AMSTERDAM-001", 10, 1);
        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> repo.remove(ghost));
        assertEquals(404, ex.getResponse().getStatus());
    }

    private static Warehouse warehouse(String buCode, String location, int capacity, int stock) {
        Warehouse w = new Warehouse();
        w.businessUnitCode = buCode;
        w.location = location;
        w.capacity = capacity;
        w.stock = stock;
        w.createdAt = LocalDateTime.now();
        return w;
    }
}
