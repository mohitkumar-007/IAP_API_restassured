package com.iap.automation.utils;

import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class for common response assertions and extractions.
 */
public class ResponseHelper {

    private static final Logger logger = LogManager.getLogger(ResponseHelper.class);

    public static void logResponse(String operation, Response response) {
        logger.info("=== {} ===", operation);
        logger.info("Status Code: {}", response.getStatusCode());
        logger.info("Response Body: {}", response.getBody().asString());
    }

    public static int extractIntField(Response response, String jsonPath) {
        return response.jsonPath().getInt(jsonPath);
    }

    public static String extractStringField(Response response, String jsonPath) {
        return response.jsonPath().getString(jsonPath);
    }
}
