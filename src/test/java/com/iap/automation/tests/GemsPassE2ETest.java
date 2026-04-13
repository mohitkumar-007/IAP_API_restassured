package com.iap.automation.tests;

import com.iap.automation.base.BaseTest;
import com.iap.automation.clients.PassApiClient;
import com.iap.automation.clients.TabApiClient;
import com.iap.automation.config.ConfigManager;
import com.iap.automation.payloads.PassPayloadBuilder;
import com.iap.automation.payloads.TabPayloadBuilder;
import com.iap.automation.report.TestReportGenerator;
import com.iap.automation.utils.ResponseHelper;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * End-to-End API Chaining Test for IAP Gems Passes.
 *
 * TC1: Check if a GEMS tab exists — display info if none found
 * TC2: If no GEMS tab, create one with name "Gems Pack via API"
 * TC3: Fetch the GEMS tab by tabId and print its details
 * TC4: Attempt duplicate GEMS tab creation — expect 400 error
 * TC5: Create Gems Passes (as per PASS_CONFIGS) with unique names
 * TC6: Fetch passes — individually by passId and fetch all by tabId
 * TC7: Update pass properties (price increase)
 * TC8: Re-fetch passes and validate updates took effect
 * TC10: Disable passSaleEnabled on all passes (hide them)
 * TC11: Fetch and verify passSaleEnabled=false on all passes
 * TC12: Re-enable passSaleEnabled on all passes (make visible)
 * TC13: Re-fetch passes and validate passSaleEnabled=true + all properties
 * TC14: Generate execution report
 */
@Epic("IAP Subscription Admin")
@Feature("Gems Pack E2E Chaining")
public class GemsPassE2ETest extends BaseTest {

    private TabApiClient tabClient;
    private PassApiClient passClient;

    // ---- Chained state ----
    private boolean gemsTabExistedBefore = false;
    private int gemsTabId = -1;
    private String gemsTabMongoId;

    private final List<Integer> createdPassIds = new ArrayList<>();
    private final List<String> createdPassMongoIds = new ArrayList<>();
    private final List<String> createdPassNames = new ArrayList<>();
    private final List<Integer> createdPassPrices = new ArrayList<>();

    // ---- Report data ----
    private final TestReportGenerator report = new TestReportGenerator("Gems Pack E2E Test Report");

    // Pass configs: displayName, grossPrice, gemsAmount, initialAmount, passImageUrl, bestValueTagText, billingProductId
    private static final Object[][] PASS_CONFIGS = {
            {"Gems Pack 75", 50,  1000,  500, "https://rummy-assets.jungleerummy.com/Content/versioned/2.0.0.1/images/version4/promotion_november_25/zenrik_images/image-%2816%29-17641481189062.webp?v=1764148119", "", "coins_1"},
            {"Gems Pack 125", 100, 2000, 1000, "https://rummy-assets.jungleerummy.com/Content/versioned/2.0.0.1/images/version4/promotion_november_25/zenrik_images/image-%2815%29-17641480806403.webp?v=1764148081", "", "coins_2"},
            {"Gems Pack 225", 200, 3000, 1500, "https://rummy-assets.jungleerummy.com/Content/versioned/2.0.0.1/images/version4/promotion_november_25/zenrik_images/image-%2814%29-17641480323971.webp?v=1764148032", "Most Popular", "coins_3"},
            {"Gems Pack 525", 500, 5000, 2500, "https://rummy-assets.jungleerummy.com/Content/versioned/2.0.0.1/images/version4/promotion_november_25/zenrik_images/image-%2811%29-17641479985932.webp?v=1764147999", "Hot Deal", "coins_4"}
    };

    private static final int PRICE_UPDATE_INCREASE = 25;

    @BeforeClass
    @Override
    public void setup() {
        super.setup();
        tabClient = new TabApiClient(requestSpec);
        passClient = new PassApiClient(requestSpec);
        logger.info("========================================================");
        logger.info("  GEMS PACK E2E CHAINING TEST — EXECUTION STARTED");
        logger.info("  Base URL: {}", ConfigManager.getBaseUrl());
        logger.info("========================================================");
    }

    // ========================================================================
    // TC1: Check if any GEMS tab exists — display info
    // ========================================================================

    @Test(priority = 1)
    @Story("Tab Discovery")
    @Description("TC1: Check if a tab with type 'gems' exists. Display information accordingly.")
    @Severity(SeverityLevel.BLOCKER)
    public void TC1_CheckIfGemsTabExists() {
        logger.info("===== TC1: Check if GEMS tab exists =====");

        Response fetchResponse = tabClient.fetchAllTabs();
        ResponseHelper.logResponse("Fetch All Tabs", fetchResponse);
        Assert.assertEquals(fetchResponse.getStatusCode(), 200, "Fetch all tabs should return 200");

        List<Map<String, Object>> tabs = fetchResponse.jsonPath().getList("$");

        if (tabs != null) {
            for (Map<String, Object> tab : tabs) {
                String tabType = (String) tab.get("tabType");
                if ("gems".equalsIgnoreCase(tabType)) {
                    gemsTabExistedBefore = true;
                    gemsTabId = (Integer) tab.get("tabId");
                    gemsTabMongoId = (String) tab.get("id");
                    break;
                }
            }
        }

        if (gemsTabExistedBefore) {
            logger.info(">> GEMS tab FOUND — tabId={}, mongoId={}", gemsTabId, gemsTabMongoId);
            report.addStep("TC1", "Check GEMS Tab Exists", "PASS",
                    "<span class='detail-label'>GEMS tab found</span><br>Tab ID: <b>" + gemsTabId + "</b>, Mongo ID: " + gemsTabMongoId);
        } else {
            logger.info(">> =====================================================");
            logger.info(">> NO GEMS TAB EXISTS!");
            logger.info(">> Total tabs returned: {}", (tabs != null ? tabs.size() : 0));
            logger.info(">> A new GEMS tab will be created in TC2.");
            logger.info(">> =====================================================");
            report.addStep("TC1", "Check GEMS Tab Exists", "INFO",
                    "No GEMS tab found. Total tabs in system: " + (tabs != null ? tabs.size() : 0) + ". A new tab will be created in TC2.");
        }
    }

