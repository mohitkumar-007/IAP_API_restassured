package com.iap.automation.payloads;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds pass creation/update payloads for the IAP Admin API (Gems/Coins type).
 */
public class PassPayloadBuilder {

    /**
     * Creates a unique pass name using a base name + timestamp suffix.
     */
    public static String generateUniquePassName(String baseName) {
        return baseName + "_" + Instant.now().toEpochMilli();
    }

    /**
     * Build a Gems pass creation payload.
     *
     * @param passName      Unique pass name
     * @param tabId         Tab ID to associate the pass with
     * @param rank          Display rank/order of the pass
     * @param grossPrice    Original price
     * @param gemsAmount    Total gems amount in the offering
     * @param initialAmount Initial gems unlocked immediately
     */
    public static Map<String, Object> buildGemsPassPayload(String passName, int tabId, int rank,
                                                            int grossPrice, int gemsAmount, int initialAmount,
                                                            String passImageUrl, String bestValueTagText,
                                                            String billingProductId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", passName);
        payload.put("adminDisplayName", passName);
        payload.put("grossPrice", grossPrice);
        payload.put("productId", "RUMMY");
        payload.put("discountedPrice", grossPrice - 10);
        payload.put("validity", 1);
        payload.put("rank", rank);

        // Discount and pass date range (future dates to keep passes active)
        long now = Instant.now().toEpochMilli();
        long thirtyDaysLater = now + (30L * 24 * 60 * 60 * 1000);
        long ninetyDaysLater = now + (90L * 24 * 60 * 60 * 1000);

        payload.put("discountStartDate", now);
        payload.put("discountEndDate", thirtyDaysLater);
        payload.put("passStartDate", now);
        payload.put("passEndDate", ninetyDaysLater);
        payload.put("passImageUrl", passImageUrl);
        payload.put("showBestValueTag", bestValueTagText != null && !bestValueTagText.isEmpty());
        payload.put("bestValueTagText", bestValueTagText != null ? bestValueTagText : "");
        payload.put("passSaleEnabled", true);
        payload.put("segmentWiseDetails", buildPassSegmentDetails());
        payload.put("renewNudgeBeforeExpiryInDays", 3);
        payload.put("renewNudgeAfterExpiryInDays", 7);
        payload.put("tabId", tabId);
        payload.put("tabType", "gems");

        // Gems offering details
        Map<String, Object> offeringDetails = new LinkedHashMap<>();
        Map<String, Object> offering = new LinkedHashMap<>();
        offering.put("type", "GEMS");
        offering.put("description", "Gems pass with " + gemsAmount + " gems");
        offering.put("amount", gemsAmount);
        offering.put("initialAmount", initialAmount);
        offeringDetails.put("offerings", List.of(offering));
        payload.put("offeringDetails", offeringDetails);

        payload.put("passCatchPhrase", "BIG REWARDS");

        // Billing details
        Map<String, Object> billingDetails = new LinkedHashMap<>();
        billingDetails.put("GOOGLE", Map.of("billingProductId", billingProductId, "fee", (double) grossPrice));
        billingDetails.put("APPLE", Map.of("billingProductId", billingProductId, "fee", (double) grossPrice + 20));
        payload.put("billingDetails", billingDetails);

        payload.put("tags", "GEMS PACK");
        payload.put("renewalType", "ONE_TIME");

        return payload;
    }

    /**
     * Build an update payload for an existing pass. Includes passId and mongo id.
     */
    public static Map<String, Object> buildUpdatePassPayload(String mongoId, int passId, String updatedName,
                                                              int tabId, int rank, int grossPrice,
                                                              int gemsAmount, int initialAmount,
                                                              String passImageUrl, String bestValueTagText,
                                                              String billingProductId) {
        Map<String, Object> payload = buildGemsPassPayload(updatedName, tabId, rank, grossPrice, gemsAmount, initialAmount, passImageUrl, bestValueTagText, billingProductId);
        payload.put("id", mongoId);
        payload.put("passId", passId);
        payload.put("deleted", false);
        return payload;
    }

    private static Map<String, Object> buildPassSegmentDetails() {
        Map<String, Object> segmentWiseDetails = new LinkedHashMap<>();

        Map<String, Object> vipCategories = new LinkedHashMap<>();
        String[] segmentNames = {"NONVIP", "VIP1", "VIP2", "VIP3", "VIP4", "VIP5", "VIP6", "NEWBIE"};
        for (int i = 0; i < segmentNames.length; i++) {
            Map<String, Object> segment = new LinkedHashMap<>();
            segment.put("segmentName", segmentNames[i]);
            segment.put("enable", true);
            segment.put("discountApplicable", i == 0);
            vipCategories.put(String.valueOf(i), segment);
        }
        segmentWiseDetails.put("vipCategories", vipCategories);
        segmentWiseDetails.put("paSegments", Map.of());

        return segmentWiseDetails;
    }
}
