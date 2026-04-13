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

@Epic("IAP Subscription Admin")
@Feature("Tournament Pass E2E Chaining")
public class TournamentPassE2ETest extends BaseTest {

    private TabApiClient tabClient;
    private PassApiClient passClient;

    // ---- Chained state ----
    private boolean tournamentTabExistedBefore = false;
    private int tournamentTabId = -1;
    private String tournamentTabMongoId;

    private final List<Integer> createdPassIds = new ArrayList<>();
    private final List<String> createdPassMongoIds = new ArrayList<>();
    private final List<String> createdPassNames = new ArrayList<>();
    private final List<Integer> createdPassPrices = new ArrayList<>();

    // ---- Report ----
    private final TestReportGenerator report = new TestReportGenerator("Tournament Pass E2E Test Report");

    // Pass configs: displayName, grossPrice, discountedPrice, bestValueTagText, billingProductId, tags, offeringType, passImageUrl
    // offeringType: "TOURNAMENT_ONLY", "ADFREE_ONLY", "TOURNAMENT_ADFREE"
    private static final Object[][] PASS_CONFIGS = {
            {"Tournament Pass",        99,  30, "Tour-Only",           "subscription_49_1", "TOURNAMENT",      "TOURNAMENT_ONLY", "https://d22ueo28hfk252.cloudfront.net/Content/versioned/2.0.0.1/images/version4/promotion_january_25/zenrik_images/PrimePassKingNew.png?v=1735815503"},
            {"Ad-Free Pass",            9,  5, "Ad-Free",           "subscription_49_1", "ADFREE",          "ADFREE_ONLY", "https://rummy-assets.jungleerummy.com/Content/versioned/2.0.0.1/images/version4/promotion_december_25/direct_api_banner/image-%2820%29-17653513568963.webp?v=1765351357"},
            {"Tournament+AdFree Pass",  199,  45, "Best Value", "subscription_49_1", "Tour+AdFree",   "TOURNAMENT_ADFREE", "https://rummy-assets.jungleerummy.com/Content/versioned/2.0.0.1/images/version4/promotion_october_25/zenrik_images/image-17600070627108.webp?v=1760007063"}
    };

    private static final int PRICE_UPDATE_INCREASE = 25;

    @BeforeClass
    @Override
    public void setup() {
        super.setup();
        tabClient = new TabApiClient(requestSpec);
        passClient = new PassApiClient(requestSpec);
        logger.info("========================================================");
        logger.info("  TOURNAMENT PASS E2E CHAINING TEST — EXECUTION STARTED");
        logger.info("  Base URL: {}", ConfigManager.getBaseUrl());
        logger.info("========================================================");
    }

    // ========================================================================
    // TC1: Check if any TOURNAMENT tab exists — display info
    // ========================================================================

    @Test(priority = 1)
    @Story("Tab Discovery")
    @Description("TC1: Check if a tab with type 'tournament' exists. Display information accordingly.")
    @Severity(SeverityLevel.BLOCKER)
    public void TC1_CheckIfTournamentTabExists() {
        logger.info("===== TC1: Check if TOURNAMENT tab exists =====");

        Response fetchResponse = tabClient.fetchAllTabs();
        ResponseHelper.logResponse("Fetch All Tabs", fetchResponse);
        Assert.assertEquals(fetchResponse.getStatusCode(), 200, "Fetch all tabs should return 200");

        List<Map<String, Object>> tabs = fetchResponse.jsonPath().getList("$");

        if (tabs != null) {
            for (Map<String, Object> tab : tabs) {
                String tabType = (String) tab.get("tabType");
                if ("tournament".equalsIgnoreCase(tabType)) {
                    tournamentTabExistedBefore = true;
                    tournamentTabId = (Integer) tab.get("tabId");
                    tournamentTabMongoId = (String) tab.get("id");
                    break;
                }
            }
        }

        if (tournamentTabExistedBefore) {
            logger.info(">> TOURNAMENT tab FOUND — tabId={}, mongoId={}", tournamentTabId, tournamentTabMongoId);
            report.addStep("TC1", "Check TOURNAMENT Tab Exists", "PASS",
                    "<span class='detail-label'>TOURNAMENT tab found</span><br>Tab ID: <b>" + tournamentTabId + "</b>, Mongo ID: " + tournamentTabMongoId);
        } else {
            logger.info(">> =====================================================");
            logger.info(">> NO TOURNAMENT TAB EXISTS!");
            logger.info(">> Total tabs returned: {}", (tabs != null ? tabs.size() : 0));
            if (tabs != null && !tabs.isEmpty()) {
                logger.info(">> Existing tabs:");
                for (Map<String, Object> tab : tabs) {
                    logger.info(">>   tabType={}, tabId={}, tabName={}",
                            tab.get("tabType"), tab.get("tabId"), tab.get("tabName"));
                }
            }
            logger.info(">> A new TOURNAMENT tab will be created in TC2.");
            logger.info(">> =====================================================");
            report.addStep("TC1", "Check TOURNAMENT Tab Exists", "INFO",
                    "No TOURNAMENT tab found. Total tabs in system: " + (tabs != null ? tabs.size() : 0) + ". A new tab will be created in TC2.");
        }
    }

