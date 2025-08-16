package com.meli;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Integration test for InventoryCommandResource reserve endpoint.
 */
@QuarkusTest
public class InventoryCommandResourceIT {

    @Test
    void reserveEndpoint() {
        given()
            .contentType(ContentType.JSON)
            .header("Idempotency-Key", "it-1")
            .body("{\"skuId\":\"SKU123\",\"quantity\":1}")
        .when().post("/v1/inventory/reserve")
        .then()
            .statusCode(201)
            .body("reserved", equalTo(1));
    }

    @Test
    void idempotentRetryReturnsStoredResponse() {
        String body = "{\"skuId\":\"SKU123\",\"quantity\":1}";
        given()
            .contentType(ContentType.JSON)
            .header("Idempotency-Key", "it-2")
            .body(body)
        .when().post("/v1/inventory/reserve")
        .then().statusCode(201);

        // same key and body should return cached response
        given()
            .contentType(ContentType.JSON)
            .header("Idempotency-Key", "it-2")
            .body(body)
        .when().post("/v1/inventory/reserve")
        .then().statusCode(201)
            .body("reserved", equalTo(1));
    }

    @Test
    void idempotentKeyWithDifferentPayloadFails() {
        given()
            .contentType(ContentType.JSON)
            .header("Idempotency-Key", "it-3")
            .body("{\"skuId\":\"SKU123\",\"quantity\":1}")
        .when().post("/v1/inventory/reserve")
        .then().statusCode(201);

        // different body with same key should return 409
        given()
            .contentType(ContentType.JSON)
            .header("Idempotency-Key", "it-3")
            .body("{\"skuId\":\"SKU123\",\"quantity\":2}")
        .when().post("/v1/inventory/reserve")
        .then().statusCode(409);
    }
}
