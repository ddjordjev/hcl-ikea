package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ProductEndpointIT {

    private static final String PATH = "product";

    @Inject
    ProductRepository productRepository;

    @BeforeEach
    @Transactional
    void cleanUp() {
        productRepository.deleteAll();
    }

    @Test
    public void testListProducts() {
        createProduct("TONSTAD");
        createProduct("KALLAX");

        given()
                .when()
                .get(PATH)
                .then()
                .statusCode(200)
                .body(containsString("TONSTAD"), containsString("KALLAX"));
    }

    @Test
    public void testGetSingleProduct() {
        int id = createProduct("BESTA");

        given()
                .when()
                .get(PATH + "/" + id)
                .then()
                .statusCode(200)
                .body("name", equalTo("BESTA"));
    }

    @Test
    public void testGetNonExistentProduct() {
        given().when().get(PATH + "/99999").then().statusCode(404);
    }

    @Test
    public void testUpdateProduct() {
        int id = createProduct("HEMNES");

        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"HEMNES-V2\", \"stock\": 20}")
                .when()
                .put(PATH + "/" + id)
                .then()
                .statusCode(200)
                .body("name", equalTo("HEMNES-V2"));
    }

    @Test
    public void testDeleteProduct() {
        int id = createProduct("TONSTAD");
        createProduct("KALLAX");

        given().when().delete(PATH + "/" + id).then().statusCode(204);

        given()
                .when()
                .get(PATH)
                .then()
                .statusCode(200)
                .body(not(containsString("TONSTAD")), containsString("KALLAX"));
    }

    @Test
    public void testDeleteNonExistentProduct() {
        given().when().delete(PATH + "/99999").then().statusCode(404);
    }

    private int createProduct(String name) {
        return given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"" + name + "\", \"stock\": 10}")
                .when()
                .post(PATH)
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }
}