    // ========================================================================
    // TC2: Create TOURNAMENT tab if it doesn't exist
    // ========================================================================

    @Test(priority = 2, dependsOnMethods = "TC1_CheckIfTournamentTabExists")
    @Story("Tab Creation")
    @Description("TC2: If no TOURNAMENT tab exists, create one named 'Tournament Pack via API'.")
    @Severity(SeverityLevel.BLOCKER)
    public void TC2_CreateTournamentTabIfNotExists() {
        logger.info("===== TC2: Create TOURNAMENT tab if not exists =====");

        if (tournamentTabExistedBefore) {
            logger.info(">> TOURNAMENT tab already exists (tabId={}). Skipping creation.", tournamentTabId);
            report.addStep("TC2", "Create TOURNAMENT Tab", "SKIP",
                    "Tab already existed — <b>tabId=" + tournamentTabId + "</b>. Creation skipped.");
            return;
        }

        String tabName = "Tournament Pack via API";
        Map<String, Object> tabPayload = TabPayloadBuilder.buildTournamentTabPayload(tabName, ConfigManager.getProductId());

        Response createResponse = tabClient.createTab(tabPayload);
        ResponseHelper.logResponse("Create TOURNAMENT Tab", createResponse);

        Assert.assertEquals(createResponse.getStatusCode(), 200,
                "Tab creation should return 200. Body: " + createResponse.getBody().asString());

        // Try extracting tabId directly from the create response
        Integer directTabId = createResponse.jsonPath().get("tabId");
        String directMongoId = createResponse.jsonPath().getString("id");

        if (directTabId != null && directTabId > 0) {
            tournamentTabId = directTabId;
            tournamentTabMongoId = directMongoId;
            logger.info(">> Tab ID extracted directly from create response: tabId={}", tournamentTabId);
        } else {
            // Fallback: re-fetch all tabs to find the newly created tournament tab
            logger.info(">> tabId not in create response. Re-fetching tabs to find the new TOURNAMENT tab...");
            Response refetch = tabClient.fetchAllTabs();
            Assert.assertEquals(refetch.getStatusCode(), 200, "Re-fetch tabs should return 200");

            List<Map<String, Object>> allTabs = refetch.jsonPath().getList("$");
            boolean found = false;
            if (allTabs != null) {
                for (Map<String, Object> tab : allTabs) {
                    String tabType = (String) tab.get("tabType");
                    if ("tournament".equalsIgnoreCase(tabType)) {
                        tournamentTabId = (Integer) tab.get("tabId");
                        tournamentTabMongoId = (String) tab.get("id");
                        found = true;
                        break;
                    }
                }
            }
            Assert.assertTrue(found, "Newly created TOURNAMENT tab should appear in fetch-all response");
            logger.info(">> Tab ID extracted from re-fetch: tabId={}", tournamentTabId);
        }

        logger.info(">> TOURNAMENT tab CREATED — tabId={}, mongoId={}, tabName='{}'", tournamentTabId, tournamentTabMongoId, tabName);
        Assert.assertTrue(tournamentTabId > 0, "Created tabId should be positive");

        report.addStep("TC2", "Create TOURNAMENT Tab", "PASS",
                "<span class='detail-label'>Tab Created Successfully</span><br>"
                + "Tab Name: <b>" + tabName + "</b><br>"
                + "Tab ID: <b>" + tournamentTabId + "</b><br>"
                + "Tab Type: <b>tournament</b><br>"
                + "Mongo ID: " + tournamentTabMongoId);
    }

