package com.iap.automation.base;

import com.iap.automation.config.ConfigManager;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.BeforeClass;

public class BaseTest {

    protected static final Logger logger = LogManager.getLogger(BaseTest.class);
    protected RequestSpecification requestSpec;

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = ConfigManager.getBaseUrl();

        requestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .addHeader("Authorization", "Bearer " + ConfigManager.getAuthToken())
                .addFilter(new AllureRestAssured())
                .log(LogDetail.ALL)
                .build();

        logger.info("Base URI set to: {}", ConfigManager.getBaseUrl());
    }
}