    // ========================================================================
    // TC2: Create GEMS tab if it doesn't exist
    // ========================================================================

    @Test(priority = 2, dependsOnMethods = "TC1_CheckIfGemsTabExists")
    @Story("Tab Creation")
    @Description("TC2: If no GEMS tab exists, create one named 'Gems Pack'.")
    @Severity(SeverityLevel.BLOCKER)
    public void TC2_CreateGemsTabIfNotExists() {
        logger.info("===== TC2: Create GEMS tab if not exists =====");

        if (gemsTabExistedBefore) {
            logger.info(">> GEMS tab already exists (tabId={}). Skipping creation.", gemsTabId);
            report.addStep("TC2", "Create GEMS Tab", "SKIP",
                    "Tab already existed — <b>tabId=" + gemsTabId + "</b>. Creation skipped.");
            return;
        }

        String tabName = "Gems Pack via API";
        Map<String, Object> tabPayload = TabPayloadBuilder.buildGemsTabPayload(tabName, ConfigManager.getProductId());

        Response createResponse = tabClient.createTab(tabPayload);
        ResponseHelper.logResponse("Create GEMS Tab", createResponse);

        Assert.assertEquals(createResponse.getStatusCode(), 200,
                "Tab creation should return 200. Body: " + createResponse.getBody().asString());

        // Try extracting tabId directly from the create response
        Integer directTabId = createResponse.jsonPath().get("tabId");
        String directMongoId = createResponse.jsonPath().getString("id");

        if (directTabId != null && directTabId > 0) {
            gemsTabId = directTabId;
            gemsTabMongoId = directMongoId;
            logger.info(">> Tab ID extracted directly from create response: tabId={}", gemsTabId);
        } else {
            // Fallback: re-fetch all tabs to find the newly created gems tab
            logger.info(">> tabId not in create response. Re-fetching tabs to find the new GEMS tab...");
            Response refetch = tabClient.fetchAllTabs();
            Assert.assertEquals(refetch.getStatusCode(), 200, "Re-fetch tabs should return 200");

            List<Map<String, Object>> allTabs = refetch.jsonPath().getList("$");
            boolean found = false;
            if (allTabs != null) {
                for (Map<String, Object> tab : allTabs) {
                    if ("gems".equalsIgnoreCase((String) tab.get("tabType"))) {
                        gemsTabId = (Integer) tab.get("tabId");
                        gemsTabMongoId = (String) tab.get("id");
                        found = true;
                        break;
                    }
                }
            }
            Assert.assertTrue(found, "Newly created GEMS tab should appear in fetch-all response");
            logger.info(">> Tab ID extracted from re-fetch: tabId={}", gemsTabId);
        }

        logger.info(">> GEMS tab CREATED — tabId={}, mongoId={}, tabName='{}'", gemsTabId, gemsTabMongoId, tabName);
        Assert.assertTrue(gemsTabId > 0, "Created tabId should be positive");

        report.addStep("TC2", "Create GEMS Tab", "PASS",
                "<span class='detail-label'>Tab Created Successfully</span><br>"
                + "Tab Name: <b>" + tabName + "</b><br>"
                + "Tab ID: <b>" + gemsTabId + "</b><br>"
                + "Tab Type: <b>gems</b><br>"
                + "Mongo ID: " + gemsTabMongoId);
    }

    // ========================================================================
    // TC3: Fetch GEMS tab by ID and print details
    // ========================================================================

