package com.iap.automation.payloads;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds tab creation/update payloads for the IAP Admin API.
 */
public class TabPayloadBuilder {

    public static Map<String, Object> buildGemsTabPayload(String tabName, String productId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tabType", "gems");
        payload.put("tabName", tabName);
        payload.put("deepLink", "");
        payload.put("platform", List.of("apk", "psrmg", "ipa"));
        payload.put("productId", productId);
        payload.put("segmentWiseDetails", buildSegmentWiseDetails());
        payload.put("howItWorks", buildHowItWorks());
        payload.put("showCompareView", true);
        payload.put("showSocialProofing", true);
        payload.put("lastXHours", 6);
        payload.put("socialProofX", 100);
        payload.put("socialProofY", "3");
        return payload;
    }

    public static Map<String, Object> buildCoinsTabPayload(String tabName, String productId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tabType", "coins");
        payload.put("tabName", tabName);
        payload.put("deepLink", "");
        payload.put("platform", List.of("apk", "psrmg", "ipa"));
        payload.put("productId", productId);
        payload.put("segmentWiseDetails", buildSegmentWiseDetails());
        payload.put("howItWorks", buildHowItWorks());
        payload.put("showCompareView", true);
        payload.put("showSocialProofing", true);
        payload.put("lastXHours", 6);
        payload.put("socialProofX", 100);
        payload.put("socialProofY", "3");
        return payload;
    }

    public static Map<String, Object> buildTournamentTabPayload(String tabName, String productId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tabType", "tournament");
        payload.put("tabName", tabName);
        payload.put("deepLink", "");
        payload.put("platform", List.of("apk", "psrmg", "ipa"));
        payload.put("productId", productId);
        payload.put("segmentWiseDetails", buildSegmentWiseDetails());
        payload.put("howItWorks", buildHowItWorks());
        payload.put("showCompareView", true);
        payload.put("showSocialProofing", true);
        payload.put("lastXHours", 6);
        payload.put("socialProofX", 100);
        payload.put("socialProofY", "3");
        return payload;
    }

    public static Map<String, Object> buildUpdateTabPayload(String mongoId, int tabId,
                                                             String tabName, String productId) {
        Map<String, Object> payload = buildGemsTabPayload(tabName, productId);
        payload.put("id", mongoId);
        payload.put("tabId", tabId);
        return payload;
    }

    private static Map<String, Object> buildSegmentWiseDetails() {
        Map<String, Object> segmentWiseDetails = new LinkedHashMap<>();

        Map<String, Object> vipCategories = new LinkedHashMap<>();
        String[] segmentNames = {"NONVIP", "VIP1", "VIP2", "VIP3", "VIP4", "VIP5", "VIP6", "NEWBIE"};
        for (int i = 0; i < segmentNames.length; i++) {
            Map<String, Object> segment = new LinkedHashMap<>();
            segment.put("enable", true);
            segment.put("tabPosition", Map.of("cashback", 0, "tournament", 1, "bonus", 2));
            segment.put("segmentName", segmentNames[i]);
            vipCategories.put(String.valueOf(i), segment);
        }
        segmentWiseDetails.put("vipCategories", vipCategories);

        segmentWiseDetails.put("paSegments", new LinkedHashMap<>());

        return segmentWiseDetails;
    }

    private static List<Map<String, String>> buildHowItWorks() {
        return List.of(
                Map.of("title", "Buy a pass",
                        "description", "Pick a pass of your choice",
                        "iconUrl", "https://d22ueo28hfk252.cloudfront.net/Content/versioned/2.0.0.1/images/version4/promotion_january_25/zenrik_images/PrimePassHIW1.png?v=1735815492"),
                Map.of("title", "Unlock Gems",
                        "description", "Get gems on every purchase",
                        "iconUrl", "https://d22ueo28hfk252.cloudfront.net/Content/versioned/2.0.0.1/images/version4/promotion_january_25/zenrik_images/PrimePassHIW2.png?v=1735815494")
        );
    }
}
