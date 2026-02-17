package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

class CreateWarehouseTest {

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
  void createHappyPath() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();
    LocationResolver resolver = id -> new Location(id, 10, 100);
    CreateWarehouse uc = new CreateWarehouse(store, resolver);

    uc.create(buildWarehouse("BU-1", "AMSTERDAM-001", 10, 5));

    Warehouse created = store.findByBusinessUnitCode("BU-1");
    assertNotNull(created);
    assertEquals(10, created.capacity);
    assertEquals(5, created.stock);
    assertNotNull(created.createdAt);
    assertNull(created.archivedAt);
  }

  @Test
  void createRejectsDuplicateBusinessUnitCode() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();
    LocationResolver resolver = id -> new Location(id, 10, 100);
    CreateWarehouse uc = new CreateWarehouse(store, resolver);

    uc.create(buildWarehouse("BU-1", "AMSTERDAM-001", 10, 5));

    WebApplicationException ex = assertThrows(WebApplicationException.class,
        () -> uc.create(buildWarehouse("BU-1", "AMSTERDAM-001", 10, 5)));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void createRejectsInvalidLocation() {
    LocationResolver resolver = id -> null;
    CreateWarehouse uc = new CreateWarehouse(new InMemoryWarehouseStore(), resolver);

    WebApplicationException ex = assertThrows(WebApplicationException.class,
        () -> uc.create(buildWarehouse("BU-2", "NOPE", 10, 5)));
    assertEquals(422, ex.getResponse().getStatus());
  }

  @Test
  void createRejectsStockGreaterThanCapacity() {
    LocationResolver resolver = id -> new Location(id, 10, 100);
    CreateWarehouse uc = new CreateWarehouse(new InMemoryWarehouseStore(), resolver);

    WebApplicationException ex = assertThrows(WebApplicationException.class,
        () -> uc.create(buildWarehouse("BU-3", "AMSTERDAM-001", 10, 11)));
    assertEquals(422, ex.getResponse().getStatus());
  }

  @Test
  void createRejectsWhenMaxWarehousesAtLocationReached() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();
    LocationResolver resolver = id -> new Location(id, 1, 100);
    CreateWarehouse uc = new CreateWarehouse(store, resolver);

    uc.create(buildWarehouse("BU-1", "ZWOLLE-001", 10, 1));

    WebApplicationException ex = assertThrows(WebApplicationException.class,
        () -> uc.create(buildWarehouse("BU-2", "ZWOLLE-001", 10, 1)));
    assertEquals(422, ex.getResponse().getStatus());
  }

  @Test
  void createRejectsWhenLocationCapacityExceeded() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();
    LocationResolver resolver = id -> new Location(id, 10, 15);
    CreateWarehouse uc = new CreateWarehouse(store, resolver);

    uc.create(buildWarehouse("BU-1", "AMSTERDAM-001", 10, 1));

    WebApplicationException ex = assertThrows(WebApplicationException.class,
        () -> uc.create(buildWarehouse("BU-2", "AMSTERDAM-001", 10, 1)));
    assertEquals(422, ex.getResponse().getStatus());
  }
}
