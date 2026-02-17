package com.fulfilment.application.monolith.fulfillment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class FulfillmentEndpointIT {

    private static final String BASE_PATH = "fulfillment";

    @Inject
    FulfillmentRepository fulfillmentRepository;

    @Inject
    EntityManager em;

    @BeforeEach
    @Transactional
    public void setUp() {
        fulfillmentRepository.deleteAll();
        restoreSeedData();
    }

    @Test
    public void testCreateAssignment() {
        given()
                .contentType(ContentType.JSON)
                .body(assignment("MWH.001", 1, 1))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .body("warehouseBusinessUnitCode", equalTo("MWH.001"))
                .body("productId", equalTo(1))
                .body("storeId", equalTo(1))
                .body("id", notNullValue())
                .body("createdAt", notNullValue());
    }

    @Test
    public void testListAll() {
        createAssignment("MWH.001", 1, 1);
        createAssignment("MWH.012", 2, 2);

        given()
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .body("size()", is(2));
    }

    @Test
    public void testListByStore() {
        createAssignment("MWH.001", 1, 1);
        createAssignment("MWH.012", 2, 2);

        given()
                .when()
                .get(BASE_PATH + "/store/1")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].warehouseBusinessUnitCode", equalTo("MWH.001"));
    }

    @Test
    public void testListByWarehouse() {
        createAssignment("MWH.001", 1, 1);

        given()
                .when()
                .get(BASE_PATH + "/warehouse/MWH.001")
                .then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    public void testListByProduct() {
        createAssignment("MWH.001", 1, 1);

        given()
                .when()
                .get(BASE_PATH + "/product/1")
                .then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    public void testListByStoreReturnsEmptyWhenNoAssignments() {
        given()
                .when()
                .get(BASE_PATH + "/store/3")
                .then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    public void testRejectsMissingRequiredFields() {
        given().contentType(ContentType.JSON)
                .body("{\"productId\": 1, \"storeId\": 1}")
                .post(BASE_PATH).then().statusCode(422);

        given().contentType(ContentType.JSON)
                .body("{\"warehouseBusinessUnitCode\": \"MWH.001\", \"storeId\": 1}")
                .post(BASE_PATH).then().statusCode(422);

        given().contentType(ContentType.JSON)
                .body("{\"warehouseBusinessUnitCode\": \"MWH.001\", \"productId\": 1}")
                .post(BASE_PATH).then().statusCode(422);
    }

    @Test
    public void testRejectsNonExistentEntities() {
        given().contentType(ContentType.JSON)
                .body(assignment("DOES-NOT-EXIST", 1, 1))
                .post(BASE_PATH).then().statusCode(404);

        given().contentType(ContentType.JSON)
                .body(assignment("MWH.001", 9999, 1))
                .post(BASE_PATH).then().statusCode(404);

        given().contentType(ContentType.JSON)
                .body(assignment("MWH.001", 1, 9999))
                .post(BASE_PATH).then().statusCode(404);
    }

    @Test
    public void testRejectsDuplicateAssignment() {
        createAssignment("MWH.001", 1, 1);

        given()
                .contentType(ContentType.JSON)
                .body(assignment("MWH.001", 1, 1))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(409)
                .body("error", containsString("already exists"));
    }

    @Test
    public void testMaxTwoWarehousesPerProductPerStore() {
        createAssignment("MWH.001", 1, 1);
        createAssignment("MWH.012", 1, 1);

        given()
                .contentType(ContentType.JSON)
                .body(assignment("MWH.023", 1, 1))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(422)
                .body("error", containsString("Maximum reached"));
    }

    @Test
    public void testDifferentProductAtSameStoreBypassesWarehouseLimit() {
        createAssignment("MWH.001", 1, 1);
        createAssignment("MWH.012", 1, 1);

        given()
                .contentType(ContentType.JSON)
                .body(assignment("MWH.023", 2, 1))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201);
    }

    @Test
    public void testMaxThreeWarehousesPerStore() {
        createAssignment("MWH.001", 1, 1);
        createAssignment("MWH.012", 2, 1);
        createAssignment("MWH.023", 3, 1);

        given()
                .contentType(ContentType.JSON)
                .body(assignment("MWH.001", 2, 1))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201);
    }

    @Test
    public void testSameWarehouseProductDifferentStoreAllowed() {
        createAssignment("MWH.012", 1, 1);

        given()
                .contentType(ContentType.JSON)
                .body(assignment("MWH.012", 1, 2))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201);
    }

    @Test
    public void testDeleteAssignment() {
        int id = createAssignment("MWH.001", 1, 1);

        given().when().delete(BASE_PATH + "/" + id).then().statusCode(204);

        given()
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    public void testDeleteNonExistentAssignment() {
        given().when().delete(BASE_PATH + "/9999").then().statusCode(404);
    }

    private void restoreSeedData() {
        em.createNativeQuery("DELETE FROM warehouse").executeUpdate();
        em.createNativeQuery("INSERT INTO warehouse(id, businessUnitCode, location, capacity, stock, createdAt) VALUES (1, 'MWH.001', 'ZWOLLE-001', 100, 10, CURRENT_TIMESTAMP)").executeUpdate();
        em.createNativeQuery("INSERT INTO warehouse(id, businessUnitCode, location, capacity, stock, createdAt) VALUES (2, 'MWH.012', 'AMSTERDAM-001', 50, 5, CURRENT_TIMESTAMP)").executeUpdate();
        em.createNativeQuery("INSERT INTO warehouse(id, businessUnitCode, location, capacity, stock, createdAt) VALUES (3, 'MWH.023', 'TILBURG-001', 30, 27, CURRENT_TIMESTAMP)").executeUpdate();
        em.createNativeQuery("ALTER SEQUENCE warehouse_seq RESTART WITH 4").executeUpdate();

        em.createNativeQuery("DELETE FROM product").executeUpdate();
        em.createNativeQuery("INSERT INTO product(id, name, stock) VALUES (1, 'TONSTAD', 10)").executeUpdate();
        em.createNativeQuery("INSERT INTO product(id, name, stock) VALUES (2, 'KALLAX', 5)").executeUpdate();
        em.createNativeQuery("INSERT INTO product(id, name, stock) VALUES (3, 'BESTÅ', 3)").executeUpdate();
        em.createNativeQuery("ALTER SEQUENCE product_seq RESTART WITH 4").executeUpdate();

        em.createNativeQuery("DELETE FROM store").executeUpdate();
        em.createNativeQuery("INSERT INTO store(id, name, quantityProductsInStock) VALUES (1, 'TONSTAD', 10)").executeUpdate();
        em.createNativeQuery("INSERT INTO store(id, name, quantityProductsInStock) VALUES (2, 'KALLAX', 5)").executeUpdate();
        em.createNativeQuery("INSERT INTO store(id, name, quantityProductsInStock) VALUES (3, 'BESTÅ', 3)").executeUpdate();
        em.createNativeQuery("ALTER SEQUENCE store_seq RESTART WITH 4").executeUpdate();

        em.flush();
        em.clear();
    }

    private int createAssignment(String warehouseCode, long productId, long storeId) {
        return given()
                .contentType(ContentType.JSON)
                .body(assignment(warehouseCode, productId, storeId))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    private static String assignment(String warehouseCode, long productId, long storeId) {
        return String.format(
                "{\"warehouseBusinessUnitCode\": \"%s\", \"productId\": %d, \"storeId\": %d}",
                warehouseCode, productId, storeId);
    }
}
