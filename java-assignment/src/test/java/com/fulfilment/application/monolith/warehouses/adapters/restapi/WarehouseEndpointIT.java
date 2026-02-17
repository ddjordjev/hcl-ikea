package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusIntegrationTest
public class WarehouseEndpointIT {

    private static final String PATH = "warehouse";

    @Test
    public void testListWarehouses() {
        createWarehouse("ACME-01", "EINDHOVEN-001", 20, 5);
        createWarehouse("ACME-02", "EINDHOVEN-001", 20, 3);

        given()
                .when()
                .get(PATH)
                .then()
                .statusCode(200)
                .body(
                        containsString("ACME-01"),
                        containsString("ACME-02"));
    }

    @Test
    public void testCreateReturnsWarehouse() {
        given()
                .contentType(ContentType.JSON)
                .body(warehouseJson("NEW.100", "AMSTERDAM-001", 10, 3))
                .when()
                .post(PATH)
                .then()
                .statusCode(200)
                .body("businessUnitCode", equalTo("NEW.100"))
                .body("location", equalTo("AMSTERDAM-001"));
    }

    @Test
    public void testGetWarehouseById() {
        createWarehouse("FETCH-01", "AMSTERDAM-001", 10, 2);

        given()
                .when()
                .get(PATH + "/FETCH-01")
                .then()
                .statusCode(200)
                .body("businessUnitCode", equalTo("FETCH-01"));
    }

    @Test
    public void testGetNonExistentWarehouse() {
        given().when().get(PATH + "/DOES-NOT-EXIST").then().statusCode(404);
    }

    @Test
    public void testCreateDuplicateBusinessUnitCode() {
        createWarehouse("DUP.001", "AMSTERDAM-002", 10, 2);

        given()
                .contentType(ContentType.JSON)
                .body(warehouseJson("DUP.001", "AMSTERDAM-002", 10, 2))
                .when()
                .post(PATH)
                .then()
                .statusCode(409);
    }

    @Test
    public void testCreateWithInvalidLocation() {
        given()
                .contentType(ContentType.JSON)
                .body(warehouseJson("BADLOC-1", "NARNIA-001", 20, 5))
                .when()
                .post(PATH)
                .then()
                .statusCode(422);
    }

    @Test
    public void testReplaceWarehouse() {
        createWarehouse("RPL.050", "AMSTERDAM-002", 10, 3);

        given()
                .contentType(ContentType.JSON)
                .body(warehouseJson("RPL.050", "AMSTERDAM-002", 20, 3))
                .when()
                .post(PATH + "/RPL.050/replacement")
                .then()
                .statusCode(200)
                .body("capacity", equalTo(20))
                .body("stock", equalTo(3));
    }

    @Test
    public void testReplaceRejectsStockMismatch() {
        createWarehouse("RPL.051", "AMSTERDAM-002", 10, 3);

        given()
                .contentType(ContentType.JSON)
                .body(warehouseJson("RPL.051", "AMSTERDAM-002", 10, 1))
                .when()
                .post(PATH + "/RPL.051/replacement")
                .then()
                .statusCode(422);
    }

    @Test
    public void testArchiveWarehouse() {
        createWarehouse("TEMP.777", "AMSTERDAM-001", 10, 2);

        given().when().delete(PATH + "/TEMP.777").then().statusCode(204);

        given()
                .when()
                .get(PATH)
                .then()
                .statusCode(200)
                .body(not(containsString("TEMP.777")));
    }

    @Test
    public void testArchiveNonExistentWarehouse() {
        given().when().delete(PATH + "/GHOST-999").then().statusCode(404);
    }

    @Test
    public void testStockCannotExceedCapacity() {
        given()
                .contentType(ContentType.JSON)
                .body(warehouseJson("OVERFLOW-1", "AMSTERDAM-001", 10, 50))
                .when()
                .post(PATH)
                .then()
                .statusCode(422);
    }

    private void createWarehouse(String buCode, String location, int capacity, int stock) {
        given()
                .contentType(ContentType.JSON)
                .body(warehouseJson(buCode, location, capacity, stock))
                .when()
                .post(PATH)
                .then()
                .statusCode(200);
    }

    private static String warehouseJson(String buCode, String location, int capacity, int stock) {
        return String.format(
                "{\"businessUnitCode\": \"%s\", \"location\": \"%s\", \"capacity\": %d, \"stock\": %d}",
                buCode, location, capacity, stock);
    }
}
