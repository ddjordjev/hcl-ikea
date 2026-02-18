package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class FulfillmentEndpointIT {

    private static final String BASE_PATH = "fulfillment";

    @Inject
    FulfillmentRepository fulfillmentRepository;
    @Inject
    WarehouseRepository warehouseRepository;
    @Inject
    ProductRepository productRepository;

    private String whA, whB, whC;
    private Long prodA, prodB, prodC;
    private Long storeA, storeB, storeC;

    @BeforeEach
    @Transactional
    void setUp() {
        fulfillmentRepository.deleteAll();
        warehouseRepository.deleteAll();
        productRepository.deleteAll();
        Store.deleteAll();

        whA = seedWarehouse("MWH.001", "ZWOLLE-001", 100, 10);
        whB = seedWarehouse("MWH.012", "AMSTERDAM-001", 50, 5);
        whC = seedWarehouse("MWH.023", "TILBURG-001", 30, 27);

        prodA = seedProduct("TONSTAD", 10);
        prodB = seedProduct("KALLAX", 5);
        prodC = seedProduct("BESTÅ", 3);

        storeA = seedStore("TONSTAD", 10);
        storeB = seedStore("KALLAX", 5);
        storeC = seedStore("BESTÅ", 3);
    }

    @Test
    public void testCreateAssignment() {
        given().contentType(ContentType.JSON)
                .body(assignment(whA, prodA, storeA))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .body("warehouseBusinessUnitCode", equalTo(whA))
                .body("productId", equalTo(prodA.intValue()))
                .body("storeId", equalTo(storeA.intValue()))
                .body("id", notNullValue())
                .body("createdAt", notNullValue());
    }

    @Test
    public void testListAll() {
        createAssignment(whA, prodA, storeA);
        createAssignment(whB, prodB, storeB);

        given().when().get(BASE_PATH).then().statusCode(200).body("size()", is(2));
    }

    @Test
    public void testListByStore() {
        createAssignment(whA, prodA, storeA);
        createAssignment(whB, prodB, storeB);

        given().when()
                .get(BASE_PATH + "/store/" + storeA)
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].warehouseBusinessUnitCode", equalTo(whA));
    }

    @Test
    public void testListByWarehouse() {
        createAssignment(whA, prodA, storeA);

        given().when()
                .get(BASE_PATH + "/warehouse/" + whA)
                .then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    public void testListByProduct() {
        createAssignment(whA, prodA, storeA);

        given().when()
                .get(BASE_PATH + "/product/" + prodA)
                .then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    public void testListByStoreReturnsEmptyWhenNoAssignments() {
        given().when()
                .get(BASE_PATH + "/store/" + storeC)
                .then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    public void testRejectsMissingRequiredFields() {
        given().contentType(ContentType.JSON)
                .body(String.format("{\"productId\": %d, \"storeId\": %d}", prodA, storeA))
                .post(BASE_PATH)
                .then()
                .statusCode(422);

        given().contentType(ContentType.JSON)
                .body(String.format(
                        "{\"warehouseBusinessUnitCode\": \"%s\", \"storeId\": %d}", whA, storeA))
                .post(BASE_PATH)
                .then()
                .statusCode(422);

        given().contentType(ContentType.JSON)
                .body(String.format(
                        "{\"warehouseBusinessUnitCode\": \"%s\", \"productId\": %d}", whA, prodA))
                .post(BASE_PATH)
                .then()
                .statusCode(422);
    }

    @Test
    public void testRejectsNonExistentEntities() {
        given().contentType(ContentType.JSON)
                .body(assignment("DOES-NOT-EXIST", prodA, storeA))
                .post(BASE_PATH)
                .then()
                .statusCode(404);

        given().contentType(ContentType.JSON)
                .body(assignment(whA, 9999L, storeA))
                .post(BASE_PATH)
                .then()
                .statusCode(404);

        given().contentType(ContentType.JSON)
                .body(assignment(whA, prodA, 9999L))
                .post(BASE_PATH)
                .then()
                .statusCode(404);
    }

    @Test
    public void testRejectsDuplicateAssignment() {
        createAssignment(whA, prodA, storeA);

        given().contentType(ContentType.JSON)
                .body(assignment(whA, prodA, storeA))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(409)
                .body("error", containsString("already exists"));
    }

    @Test
    public void testMaxTwoWarehousesPerProductPerStore() {
        createAssignment(whA, prodA, storeA);
        createAssignment(whB, prodA, storeA);

        given().contentType(ContentType.JSON)
                .body(assignment(whC, prodA, storeA))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(422)
                .body("error", containsString("Maximum reached"));
    }

    @Test
    public void testDifferentProductAtSameStoreBypassesWarehouseLimit() {
        createAssignment(whA, prodA, storeA);
        createAssignment(whB, prodA, storeA);

        given().contentType(ContentType.JSON)
                .body(assignment(whC, prodB, storeA))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201);
    }

    @Test
    public void testMaxThreeWarehousesPerStore() {
        createAssignment(whA, prodA, storeA);
        createAssignment(whB, prodB, storeA);
        createAssignment(whC, prodC, storeA);

        // whA already fulfills storeA, so this doesn't count as a new warehouse
        given().contentType(ContentType.JSON)
                .body(assignment(whA, prodB, storeA))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201);
    }

    @Test
    public void testSameWarehouseProductDifferentStoreAllowed() {
        createAssignment(whB, prodA, storeA);

        given().contentType(ContentType.JSON)
                .body(assignment(whB, prodA, storeB))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201);
    }

    @Test
    public void testDeleteAssignment() {
        int id = createAssignment(whA, prodA, storeA);

        given().when().delete(BASE_PATH + "/" + id).then().statusCode(204);

        given().when().get(BASE_PATH).then().statusCode(200).body("size()", is(0));
    }

    @Test
    public void testDeleteNonExistentAssignment() {
        given().when().delete(BASE_PATH + "/9999").then().statusCode(404);
    }

    private String seedWarehouse(String buCode, String location, int capacity, int stock) {
        Warehouse w = new Warehouse();
        w.businessUnitCode = buCode;
        w.location = location;
        w.capacity = capacity;
        w.stock = stock;
        w.createdAt = LocalDateTime.now();
        warehouseRepository.create(w);
        return buCode;
    }

    private Long seedProduct(String name, int stock) {
        Product p = new Product();
        p.name = name;
        p.stock = stock;
        productRepository.persist(p);
        return p.id;
    }

    private Long seedStore(String name, int qty) {
        Store s = new Store(name);
        s.quantityProductsInStock = qty;
        s.persist();
        return s.id;
    }

    private int createAssignment(String warehouseCode, Long productId, Long storeId) {
        return given().contentType(ContentType.JSON)
                .body(assignment(warehouseCode, productId, storeId))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    private static String assignment(String warehouseCode, Long productId, Long storeId) {
        return String.format(
                "{\"warehouseBusinessUnitCode\": \"%s\", \"productId\": %d, \"storeId\": %d}",
                warehouseCode, productId, storeId);
    }
}