    @Test(priority = 3, dependsOnMethods = "TC2_CreateGemsTabIfNotExists")
    @Story("Tab Verification")
    @Description("TC3: Fetch the GEMS tab by tabId and print its details.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC3_FetchGemsTabById() {
        logger.info("===== TC3: Fetch GEMS tab by tabId={} =====", gemsTabId);

        Response fetchResponse = tabClient.fetchTabById(gemsTabId);
        ResponseHelper.logResponse("Fetch GEMS Tab by ID=" + gemsTabId, fetchResponse);
        Assert.assertEquals(fetchResponse.getStatusCode(), 200,
                "Fetch tab by ID should return 200. Body: " + fetchResponse.getBody().asString());

        int fetchedTabId = fetchResponse.jsonPath().getInt("tabId");
        String fetchedTabName = fetchResponse.jsonPath().getString("tabName");
        String fetchedTabType = fetchResponse.jsonPath().getString("tabType");

        Assert.assertEquals(fetchedTabId, gemsTabId, "Fetched tabId should match");
        Assert.assertEquals(fetchedTabType, "gems", "Tab type should be gems");

        logger.info(">> GEMS Tab fetched — tabId={}, tabName='{}', tabType='{}'",
                fetchedTabId, fetchedTabName, fetchedTabType);

        report.addStep("TC3", "Fetch GEMS Tab by ID", "PASS",
                "<span class='detail-label'>Tab Fetched Successfully</span><br>"
                + "Tab ID: <b>" + fetchedTabId + "</b><br>"
                + "Tab Name: <b>" + fetchedTabName + "</b><br>"
                + "Tab Type: <b>" + fetchedTabType + "</b>");
    }

    // ========================================================================
    // TC4: Attempt duplicate GEMS tab creation — expect 400 error
    // ========================================================================

    @Test(priority = 4, dependsOnMethods = "TC3_FetchGemsTabById")
    @Story("Tab Creation — Negative")
    @Description("TC4: Attempt to create a GEMS tab again. Expect 400 with duplicate error message.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC4_DuplicateGemsTabCreationShouldFail() {
        logger.info("===== TC4: Attempt duplicate GEMS tab creation — expect 400 =====");

        String tabName = "Gems Pack via API";
        Map<String, Object> tabPayload = TabPayloadBuilder.buildGemsTabPayload(tabName, ConfigManager.getProductId());

        Response createResponse = tabClient.createTab(tabPayload);
        ResponseHelper.logResponse("Duplicate GEMS Tab Creation Attempt", createResponse);

        Assert.assertEquals(createResponse.getStatusCode(), 400,
                "Duplicate tab creation should return 400. Body: " + createResponse.getBody().asString());

        String message = createResponse.jsonPath().getString("message");
        String expectedMessage = "Tab with type gems and product RUMMY already exists";

        Assert.assertNotNull(message, "Response should contain a 'message' field");
        Assert.assertEquals(message, expectedMessage,
                "Error message should indicate duplicate tab. Actual: " + message);

        logger.info(">> Duplicate creation correctly rejected — status=400, message='{}'", message);
        report.addStep("TC4", "Duplicate GEMS Tab Creation", "PASS",
                "<span class='detail-label'>Duplicate Rejected (Expected)</span><br>"
                + "HTTP Status: <b>400</b><br>"
                + "Error Message: <b>" + message + "</b>");
    }

    // ========================================================================
    // TC5: Create Gems Passes with unique names
    // ========================================================================

