package com.iap.automation.clients;

import com.iap.automation.config.ConfigManager;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class TabApiClient {

    private static final Logger logger = LogManager.getLogger(TabApiClient.class);
    private final RequestSpecification requestSpec;

    public TabApiClient(RequestSpecification requestSpec) {
        this.requestSpec = requestSpec;
    }

    /**
     * Fetch all tabs
     */
    public Response fetchAllTabs() {
        logger.info("Fetching all tabs...");
        return given()
                .spec(requestSpec)
                .when()
                .get(ConfigManager.getTabsFetchPath())
                .then()
                .extract().response();
    }

    /**
     * Create a new tab
     */
    public Response createTab(Map<String, Object> tabPayload) {
        logger.info("Creating tab with payload: {}", tabPayload.get("tabName"));
        return given()
                .spec(requestSpec)
                .body(tabPayload)
                .when()
                .post(ConfigManager.getTabBasePath())
                .then()
                .extract().response();
    }

    /**
     * Update an existing tab
     */
    public Response updateTab(int tabId, Map<String, Object> tabPayload) {
        logger.info("Updating tab with ID: {}", tabId);
        return given()
                .spec(requestSpec)
                .body(tabPayload)
                .when()
                .put(ConfigManager.getTabBasePath() + "/" + tabId)
                .then()
                .extract().response();
    }

    /**
     * Delete a tab by ID
     */
    public Response deleteTab(int tabId) {
        logger.info("Deleting tab with ID: {}", tabId);
        return given()
                .spec(requestSpec)
                .when()
                .delete(ConfigManager.getTabBasePath() + "/" + tabId)
                .then()
                .extract().response();
    }

    /**
     * Fetch tab by ID
     */
    public Response fetchTabById(int tabId) {
        logger.info("Fetching tab by ID: {}", tabId);
        return given()
                .spec(requestSpec)
                .when()
                .get(ConfigManager.getTabBasePath() + "/" + tabId)
                .then()
                .extract().response();
    }
}
