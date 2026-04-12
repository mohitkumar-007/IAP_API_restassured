package com.iap.automation.clients;

import com.iap.automation.config.ConfigManager;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class PassApiClient {

    private static final Logger logger = LogManager.getLogger(PassApiClient.class);
    private final RequestSpecification requestSpec;

    public PassApiClient(RequestSpecification requestSpec) {
        this.requestSpec = requestSpec;
    }

    /**
     * Create a new pass
     */
    public Response createPass(Map<String, Object> passPayload) {
        logger.info("Creating pass: {}", passPayload.get("name"));
        return given()
                .spec(requestSpec)
                .body(passPayload)
                .when()
                .post(ConfigManager.getPassBasePath())
                .then()
                .extract().response();
    }

    /**
     * Update an existing pass by passId
     */
    public Response updatePass(int passId, Map<String, Object> passPayload) {
        logger.info("Updating pass with ID: {}", passId);
        return given()
                .spec(requestSpec)
                .body(passPayload)
                .when()
                .put(ConfigManager.getPassBasePath() + "/" + passId)
                .then()
                .extract().response();
    }

    /**
     * Delete a pass by passId
     */
    public Response deletePass(int passId) {
        logger.info("Deleting pass with ID: {}", passId);
        return given()
                .spec(requestSpec)
                .when()
                .delete(ConfigManager.getPassBasePath() + "/" + passId)
                .then()
                .extract().response();
    }

    /**
     * Fetch pass by passId
     */
    public Response fetchPassById(int passId) {
        logger.info("Fetching pass by ID: {}", passId);
        return given()
                .spec(requestSpec)
                .when()
                .get(ConfigManager.getPassBasePath() + "/" + passId)
                .then()
                .extract().response();
    }

    /**
     * Fetch all passes by tabId
     */
    public Response fetchAllPassesByTabId(int tabId) {
        logger.info("Fetching all passes for tab ID: {}", tabId);
        return given()
                .spec(requestSpec)
                .when()
                .get(ConfigManager.getPassBasePath() + "/" + tabId)
                .then()
                .extract().response();
    }
}