    @Test(priority = 5, dependsOnMethods = "TC4_DuplicateGemsTabCreationShouldFail")
    @Story("Pass Creation")
    @Description("TC5: Create Gems passes under the GEMS tab. Skips if passes already exist on backend.")
    @Severity(SeverityLevel.BLOCKER)
    public void TC5_Create4GemsPasses() {
        logger.info("===== TC5: Create {} Gems Passes under tabId={} =====", PASS_CONFIGS.length, gemsTabId);

        // Guard: skip if passes were already created in this test run
        if (!createdPassIds.isEmpty()) {
            logger.info(">> Passes already created in this run. Skipping. passIds={}", createdPassIds);
            report.addStep("TC5", "Skip — Already Created", "SKIP", "passIds=" + createdPassIds);
            return;
        }

        // Scan backend for existing passes belonging to this tab
        logger.info(">> Scanning for existing passes under tabId={}...", gemsTabId);
        List<Integer> existingPassIds = new ArrayList<>();
        List<String> existingMongoIds = new ArrayList<>();
        List<String> existingNames = new ArrayList<>();
        List<Integer> existingPrices = new ArrayList<>();

        int consecutiveMisses = 0;
        for (int probeId = gemsTabId + 1; consecutiveMisses < 5; probeId++) {
            Response resp = passClient.fetchPassById(probeId);
            if (resp.getStatusCode() == 200 && resp.jsonPath().getInt("tabId") == gemsTabId) {
                existingPassIds.add(resp.jsonPath().getInt("passId"));
                existingMongoIds.add(resp.jsonPath().getString("id"));
                existingNames.add(resp.jsonPath().getString("name"));
                existingPrices.add(resp.jsonPath().getInt("grossPrice"));
                consecutiveMisses = 0;
            } else {
                consecutiveMisses++;
            }
        }

        if (existingPassIds.size() >= PASS_CONFIGS.length) {
            logger.info(">> Found {} existing passes for tabId={}. Reusing first {} — skipping creation.",
                    existingPassIds.size(), gemsTabId, PASS_CONFIGS.length);
            for (int i = 0; i < PASS_CONFIGS.length; i++) {
                createdPassIds.add(existingPassIds.get(i));
                createdPassMongoIds.add(existingMongoIds.get(i));
                createdPassNames.add(existingNames.get(i));
                createdPassPrices.add(existingPrices.get(i));
            }
            logger.info(">> Reusing passIds={}, prices={}", createdPassIds, createdPassPrices);
            List<String[]> reuseRows = new ArrayList<>();
            for (int i = 0; i < createdPassIds.size(); i++) {
                String tag = (String) PASS_CONFIGS[i][5];
                reuseRows.add(new String[]{createdPassNames.get(i), String.valueOf(createdPassIds.get(i)),
                        String.valueOf(createdPassPrices.get(i)),
                        tag.isEmpty() ? "(none)" : tag, (String) PASS_CONFIGS[i][6]});
            }
            report.addStep("TC5", "Reuse Existing Passes", "PASS",
                    "<span class='detail-label'>Found " + createdPassIds.size() + " existing passes on backend — reusing</span>"
                    + htmlTable(new String[]{"Pass Name", "Pass ID", "Price", "Tag Text", "Billing ID"}, reuseRows));
            return;
        }

        logger.info(">> No existing passes found. Creating {} new passes...", PASS_CONFIGS.length);

        for (int i = 0; i < PASS_CONFIGS.length; i++) {
            String displayName = (String) PASS_CONFIGS[i][0];
            int grossPrice = (int) PASS_CONFIGS[i][1];
            int gemsAmount = (int) PASS_CONFIGS[i][2];
            int initialAmount = (int) PASS_CONFIGS[i][3];
            String passImageUrl = (String) PASS_CONFIGS[i][4];
            String bestValueTagText = (String) PASS_CONFIGS[i][5];
            String billingProductId = (String) PASS_CONFIGS[i][6];
            int rank = i + 1;

            String uniqueName = PassPayloadBuilder.generateUniquePassName(displayName);

            Map<String, Object> payload = PassPayloadBuilder.buildGemsPassPayload(
                    uniqueName, gemsTabId, rank, grossPrice, gemsAmount, initialAmount, passImageUrl, bestValueTagText, billingProductId);

            Response response = passClient.createPass(payload);
            ResponseHelper.logResponse("Create Pass #" + rank + " [" + uniqueName + "]", response);

            Assert.assertEquals(response.getStatusCode(), 200,
                    "Pass #" + rank + " creation failed. Body: " + response.getBody().asString());

            int passId = response.jsonPath().getInt("pass.passId");
            String mongoId = response.jsonPath().getString("pass.id");

            createdPassIds.add(passId);
            createdPassMongoIds.add(mongoId);
            createdPassNames.add(uniqueName);
            createdPassPrices.add(grossPrice);

            logger.info(">> Pass #{} CREATED — name='{}', passId={}, grossPrice={}",
                    rank, uniqueName, passId, grossPrice);

            // Brief pause to guarantee unique timestamps
            try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        Assert.assertEquals(createdPassIds.size(), PASS_CONFIGS.length,
                "Must have created exactly " + PASS_CONFIGS.length + " passes");
        logger.info(">> All {} passes created: {}", PASS_CONFIGS.length, createdPassIds);

        // Add consolidated HTML table to report
        List<String[]> tc5Rows = new ArrayList<>();
        for (int i = 0; i < createdPassIds.size(); i++) {
            String tag = (String) PASS_CONFIGS[i][5];
            tc5Rows.add(new String[]{createdPassNames.get(i), String.valueOf(createdPassIds.get(i)),
                    String.valueOf(createdPassPrices.get(i)),
                    tag.isEmpty() ? "(none)" : tag, (String) PASS_CONFIGS[i][6]});
        }
        report.addStep("TC5", "Create " + PASS_CONFIGS.length + " Gems Passes", "PASS",
                "<span class='detail-label'>" + createdPassIds.size() + " passes created under tabId=" + gemsTabId + "</span>"
                + htmlTable(new String[]{"Pass Name", "Pass ID", "Price", "Tag Text", "Billing ID"}, tc5Rows));
    }

    // ========================================================================
    // TC6: Fetch passes — by individual passId + fetch all by tabId
    // ========================================================================

    @Test(priority = 6, dependsOnMethods = "TC5_Create4GemsPasses")
    @Story("Pass Fetch")
    @Description("TC6: Fetch all gems passes by tabId and print in tabular form.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC6_FetchGemsPassesAfterCreation() {
        logger.info("===== TC6: Fetch All Gems Passes for tabId={} =====", gemsTabId);

        // Fetch each pass individually to verify
        for (int i = 0; i < createdPassIds.size(); i++) {
            int passId = createdPassIds.get(i);
            Response resp = passClient.fetchPassById(passId);
            Assert.assertEquals(resp.getStatusCode(), 200, "Fetch pass " + passId + " should return 200");
            Assert.assertEquals(resp.jsonPath().getInt("passId"), passId);
            Assert.assertEquals(resp.jsonPath().getInt("tabId"), gemsTabId);
        }

        // Print tabular summary
        logger.info(">> ============================================================================================================================");
        logger.info(">> GEMS PASSES UNDER tabId={}", gemsTabId);
        logger.info(">> ----------------------------------------------------------------------------------------------------------------------------");
        logger.info(">> {}", String.format("%-40s | %-10s | %-8s | %-15s | %s", "PASS NAME", "PASS ID", "PRICE", "TAG TEXT", "BILLING ID"));
        logger.info(">> ----------------------------------------------------------------------------------------------------------------------------");
        for (int i = 0; i < createdPassIds.size(); i++) {
            String tagText = (String) PASS_CONFIGS[i][5];
            String billingId = (String) PASS_CONFIGS[i][6];
            logger.info(">> {}", String.format("%-40s | %-10d | %-8d | %-15s | %s",
                    createdPassNames.get(i), createdPassIds.get(i), createdPassPrices.get(i),
                    tagText.isEmpty() ? "(none)" : tagText, billingId));
        }
        logger.info(">> ============================================================================================================================");

        List<String[]> tc6Rows = new ArrayList<>();
        for (int i = 0; i < createdPassIds.size(); i++) {
            String tag = (String) PASS_CONFIGS[i][5];
            tc6Rows.add(new String[]{createdPassNames.get(i), String.valueOf(createdPassIds.get(i)),
                    String.valueOf(createdPassPrices.get(i)),
                    tag.isEmpty() ? "(none)" : tag, (String) PASS_CONFIGS[i][6]});
        }
        report.addStep("TC6", "Fetch All Gems Passes", "PASS",
                "<span class='detail-label'>Fetched " + createdPassIds.size() + " passes for tabId=" + gemsTabId + "</span>"
                + htmlTable(new String[]{"Pass Name", "Pass ID", "Price", "Tag Text", "Billing ID"}, tc6Rows));
    }

    // ========================================================================
    // TC7: Update pass properties (increase price)
    // ========================================================================

    @Test(priority = 7, dependsOnMethods = "TC6_FetchGemsPassesAfterCreation")
    @Story("Pass Update")
    @Description("TC7: Update all passes — increase grossPrice by 25.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC7_UpdateGemsPasses() {
        logger.info("===== TC7: Update all {} passes (price +{}) =====", createdPassIds.size(), PRICE_UPDATE_INCREASE);

        List<String[]> tc7Rows = new ArrayList<>();

        for (int i = 0; i < createdPassIds.size(); i++) {
            int passId = createdPassIds.get(i);
            String mongoId = createdPassMongoIds.get(i);
            String currentName = createdPassNames.get(i);
            int oldPrice = createdPassPrices.get(i);
            int newPrice = oldPrice + PRICE_UPDATE_INCREASE;
            int gemsAmount = (int) PASS_CONFIGS[i][2];
            int initialAmount = (int) PASS_CONFIGS[i][3];
            String passImageUrl = (String) PASS_CONFIGS[i][4];
            String bestValueTagText = (String) PASS_CONFIGS[i][5];
            String billingProductId = (String) PASS_CONFIGS[i][6];
            int rank = i + 1;

            Map<String, Object> updatePayload = PassPayloadBuilder.buildUpdatePassPayload(
                    mongoId, passId, currentName, gemsTabId, rank, newPrice, gemsAmount, initialAmount, passImageUrl, bestValueTagText, billingProductId);

            Response resp = passClient.updatePass(passId, updatePayload);
            ResponseHelper.logResponse("Update Pass passId=" + passId + " price " + oldPrice + " -> " + newPrice, resp);

            Assert.assertEquals(resp.getStatusCode(), 200,
                    "Update pass " + passId + " should return 200. Body: " + resp.getBody().asString());

            // Store updated price
            createdPassPrices.set(i, newPrice);

            logger.info(">> Pass passId={} UPDATED — grossPrice: {} -> {}", passId, oldPrice, newPrice);
            tc7Rows.add(new String[]{currentName, String.valueOf(passId),
                    String.valueOf(oldPrice), String.valueOf(newPrice)});
        }

        report.addStep("TC7", "Update All Passes (Price +" + PRICE_UPDATE_INCREASE + ")", "PASS",
                "<span class='detail-label'>" + createdPassIds.size() + " passes updated with price increase of +" + PRICE_UPDATE_INCREASE + "</span>"
                + htmlTable(new String[]{"Pass Name", "Pass ID", "Old Price", "New Price"}, tc7Rows));
        logger.info(">> All {} passes updated successfully", createdPassIds.size());
    }

    // ========================================================================
    // TC8: Re-fetch passes and validate updates
    // ========================================================================

    @Test(priority = 8, dependsOnMethods = "TC7_UpdateGemsPasses")
    @Story("Pass Update Verification")
    @Description("TC8: Re-fetch all passes, verify updated prices, and print tabular summary.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC8_VerifyUpdatedPasses() {
        logger.info("===== TC8: Verify updated passes for tabId={} =====", gemsTabId);

        List<String> verifiedNames = new ArrayList<>();
        List<Integer> verifiedIds = new ArrayList<>();
        List<Integer> verifiedPrices = new ArrayList<>();

        for (int i = 0; i < createdPassIds.size(); i++) {
            int passId = createdPassIds.get(i);
            int expectedPrice = createdPassPrices.get(i);
            String expectedName = createdPassNames.get(i);

            Response resp = passClient.fetchPassById(passId);
            ResponseHelper.logResponse("Verify Updated Pass passId=" + passId, resp);

            Assert.assertEquals(resp.getStatusCode(), 200);

            int actualPrice = resp.jsonPath().getInt("grossPrice");
            String actualName = resp.jsonPath().getString("name");
            int actualTabId = resp.jsonPath().getInt("tabId");
            String actualTabType = resp.jsonPath().getString("tabType");

            Assert.assertEquals(actualPrice, expectedPrice,
                    "Pass " + passId + ": grossPrice should be " + expectedPrice + " but was " + actualPrice);
            Assert.assertEquals(actualName, expectedName,
                    "Pass " + passId + ": name should be '" + expectedName + "'");
            Assert.assertEquals(actualTabId, gemsTabId, "tabId should match");
            Assert.assertEquals(actualTabType, "gems", "tabType should be gems");

            verifiedNames.add(actualName);
            verifiedIds.add(passId);
            verifiedPrices.add(actualPrice);
        }

        // Print tabular summary of verified updated passes
        logger.info(">> ============================================================================================================================");
        logger.info(">> VERIFIED UPDATED PASSES UNDER tabId={}", gemsTabId);
        logger.info(">> ----------------------------------------------------------------------------------------------------------------------------");
        logger.info(">> {}", String.format("%-40s | %-10s | %-14s | %-15s | %s", "PASS NAME", "PASS ID", "UPDATED PRICE", "TAG TEXT", "BILLING ID"));
        logger.info(">> ----------------------------------------------------------------------------------------------------------------------------");
        List<String[]> tc8Rows = new ArrayList<>();
        for (int i = 0; i < verifiedIds.size(); i++) {
            String tagText = (String) PASS_CONFIGS[i][5];
            String billingId = (String) PASS_CONFIGS[i][6];
            logger.info(">> {}", String.format("%-40s | %-10d | %-14d | %-15s | %s",
                    verifiedNames.get(i), verifiedIds.get(i), verifiedPrices.get(i),
                    tagText.isEmpty() ? "(none)" : tagText, billingId));
            tc8Rows.add(new String[]{verifiedNames.get(i), String.valueOf(verifiedIds.get(i)),
                    String.valueOf(verifiedPrices.get(i)),
                    tagText.isEmpty() ? "(none)" : tagText, billingId});
        }
        logger.info(">> ============================================================================================================================");

        logger.info(">> All {} updated passes verified successfully", createdPassIds.size());
        report.addStep("TC8", "Verify Updated Passes", "PASS",
                "<span class='detail-label'>All " + createdPassIds.size() + " passes verified with correct updated prices</span>"
                + htmlTable(new String[]{"Pass Name", "Pass ID", "Updated Price", "Tag Text", "Billing ID"}, tc8Rows));
    }

    // ========================================================================
    // TC10: Disable passSaleEnabled on all passes
    // ========================================================================

    @Test(priority = 10, dependsOnMethods = "TC8_VerifyUpdatedPasses")
    @Story("Pass Sale Toggle")
    @Description("TC10: Update all passes — set passSaleEnabled=false to hide them.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC10_DisablePassSaleEnabled() {
        logger.info("===== TC10: Disable passSaleEnabled on all {} passes =====", createdPassIds.size());

        List<String[]> tc10Rows = new ArrayList<>();

        for (int i = 0; i < createdPassIds.size(); i++) {
            int passId = createdPassIds.get(i);
            String mongoId = createdPassMongoIds.get(i);
            String currentName = createdPassNames.get(i);
            int currentPrice = createdPassPrices.get(i);
            int gemsAmount = (int) PASS_CONFIGS[i][2];
            int initialAmount = (int) PASS_CONFIGS[i][3];
            String passImageUrl = (String) PASS_CONFIGS[i][4];
            String bestValueTagText = (String) PASS_CONFIGS[i][5];
            String billingProductId = (String) PASS_CONFIGS[i][6];
            int rank = i + 1;

            Map<String, Object> updatePayload = PassPayloadBuilder.buildUpdatePassPayload(
                    mongoId, passId, currentName, gemsTabId, rank, currentPrice, gemsAmount, initialAmount,
                    passImageUrl, bestValueTagText, billingProductId);
            updatePayload.put("passSaleEnabled", false);

            Response resp = passClient.updatePass(passId, updatePayload);
            ResponseHelper.logResponse("Disable passSaleEnabled passId=" + passId, resp);

            Assert.assertEquals(resp.getStatusCode(), 200,
                    "Update pass " + passId + " should return 200. Body: " + resp.getBody().asString());

            logger.info(">> Pass passId={} — passSaleEnabled set to FALSE", passId);
            tc10Rows.add(new String[]{currentName, String.valueOf(passId), "true", "false", "HIDDEN"});
        }

        report.addStep("TC10", "Disable passSaleEnabled on All Passes", "PASS",
                "<span class='detail-label'>All " + createdPassIds.size() + " passes set to passSaleEnabled=false (HIDDEN)</span>"
                + htmlTable(new String[]{"Pass Name", "Pass ID", "Before", "After", "Visibility"}, tc10Rows));
        logger.info(">> All {} passes disabled (passSaleEnabled=false)", createdPassIds.size());
    }

    // ========================================================================
    // TC11: Verify passSaleEnabled is disabled on all passes
    // ========================================================================

    @Test(priority = 11, dependsOnMethods = "TC10_DisablePassSaleEnabled")
    @Story("Pass Sale Toggle Verification")
    @Description("TC11: Fetch all passes and verify passSaleEnabled=false.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC11_VerifyPassSaleDisabled() {
        logger.info("===== TC11: Verify passSaleEnabled=false on all passes =====");

        logger.info(">> =====================================================================================");
        logger.info(">> PASS SALE STATUS (EXPECTED: DISABLED)");
        logger.info(">> -------------------------------------------------------------------------------------");
        logger.info(">> {}", String.format("%-40s | %-10s | %-15s | %s", "PASS NAME", "PASS ID", "SALE ENABLED", "STATUS"));
        logger.info(">> -------------------------------------------------------------------------------------");

        List<String[]> tc11Rows = new ArrayList<>();

        for (int i = 0; i < createdPassIds.size(); i++) {
            int passId = createdPassIds.get(i);
            Response resp = passClient.fetchPassById(passId);
            Assert.assertEquals(resp.getStatusCode(), 200, "Fetch pass " + passId + " should return 200");

            boolean passSaleEnabled = resp.jsonPath().getBoolean("passSaleEnabled");

            Assert.assertFalse(passSaleEnabled,
                    "Pass " + passId + ": passSaleEnabled should be false but was " + passSaleEnabled);

            String statusLabel = passSaleEnabled ? "VISIBLE" : "HIDDEN";
            logger.info(">> {}", String.format("%-40s | %-10d | %-15s | %s",
                    createdPassNames.get(i), passId, passSaleEnabled, statusLabel));

            tc11Rows.add(new String[]{createdPassNames.get(i), String.valueOf(passId),
                    String.valueOf(passSaleEnabled), statusLabel});
        }

        logger.info(">> =====================================================================================");
        logger.info(">> All {} passes verified as HIDDEN (passSaleEnabled=false)", createdPassIds.size());
        report.addStep("TC11", "Verify All Passes HIDDEN", "PASS",
                "<span class='detail-label'>API Proof: All " + createdPassIds.size() + " passes confirmed passSaleEnabled=false</span>"
                + htmlTable(new String[]{"Pass Name", "Pass ID", "passSaleEnabled (API)", "Visibility Status"}, tc11Rows));
    }

    // ========================================================================
    // TC12: Re-enable passSaleEnabled on all passes
    // ========================================================================

    @Test(priority = 12, dependsOnMethods = "TC11_VerifyPassSaleDisabled")
    @Story("Pass Sale Toggle")
    @Description("TC12: Update all passes — set passSaleEnabled=true to make them visible again.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC12_EnablePassSaleEnabled() {
        logger.info("===== TC12: Enable passSaleEnabled on all {} passes =====", createdPassIds.size());

        List<String[]> tc12Rows = new ArrayList<>();

        for (int i = 0; i < createdPassIds.size(); i++) {
            int passId = createdPassIds.get(i);
            String mongoId = createdPassMongoIds.get(i);
            String currentName = createdPassNames.get(i);
            int currentPrice = createdPassPrices.get(i);
            int gemsAmount = (int) PASS_CONFIGS[i][2];
            int initialAmount = (int) PASS_CONFIGS[i][3];
            String passImageUrl = (String) PASS_CONFIGS[i][4];
            String bestValueTagText = (String) PASS_CONFIGS[i][5];
            String billingProductId = (String) PASS_CONFIGS[i][6];
            int rank = i + 1;

            Map<String, Object> updatePayload = PassPayloadBuilder.buildUpdatePassPayload(
                    mongoId, passId, currentName, gemsTabId, rank, currentPrice, gemsAmount, initialAmount,
                    passImageUrl, bestValueTagText, billingProductId);
            updatePayload.put("passSaleEnabled", true);

            Response resp = passClient.updatePass(passId, updatePayload);
            ResponseHelper.logResponse("Enable passSaleEnabled passId=" + passId, resp);

            Assert.assertEquals(resp.getStatusCode(), 200,
                    "Update pass " + passId + " should return 200. Body: " + resp.getBody().asString());

            logger.info(">> Pass passId={} — passSaleEnabled set to TRUE", passId);
            tc12Rows.add(new String[]{currentName, String.valueOf(passId), "false", "true", "VISIBLE"});
        }

        report.addStep("TC12", "Re-enable passSaleEnabled on All Passes", "PASS",
                "<span class='detail-label'>All " + createdPassIds.size() + " passes set to passSaleEnabled=true (VISIBLE)</span>"
                + htmlTable(new String[]{"Pass Name", "Pass ID", "Before", "After", "Visibility"}, tc12Rows));
        logger.info(">> All {} passes re-enabled (passSaleEnabled=true)", createdPassIds.size());
    }

    // ========================================================================
    // TC13: Re-fetch and validate passSaleEnabled is true
    // ========================================================================

    @Test(priority = 13, dependsOnMethods = "TC12_EnablePassSaleEnabled")
    @Story("Pass Sale Toggle Verification")
    @Description("TC13: Re-fetch all passes, verify passSaleEnabled=true and all other properties.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC13_VerifyPassSaleReEnabled() {
        logger.info("===== TC13: Verify passSaleEnabled=true on all passes =====");

        logger.info(">> ============================================================================================================================");
        logger.info(">> VERIFIED RE-ENABLED PASSES UNDER tabId={}", gemsTabId);
        logger.info(">> ----------------------------------------------------------------------------------------------------------------------------");
        logger.info(">> {}", String.format("%-40s | %-10s | %-8s | %-15s | %-12s | %s",
                "PASS NAME", "PASS ID", "PRICE", "TAG TEXT", "SALE ENABLED", "BILLING ID"));
        logger.info(">> ----------------------------------------------------------------------------------------------------------------------------");

        List<String[]> tc13Rows = new ArrayList<>();

        for (int i = 0; i < createdPassIds.size(); i++) {
            int passId = createdPassIds.get(i);
            int expectedPrice = createdPassPrices.get(i);
            String expectedName = createdPassNames.get(i);

            Response resp = passClient.fetchPassById(passId);
            ResponseHelper.logResponse("Verify re-enabled pass passId=" + passId, resp);

            Assert.assertEquals(resp.getStatusCode(), 200);

            int actualPrice = resp.jsonPath().getInt("grossPrice");
            String actualName = resp.jsonPath().getString("name");
            int actualTabId = resp.jsonPath().getInt("tabId");
            boolean passSaleEnabled = resp.jsonPath().getBoolean("passSaleEnabled");

            Assert.assertEquals(actualPrice, expectedPrice,
                    "Pass " + passId + ": grossPrice should be " + expectedPrice);
            Assert.assertEquals(actualName, expectedName,
                    "Pass " + passId + ": name should be '" + expectedName + "'");
            Assert.assertEquals(actualTabId, gemsTabId, "tabId should match");
            Assert.assertTrue(passSaleEnabled,
                    "Pass " + passId + ": passSaleEnabled should be true but was " + passSaleEnabled);

            String tagText = (String) PASS_CONFIGS[i][5];
            String billingId = (String) PASS_CONFIGS[i][6];
            logger.info(">> {}", String.format("%-40s | %-10d | %-8d | %-15s | %-12s | %s",
                    actualName, passId, actualPrice,
                    tagText.isEmpty() ? "(none)" : tagText, passSaleEnabled, billingId));

            tc13Rows.add(new String[]{actualName, String.valueOf(passId), String.valueOf(actualPrice),
                    tagText.isEmpty() ? "(none)" : tagText, String.valueOf(passSaleEnabled), billingId});
        }

        logger.info(">> ============================================================================================================================");
        logger.info(">> All {} passes verified as VISIBLE (passSaleEnabled=true)", createdPassIds.size());
        report.addStep("TC13", "Verify All Passes Re-enabled", "PASS",
                "<span class='detail-label'>API Proof: All " + createdPassIds.size() + " passes confirmed passSaleEnabled=true</span>"
                + htmlTable(new String[]{"Pass Name", "Pass ID", "Price", "Tag Text", "passSaleEnabled (API)", "Billing ID"}, tc13Rows));
    }

    // ========================================================================
    // TC14: Generate Execution Report
    // ========================================================================

    @Test(priority = 14, dependsOnMethods = "TC13_VerifyPassSaleReEnabled", alwaysRun = true)
    @Story("Reporting")
    @Description("TC14: Generate HTML execution report with all test results.")
    @Severity(SeverityLevel.MINOR)
    public void TC14_GenerateExecutionReport() {
        logger.info("===== TC14: Generate Execution Report =====");

        // Add summary data
        report.addSummary("GEMS Tab ID", String.valueOf(gemsTabId));
        report.addSummary("Tab Existed Before Run", String.valueOf(gemsTabExistedBefore));
        report.addSummary("Passes Created", String.valueOf(createdPassIds.size()));
        report.addSummary("Pass IDs", createdPassIds.toString());
        report.addSummary("Pass Names", createdPassNames.toString());
        List<String> tagTexts = new ArrayList<>();
        List<String> billingIds = new ArrayList<>();
        for (Object[] cfg : PASS_CONFIGS) {
            String t = (String) cfg[5];
            tagTexts.add(t.isEmpty() ? "(none)" : t);
            billingIds.add((String) cfg[6]);
        }
        report.addSummary("Final Prices (after update)", createdPassPrices.toString());
        report.addSummary("Tag Texts", tagTexts.toString());
        report.addSummary("Billing Product IDs", billingIds.toString());
        report.addSummary("Price Increase Applied", "+" + PRICE_UPDATE_INCREASE);
        report.addSummary("Base URL", ConfigManager.getBaseUrl());

        String reportPath = report.generateHtmlReport();
        logger.info(">> Execution report generated at: {}", reportPath);

        // Print console summary
        report.printConsoleSummary();

        report.addStep("TC14", "Generate Report", "PASS", "Report saved to: " + reportPath);
    }

    // ========================================================================
    // Helper: Build an HTML table string for embedding in report details
    // ========================================================================

    private String htmlTable(String[] headers, List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table class='detail-table'><thead><tr>");
        for (String h : headers) sb.append("<th>").append(h).append("</th>");
        sb.append("</tr></thead><tbody>");
        for (String[] row : rows) {
            sb.append("<tr>");
            for (String cell : row) sb.append("<td>").append(cell).append("</td>");
            sb.append("</tr>");
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    @AfterClass
    public void tearDown() {
        logger.info("========================================================");
        logger.info("  GEMS PACK E2E TEST — EXECUTION COMPLETED");
        logger.info("  Tab ID: {}", gemsTabId);
        logger.info("  Pass IDs: {}", createdPassIds);
        logger.info("  Final Prices: {}", createdPassPrices);
        logger.info("========================================================");
    }
}
