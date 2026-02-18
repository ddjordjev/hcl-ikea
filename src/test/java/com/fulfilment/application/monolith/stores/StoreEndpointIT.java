package com.fulfilment.application.monolith.stores;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

@QuarkusTest
public class StoreEndpointIT {

    private static final String PATH = "store";

    @BeforeEach
    @Transactional
    void cleanUp() {
        Store.deleteAll();
    }

    @Test
    public void testCreateAndList() {
        createStore("Malmö Central", 42);
        createStore("Gothenburg East", 18);

        given().when()
                .get(PATH)
                .then()
                .statusCode(200)
                .body(containsString("Malmö Central"), containsString("Gothenburg East"));
    }

    @Test
    public void testGetSingle() {
        int id = createStore("Uppsala Nord", 7);

        given().when()
                .get(PATH + "/" + id)
                .then()
                .statusCode(200)
                .body("name", equalTo("Uppsala Nord"));
    }

    @Test
    public void testGetNonExistent() {
        given().when().get(PATH + "/88888").then().statusCode(404);
    }

    @Test
    public void testUpdateStore() {
        int id = createStore("Linköping", 10);

        given().contentType(ContentType.JSON)
                .body("{\"name\": \"Linköping Renovated\", \"quantityProductsInStock\": 25}")
                .when()
                .put(PATH + "/" + id)
                .then()
                .statusCode(200)
                .body("name", equalTo("Linköping Renovated"))
                .body("quantityProductsInStock", equalTo(25));
    }

    @Test
    public void testPatchOnlyUpdatesProvidedFields() {
        int id = createStore("Norrköping", 15);

        given().contentType(ContentType.JSON)
                .body("{\"quantityProductsInStock\": 99}")
                .when()
                .patch(PATH + "/" + id)
                .then()
                .statusCode(200)
                .body("name", equalTo("Norrköping"))
                .body("quantityProductsInStock", equalTo(99));
    }

    @Test
    public void testDeleteStore() {
        int id = createStore("Västerås", 5);
        createStore("Örebro", 12);

        given().when().delete(PATH + "/" + id).then().statusCode(204);

        given().when()
                .get(PATH)
                .then()
                .statusCode(200)
                .body(not(containsString("Västerås")), containsString("Örebro"));
    }

    @Test
    public void testDeleteNonExistent() {
        given().when().delete(PATH + "/88888").then().statusCode(404);
    }

    private int createStore(String name, int stock) {
        return given().contentType(ContentType.JSON)
                .body("{\"name\": \"" + name + "\", \"quantityProductsInStock\": " + stock + "}")
                .when()
                .post(PATH)
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }
}