    // ========================================================================
    // TC3: Fetch TOURNAMENT tab by ID and print details
    // ========================================================================

    @Test(priority = 3, dependsOnMethods = "TC2_CreateTournamentTabIfNotExists")
    @Story("Tab Verification")
    @Description("TC3: Fetch the TOURNAMENT tab by tabId and print its details.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC3_FetchTournamentTabById() {
        logger.info("===== TC3: Fetch TOURNAMENT tab by tabId={} =====", tournamentTabId);

        Response fetchResponse = tabClient.fetchTabById(tournamentTabId);
        ResponseHelper.logResponse("Fetch TOURNAMENT Tab by ID=" + tournamentTabId, fetchResponse);
        Assert.assertEquals(fetchResponse.getStatusCode(), 200,
                "Fetch tab by ID should return 200. Body: " + fetchResponse.getBody().asString());

        int fetchedTabId = fetchResponse.jsonPath().getInt("tabId");
        String fetchedTabName = fetchResponse.jsonPath().getString("tabName");
        String fetchedTabType = fetchResponse.jsonPath().getString("tabType");

        Assert.assertEquals(fetchedTabId, tournamentTabId, "Fetched tabId should match");
        Assert.assertEquals(fetchedTabType, "tournament", "Tab type should be tournament");

        logger.info(">> TOURNAMENT Tab fetched — tabId={}, tabName='{}', tabType='{}'",
                fetchedTabId, fetchedTabName, fetchedTabType);

        report.addStep("TC3", "Fetch TOURNAMENT Tab by ID", "PASS",
                "<span class='detail-label'>Tab Fetched Successfully</span><br>"
                + "Tab ID: <b>" + fetchedTabId + "</b><br>"
                + "Tab Name: <b>" + fetchedTabName + "</b><br>"
                + "Tab Type: <b>" + fetchedTabType + "</b>");
    }

    // ========================================================================
    // TC4: Attempt duplicate TOURNAMENT tab creation — expect 400 error
    // ========================================================================

    @Test(priority = 4, dependsOnMethods = "TC3_FetchTournamentTabById")
    @Story("Tab Creation — Negative")
    @Description("TC4: Attempt to create a TOURNAMENT tab again. Expect 400 with duplicate error message.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC4_DuplicateTournamentTabCreationShouldFail() {
        logger.info("===== TC4: Attempt duplicate TOURNAMENT tab creation — expect 400 =====");

        String tabName = "Tournament Pack via API";
        Map<String, Object> tabPayload = TabPayloadBuilder.buildTournamentTabPayload(tabName, ConfigManager.getProductId());

        Response createResponse = tabClient.createTab(tabPayload);
        ResponseHelper.logResponse("Duplicate TOURNAMENT Tab Creation Attempt", createResponse);

        Assert.assertEquals(createResponse.getStatusCode(), 400,
                "Duplicate tab creation should return 400. Body: " + createResponse.getBody().asString());

        String message = createResponse.jsonPath().getString("message");
        String expectedMessage = "Tab with type tournament and product RUMMY already exists";

        Assert.assertNotNull(message, "Response should contain a 'message' field");
        Assert.assertEquals(message, expectedMessage,
                "Error message should indicate duplicate tab. Actual: " + message);

        logger.info(">> Duplicate creation correctly rejected — status=400, message='{}'", message);
        report.addStep("TC4", "Duplicate TOURNAMENT Tab Creation", "PASS",
                "<span class='detail-label'>Duplicate Rejected (Expected)</span><br>"
                + "HTTP Status: <b>400</b><br>"
                + "Error Message: <b>" + message + "</b>");
    }

    // ========================================================================
    // TC5: Create 3 Tournament Passes with different offerings
    // ========================================================================

