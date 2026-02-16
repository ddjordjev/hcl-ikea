package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreateWarehouseTest {

  @Test
  void create_happyPath() {
    InMemoryStore store = new InMemoryStore();
    LocationResolver resolver = id -> new Location(id, 10, 100);
    CreateWarehouse uc = new CreateWarehouse(store, resolver);

    Warehouse w = new Warehouse();
    w.businessUnitCode = "BU-1";
    w.location = "AMSTERDAM-001";
    w.capacity = 10;
    w.stock = 5;

    uc.create(w);

    assertNotNull(store.findByBusinessUnitCode("BU-1"));
  }

  @Test
  void create_rejectsDuplicateBusinessUnitCode() {
    InMemoryStore store = new InMemoryStore();
    LocationResolver resolver = id -> new Location(id, 10, 100);
    CreateWarehouse uc = new CreateWarehouse(store, resolver);

    Warehouse w1 = new Warehouse();
    w1.businessUnitCode = "BU-1";
    w1.location = "AMSTERDAM-001";
    w1.capacity = 10;
    w1.stock = 5;
    uc.create(w1);

    Warehouse w2 = new Warehouse();
    w2.businessUnitCode = "BU-1";
    w2.location = "AMSTERDAM-001";
    w2.capacity = 10;
    w2.stock = 5;

    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> uc.create(w2));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void create_rejectsInvalidLocation() {
    InMemoryStore store = new InMemoryStore();
    LocationResolver resolver = id -> null;
    CreateWarehouse uc = new CreateWarehouse(store, resolver);

    Warehouse w = new Warehouse();
    w.businessUnitCode = "BU-2";
    w.location = "NOPE";
    w.capacity = 10;
    w.stock = 5;

    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> uc.create(w));
    assertEquals(422, ex.getResponse().getStatus());
  }

  @Test
  void create_rejectsStockGreaterThanCapacity() {
    InMemoryStore store = new InMemoryStore();
    LocationResolver resolver = id -> new Location(id, 10, 100);
    CreateWarehouse uc = new CreateWarehouse(store, resolver);

    Warehouse w = new Warehouse();
    w.businessUnitCode = "BU-3";
    w.location = "AMSTERDAM-001";
    w.capacity = 10;
    w.stock = 11;

    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> uc.create(w));
    assertEquals(422, ex.getResponse().getStatus());
  }

  @Test
  void create_rejectsWhenMaxNumberOfWarehousesReachedAtLocation() {
    InMemoryStore store = new InMemoryStore();

    // Location allows only 1 active warehouse
    LocationResolver resolver = id -> new Location(id, 1, 100);
    CreateWarehouse uc = new CreateWarehouse(store, resolver);

    Warehouse w1 = new Warehouse();
    w1.businessUnitCode = "BU-1";
    w1.location = "ZWOLLE-001";
    w1.capacity = 10;
    w1.stock = 1;
    uc.create(w1);

    Warehouse w2 = new Warehouse();
    w2.businessUnitCode = "BU-2";
    w2.location = "ZWOLLE-001";
    w2.capacity = 10;
    w2.stock = 1;

    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> uc.create(w2));
    assertEquals(422, ex.getResponse().getStatus());
  }

  @Test
  void create_rejectsWhenLocationMaxCapacityWouldBeExceeded() {
    InMemoryStore store = new InMemoryStore();

    // Location max capacity is 15 total across active warehouses
    LocationResolver resolver = id -> new Location(id, 10, 15);
    CreateWarehouse uc = new CreateWarehouse(store, resolver);

    Warehouse w1 = new Warehouse();
    w1.businessUnitCode = "BU-1";
    w1.location = "AMSTERDAM-001";
    w1.capacity = 10;
    w1.stock = 1;
    uc.create(w1);

    Warehouse w2 = new Warehouse();
    w2.businessUnitCode = "BU-2";
    w2.location = "AMSTERDAM-001";
    w2.capacity = 10; // would make total 20 > 15
    w2.stock = 1;

    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> uc.create(w2));
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