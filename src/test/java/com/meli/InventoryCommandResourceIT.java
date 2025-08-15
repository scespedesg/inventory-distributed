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
}