    @Test(priority = 5, dependsOnMethods = "TC4_DuplicateTournamentTabCreationShouldFail")
    @Story("Pass Creation")
    @Description("TC5: Create 3 Tournament passes — Tournament only, AdFree only, Tournament+AdFree.")
    @Severity(SeverityLevel.BLOCKER)
    public void TC5_Create3TournamentPasses() {
        logger.info("===== TC5: Create {} Tournament Passes under tabId={} =====", PASS_CONFIGS.length, tournamentTabId);

        if (!createdPassIds.isEmpty()) {
            logger.info(">> Passes already created in this run. Skipping. passIds={}", createdPassIds);
            report.addStep("TC5", "Skip — Already Created", "SKIP", "passIds=" + createdPassIds);
            return;
        }

        // Scan backend for existing passes belonging to this tab
        logger.info(">> Scanning for existing passes under tabId={}...", tournamentTabId);
        List<Integer> existingPassIds = new ArrayList<>();
        List<String> existingMongoIds = new ArrayList<>();
        List<String> existingNames = new ArrayList<>();
        List<Integer> existingPrices = new ArrayList<>();

        int consecutiveMisses = 0;
        for (int probeId = tournamentTabId + 1; consecutiveMisses < 5; probeId++) {
            Response resp = passClient.fetchPassById(probeId);
            if (resp.getStatusCode() == 200 && resp.jsonPath().getInt("tabId") == tournamentTabId) {
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
                    existingPassIds.size(), tournamentTabId, PASS_CONFIGS.length);
            for (int i = 0; i < PASS_CONFIGS.length; i++) {
                createdPassIds.add(existingPassIds.get(i));
                createdPassMongoIds.add(existingMongoIds.get(i));
                createdPassNames.add(existingNames.get(i));
                createdPassPrices.add(existingPrices.get(i));
            }
            List<String[]> reuseRows = new ArrayList<>();
            for (int i = 0; i < createdPassIds.size(); i++) {
                String offeringType = (String) PASS_CONFIGS[i][6];
                reuseRows.add(new String[]{createdPassNames.get(i), String.valueOf(createdPassIds.get(i)),
                        String.valueOf(createdPassPrices.get(i)), offeringType, (String) PASS_CONFIGS[i][4]});
            }
            report.addStep("TC5", "Reuse Existing Passes", "PASS",
                    "<span class='detail-label'>Found " + createdPassIds.size() + " existing passes on backend — reusing</span>"
                    + htmlTable(new String[]{"Pass Name", "Pass ID", "Price", "Offering Type", "Billing ID"}, reuseRows));
            return;
        }

        logger.info(">> No existing passes found. Creating {} new passes...", PASS_CONFIGS.length);

        for (int i = 0; i < PASS_CONFIGS.length; i++) {
            String displayName = (String) PASS_CONFIGS[i][0];
            int grossPrice = (int) PASS_CONFIGS[i][1];
            int discountedPrice = (int) PASS_CONFIGS[i][2];
            String bestValueTagText = (String) PASS_CONFIGS[i][3];
            String billingProductId = (String) PASS_CONFIGS[i][4];
            String tags = (String) PASS_CONFIGS[i][5];
            String offeringType = (String) PASS_CONFIGS[i][6];
            String passImageUrl = (String) PASS_CONFIGS[i][7];
            int rank = i + 1;

            String uniqueName = displayName;

            // Build offerings based on type
            List<Map<String, Object>> offerings = new ArrayList<>();
            switch (offeringType) {
                case "TOURNAMENT_ONLY":
                    offerings.add(PassPayloadBuilder.buildTournamentOffering(1000, List.of(1, 2)));
                    break;
                case "ADFREE_ONLY":
                    offerings.add(PassPayloadBuilder.buildAdFreeOffering());
                    break;
                case "TOURNAMENT_ADFREE":
                    offerings.add(PassPayloadBuilder.buildTournamentOffering(1000, List.of(1, 2)));
                    offerings.add(PassPayloadBuilder.buildAdFreeOffering());
                    break;
            }

            Map<String, Object> payload = PassPayloadBuilder.buildTournamentPassPayload(
                    uniqueName, tournamentTabId, rank, grossPrice, discountedPrice,
                    passImageUrl, bestValueTagText, billingProductId, offerings, tags);

            Response response = passClient.createPass(payload);
            ResponseHelper.logResponse("Create Pass #" + rank + " [" + uniqueName + "] (" + offeringType + ")", response);

            Assert.assertEquals(response.getStatusCode(), 200,
                    "Pass #" + rank + " creation failed. Body: " + response.getBody().asString());

            int passId = response.jsonPath().getInt("pass.passId");
            String mongoId = response.jsonPath().getString("pass.id");

            createdPassIds.add(passId);
            createdPassMongoIds.add(mongoId);
            createdPassNames.add(uniqueName);
            createdPassPrices.add(grossPrice);

            logger.info(">> Pass #{} CREATED — name='{}', passId={}, grossPrice={}, offering={}",
                    rank, uniqueName, passId, grossPrice, offeringType);

            try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        Assert.assertEquals(createdPassIds.size(), PASS_CONFIGS.length,
                "Must have created exactly " + PASS_CONFIGS.length + " passes");
        logger.info(">> All {} passes created: {}", PASS_CONFIGS.length, createdPassIds);

        List<String[]> tc5Rows = new ArrayList<>();
        for (int i = 0; i < createdPassIds.size(); i++) {
            String offeringType = (String) PASS_CONFIGS[i][6];
            tc5Rows.add(new String[]{createdPassNames.get(i), String.valueOf(createdPassIds.get(i)),
                    String.valueOf(createdPassPrices.get(i)), offeringType,
                    (String) PASS_CONFIGS[i][5], (String) PASS_CONFIGS[i][4]});
        }
        report.addStep("TC5", "Create " + PASS_CONFIGS.length + " Tournament Passes", "PASS",
                "<span class='detail-label'>" + createdPassIds.size() + " passes created under tabId=" + tournamentTabId + "</span>"
                + htmlTable(new String[]{"Pass Name", "Pass ID", "Price", "Offering Type", "Tags", "Billing ID"}, tc5Rows));
    }

    // ========================================================================
    // TC6: Fetch all 3 tournament passes by passId — print detailed info
    // ========================================================================

    @Test(priority = 6, dependsOnMethods = "TC5_Create3TournamentPasses")
    @Story("Pass Fetch")
    @Description("TC6: Fetch all 3 tournament passes individually by passId. Print detailed pass info in HTML report.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC6_FetchTournamentPassesAfterCreation() {
        logger.info("===== TC6: Fetch All Tournament Passes for tabId={} =====", tournamentTabId);

        List<String[]> tc6Rows = new ArrayList<>();

        for (int i = 0; i < createdPassIds.size(); i++) {
            int passId = createdPassIds.get(i);
            Response resp = passClient.fetchPassById(passId);
            ResponseHelper.logResponse("Fetch Pass #" + (i + 1) + " [passId=" + passId + "]", resp);

            Assert.assertEquals(resp.getStatusCode(), 200, "Fetch pass " + passId + " should return 200");
            Assert.assertEquals(resp.jsonPath().getInt("passId"), passId, "passId should match");
            Assert.assertEquals(resp.jsonPath().getInt("tabId"), tournamentTabId, "tabId should match");

            // Extract fields from response
            String passName = resp.jsonPath().getString("name");
            int grossPrice = resp.jsonPath().getInt("grossPrice");
            int discountedPrice = resp.jsonPath().getInt("discountedPrice");
            int validity = resp.jsonPath().getInt("validity");
            String renewalType = resp.jsonPath().getString("renewalType");
            String productId = resp.jsonPath().getString("productId");
            String passCatchPhrase = resp.jsonPath().getString("passCatchPhrase");
            String tags = resp.jsonPath().getString("tags");
            String bestValueTagText = resp.jsonPath().getString("bestValueTagText");

            // Extract offerings info
            List<Map<String, Object>> offerings = resp.jsonPath().getList("offeringDetails.offerings");
            StringBuilder offeringTypes = new StringBuilder();
            StringBuilder templateIds = new StringBuilder();

            if (offerings != null) {
                for (int j = 0; j < offerings.size(); j++) {
                    Map<String, Object> off = offerings.get(j);
                    String type = (String) off.get("type");
                    if (j > 0) offeringTypes.append(" + ");
                    offeringTypes.append(type);

                    // Extract templates if TOURNAMENT offering
                    if ("TOURNAMENT".equals(type) && off.get("templates") != null) {
                        templateIds.append(off.get("templates").toString());
                    }
                }
            }

            String templatesStr = templateIds.length() > 0 ? templateIds.toString() : "-";
            String tagTextStr = (bestValueTagText != null && !bestValueTagText.isEmpty()) ? bestValueTagText : "-";

            logger.info(">> Pass #{}: name='{}', price={}, discounted={}, validity={}, renewalType={}, " +
                            "productId={}, catchPhrase='{}', tags='{}', templates={}, offerings={}, tagText='{}'",
                    i + 1, passName, grossPrice, discountedPrice, validity, renewalType,
                    productId, passCatchPhrase, tags, templatesStr, offeringTypes, tagTextStr);

            tc6Rows.add(new String[]{
                    passName,
                    String.valueOf(grossPrice),
                    String.valueOf(discountedPrice),
                    String.valueOf(validity),
                    renewalType,
                    productId,
                    passCatchPhrase != null ? passCatchPhrase : "-",
                    tags != null ? tags : "-",
                    templatesStr,
                    offeringTypes.toString(),
                    tagTextStr
            });
        }

        logger.info(">> All {} passes fetched and verified successfully.", createdPassIds.size());

        report.addStep("TC6", "Fetch All Tournament Passes", "PASS",
                "<span class='detail-label'>Fetched " + createdPassIds.size() + " passes for tabId=" + tournamentTabId + "</span>"
                + htmlTable(new String[]{"Pass Name", "Pass Price", "Discounted Price", "Validity",
                        "Renewal Type", "ProductID", "Pass Catch Phrase", "Tags",
                        "Templates ID", "Offering", "Tag Text"}, tc6Rows));
    }

    // ========================================================================
    // TC7: Update pass prices (+25) and print updated prices
    // ========================================================================

    @Test(priority = 7, dependsOnMethods = "TC6_FetchTournamentPassesAfterCreation")
    @Story("Pass Update")
    @Description("TC7: Update all 3 tournament passes — increase grossPrice by 25. Print updated prices in HTML report.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC7_UpdateTournamentPassPrices() {
        logger.info("===== TC7: Update all {} passes (price +{}) =====", createdPassIds.size(), PRICE_UPDATE_INCREASE);

        List<String[]> tc7Rows = new ArrayList<>();

        for (int i = 0; i < createdPassIds.size(); i++) {
            int passId = createdPassIds.get(i);
            String mongoId = createdPassMongoIds.get(i);
            String currentName = createdPassNames.get(i);
            int oldPrice = createdPassPrices.get(i);
            int newPrice = oldPrice + PRICE_UPDATE_INCREASE;
            int discountedPrice = (int) PASS_CONFIGS[i][2];
            String bestValueTagText = (String) PASS_CONFIGS[i][3];
            String billingProductId = (String) PASS_CONFIGS[i][4];
            String tags = (String) PASS_CONFIGS[i][5];
            String offeringType = (String) PASS_CONFIGS[i][6];
            String passImageUrl = (String) PASS_CONFIGS[i][7];
            int rank = i + 1;

            // Rebuild offerings for this pass
            List<Map<String, Object>> offerings = new ArrayList<>();
            switch (offeringType) {
                case "TOURNAMENT_ONLY":
                    offerings.add(PassPayloadBuilder.buildTournamentOffering(1000, List.of(1, 2)));
                    break;
                case "ADFREE_ONLY":
                    offerings.add(PassPayloadBuilder.buildAdFreeOffering());
                    break;
                case "TOURNAMENT_ADFREE":
                    offerings.add(PassPayloadBuilder.buildTournamentOffering(1000, List.of(1, 2)));
                    offerings.add(PassPayloadBuilder.buildAdFreeOffering());
                    break;
            }

            Map<String, Object> updatePayload = PassPayloadBuilder.buildUpdateTournamentPassPayload(
                    mongoId, passId, currentName, tournamentTabId, rank, newPrice, discountedPrice,
                    passImageUrl, bestValueTagText, billingProductId, offerings, tags);

            Response resp = passClient.updatePass(passId, updatePayload);
            ResponseHelper.logResponse("Update Pass passId=" + passId + " price " + oldPrice + " -> " + newPrice, resp);

            Assert.assertEquals(resp.getStatusCode(), 200,
                    "Update pass " + passId + " should return 200. Body: " + resp.getBody().asString());

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
    // TC8: Disable passSaleEnabled on all 3 passes
    // ========================================================================

    @Test(priority = 8, dependsOnMethods = "TC7_UpdateTournamentPassPrices")
    @Story("Pass Sale Toggle")
    @Description("TC8: Set passSaleEnabled=false on all 3 tournament passes and print updated values.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC8_DisablePassSaleEnabled() {
        logger.info("===== TC8: Disable passSaleEnabled on all {} passes =====", createdPassIds.size());

        List<String[]> tc8Rows = new ArrayList<>();

        for (int i = 0; i < createdPassIds.size(); i++) {
            int passId = createdPassIds.get(i);
            String mongoId = createdPassMongoIds.get(i);
            String currentName = createdPassNames.get(i);
            int currentPrice = createdPassPrices.get(i);
            int discountedPrice = (int) PASS_CONFIGS[i][2];
            String bestValueTagText = (String) PASS_CONFIGS[i][3];
            String billingProductId = (String) PASS_CONFIGS[i][4];
            String tags = (String) PASS_CONFIGS[i][5];
            String offeringType = (String) PASS_CONFIGS[i][6];
            String passImageUrl = (String) PASS_CONFIGS[i][7];
            int rank = i + 1;

            // Rebuild offerings
            List<Map<String, Object>> offerings = new ArrayList<>();
            switch (offeringType) {
                case "TOURNAMENT_ONLY":
                    offerings.add(PassPayloadBuilder.buildTournamentOffering(1000, List.of(1, 2)));
                    break;
                case "ADFREE_ONLY":
                    offerings.add(PassPayloadBuilder.buildAdFreeOffering());
                    break;
                case "TOURNAMENT_ADFREE":
                    offerings.add(PassPayloadBuilder.buildTournamentOffering(1000, List.of(1, 2)));
                    offerings.add(PassPayloadBuilder.buildAdFreeOffering());
                    break;
            }

            Map<String, Object> updatePayload = PassPayloadBuilder.buildUpdateTournamentPassPayload(
                    mongoId, passId, currentName, tournamentTabId, rank, currentPrice, discountedPrice,
                    passImageUrl, bestValueTagText, billingProductId, offerings, tags);
            updatePayload.put("passSaleEnabled", false);

            Response resp = passClient.updatePass(passId, updatePayload);
            ResponseHelper.logResponse("Disable passSaleEnabled passId=" + passId, resp);

            Assert.assertEquals(resp.getStatusCode(), 200,
                    "Update pass " + passId + " should return 200. Body: " + resp.getBody().asString());

            boolean updatedValue = resp.jsonPath().getBoolean("pass.passSaleEnabled");
            logger.info(">> Pass passId={} — passSaleEnabled set to FALSE (response: {})", passId, updatedValue);

            tc8Rows.add(new String[]{currentName, String.valueOf(passId), "true", String.valueOf(updatedValue),
                    updatedValue ? "VISIBLE" : "HIDDEN"});
        }

        report.addStep("TC8", "Disable passSaleEnabled on All Passes", "PASS",
                "<span class='detail-label'>All " + createdPassIds.size() + " passes set to passSaleEnabled=false (HIDDEN)</span>"
                + htmlTable(new String[]{"Pass Name", "Pass ID", "Before", "After (passSaleEnabled)", "Visibility"}, tc8Rows));
        logger.info(">> All {} passes disabled (passSaleEnabled=false)", createdPassIds.size());
    }

    // ========================================================================
    // TC9: Re-enable passSaleEnabled on all 3 passes
    // ========================================================================

    @Test(priority = 9, dependsOnMethods = "TC8_DisablePassSaleEnabled")
    @Story("Pass Sale Toggle")
    @Description("TC9: Set passSaleEnabled=true on all 3 tournament passes and print updated status.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC9_EnablePassSaleEnabled() {
        logger.info("===== TC9: Re-enable passSaleEnabled on all {} passes =====", createdPassIds.size());

        List<String[]> tc9Rows = new ArrayList<>();

        for (int i = 0; i < createdPassIds.size(); i++) {
            int passId = createdPassIds.get(i);
            String mongoId = createdPassMongoIds.get(i);
            String currentName = createdPassNames.get(i);
            int currentPrice = createdPassPrices.get(i);
            int discountedPrice = (int) PASS_CONFIGS[i][2];
            String bestValueTagText = (String) PASS_CONFIGS[i][3];
            String billingProductId = (String) PASS_CONFIGS[i][4];
            String tags = (String) PASS_CONFIGS[i][5];
            String offeringType = (String) PASS_CONFIGS[i][6];
            String passImageUrl = (String) PASS_CONFIGS[i][7];
            int rank = i + 1;

            // Rebuild offerings
            List<Map<String, Object>> offerings = new ArrayList<>();
            switch (offeringType) {
                case "TOURNAMENT_ONLY":
                    offerings.add(PassPayloadBuilder.buildTournamentOffering(1000, List.of(1, 2)));
                    break;
                case "ADFREE_ONLY":
                    offerings.add(PassPayloadBuilder.buildAdFreeOffering());
                    break;
                case "TOURNAMENT_ADFREE":
                    offerings.add(PassPayloadBuilder.buildTournamentOffering(1000, List.of(1, 2)));
                    offerings.add(PassPayloadBuilder.buildAdFreeOffering());
                    break;
            }

            Map<String, Object> updatePayload = PassPayloadBuilder.buildUpdateTournamentPassPayload(
                    mongoId, passId, currentName, tournamentTabId, rank, currentPrice, discountedPrice,
                    passImageUrl, bestValueTagText, billingProductId, offerings, tags);
            updatePayload.put("passSaleEnabled", true);

            Response resp = passClient.updatePass(passId, updatePayload);
            ResponseHelper.logResponse("Enable passSaleEnabled passId=" + passId, resp);

            Assert.assertEquals(resp.getStatusCode(), 200,
                    "Update pass " + passId + " should return 200. Body: " + resp.getBody().asString());

            boolean updatedValue = resp.jsonPath().getBoolean("pass.passSaleEnabled");
            logger.info(">> Pass passId={} — passSaleEnabled set to TRUE (response: {})", passId, updatedValue);

            tc9Rows.add(new String[]{currentName, String.valueOf(passId), "false", String.valueOf(updatedValue),
                    updatedValue ? "VISIBLE" : "HIDDEN"});
        }

        report.addStep("TC9", "Re-enable passSaleEnabled on All Passes", "PASS",
                "<span class='detail-label'>All " + createdPassIds.size() + " passes set to passSaleEnabled=true (VISIBLE)</span>"
                + htmlTable(new String[]{"Pass Name", "Pass ID", "Before", "After (passSaleEnabled)", "Visibility"}, tc9Rows));
        logger.info(">> All {} passes re-enabled (passSaleEnabled=true)", createdPassIds.size());
    }

    // ========================================================================
    // TC10: Generate HTML Execution Report
    // ========================================================================

    @Test(priority = 10, dependsOnMethods = "TC9_EnablePassSaleEnabled", alwaysRun = true)
    @Story("Reporting")
    @Description("TC10: Generate HTML execution report with all test results.")
    @Severity(SeverityLevel.MINOR)
    public void TC10_GenerateExecutionReport() {
        logger.info("===== TC10: Generate Execution Report =====");

        // Add summary data
        report.addSummary("TOURNAMENT Tab ID", String.valueOf(tournamentTabId));
        report.addSummary("Tab Existed Before Run", String.valueOf(tournamentTabExistedBefore));
        report.addSummary("Passes Created", String.valueOf(createdPassIds.size()));
        report.addSummary("Pass IDs", createdPassIds.toString());
        report.addSummary("Pass Names", createdPassNames.toString());

        List<String> offeringTypes = new ArrayList<>();
        List<String> tagsList = new ArrayList<>();
        List<String> billingIds = new ArrayList<>();
        for (Object[] cfg : PASS_CONFIGS) {
            tagsList.add((String) cfg[5]);
            billingIds.add((String) cfg[4]);
            offeringTypes.add((String) cfg[6]);
        }
        report.addSummary("Final Prices (after update)", createdPassPrices.toString());
        report.addSummary("Offering Types", offeringTypes.toString());
        report.addSummary("Tags", tagsList.toString());
        report.addSummary("Billing Product IDs", billingIds.toString());
        report.addSummary("Price Increase Applied", "+" + PRICE_UPDATE_INCREASE);
        report.addSummary("Base URL", ConfigManager.getBaseUrl());

        String reportPath = report.generateHtmlReport();
        logger.info(">> Execution report generated at: {}", reportPath);

        // Print console summary
        report.printConsoleSummary();

        report.addStep("TC10", "Generate Report", "PASS", "Report saved to: " + reportPath);
    }

    @AfterClass
    public void tearDown() {
        logger.info("========================================================");
        logger.info("  TOURNAMENT PASS E2E TEST — EXECUTION COMPLETED");
        logger.info("  Tab ID: {}", tournamentTabId);
        logger.info("  Pass IDs: {}", createdPassIds);
        logger.info("  Final Prices: {}", createdPassPrices);
        logger.info("========================================================");
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
}
