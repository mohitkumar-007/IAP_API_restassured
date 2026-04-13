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
     * Build an update payload for an existing Gems pass.
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

    /**
     * Build a Coins pass creation payload.
     *
     * @param passName       Unique pass name
     * @param tabId          Tab ID to associate the pass with
     * @param rank           Display rank/order of the pass
     * @param grossPrice     Original price
     * @param coinsAmount    Total coins amount in the offering
     * @param initialAmount  Initial coins unlocked immediately
     */
    public static Map<String, Object> buildCoinsPassPayload(String passName, int tabId, int rank,
                                                             int grossPrice, int coinsAmount, int initialAmount,
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
        payload.put("tabType", "coins");

        // Coins offering details
        Map<String, Object> offeringDetails = new LinkedHashMap<>();
        Map<String, Object> offering = new LinkedHashMap<>();
        offering.put("type", "COINS");
        offering.put("description", "Coins pass with " + coinsAmount + " coins");
        offering.put("amount", coinsAmount);
        offering.put("initialAmount", initialAmount);
        offeringDetails.put("offerings", List.of(offering));
        payload.put("offeringDetails", offeringDetails);

        payload.put("passCatchPhrase", "BIG REWARDS");

        // Billing details
        Map<String, Object> billingDetails = new LinkedHashMap<>();
        billingDetails.put("GOOGLE", Map.of("billingProductId", billingProductId, "fee", (double) grossPrice));
        billingDetails.put("APPLE", Map.of("billingProductId", billingProductId, "fee", (double) grossPrice + 20));
        payload.put("billingDetails", billingDetails);

        payload.put("tags", "COINS PACK");
        payload.put("renewalType", "ONE_TIME");

        return payload;
    }

    /**
     * Build an update payload for an existing Coins pass.
     */
    public static Map<String, Object> buildUpdateCoinsPassPayload(String mongoId, int passId, String updatedName,
                                                                   int tabId, int rank, int grossPrice,
                                                                   int coinsAmount, int initialAmount,
                                                                   String passImageUrl, String bestValueTagText,
                                                                   String billingProductId) {
        Map<String, Object> payload = buildCoinsPassPayload(updatedName, tabId, rank, grossPrice, coinsAmount, initialAmount, passImageUrl, bestValueTagText, billingProductId);
        payload.put("id", mongoId);
        payload.put("passId", passId);
        payload.put("deleted", false);
        return payload;
    }

    /**
     * Build a Tournament pass creation payload.
     *
     * @param passName         Unique pass name
     * @param tabId            Tab ID to associate the pass with
     * @param rank             Display rank/order
     * @param grossPrice       Original price
     * @param discountedPrice  Discounted price
     * @param passImageUrl     Image URL
     * @param bestValueTagText Tag text (empty = no tag)
     * @param billingProductId Billing product ID
     * @param offerings        List of offering maps (TOURNAMENT, ADFREE, or both)
     * @param tags             Tags string e.g. "ELITE + PRIME"
     */
    public static Map<String, Object> buildTournamentPassPayload(String passName, int tabId, int rank,
                                                                  int grossPrice, int discountedPrice,
                                                                  String passImageUrl, String bestValueTagText,
                                                                  String billingProductId,
                                                                  List<Map<String, Object>> offerings,
                                                                  String tags) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", passName);
        payload.put("adminDisplayName", passName);
        payload.put("grossPrice", grossPrice);
        payload.put("productId", "RUMMY");
        payload.put("discountedPrice", discountedPrice);
        payload.put("validity", 1);
        payload.put("rank", rank);

        long now = Instant.now().toEpochMilli();
        long oneYearLater = now + (365L * 24 * 60 * 60 * 1000);

        payload.put("discountStartDate", now);
        payload.put("discountEndDate", oneYearLater);
        payload.put("passStartDate", now);
        payload.put("passEndDate", oneYearLater);
        payload.put("passImageUrl", passImageUrl);
        payload.put("showBestValueTag", bestValueTagText != null && !bestValueTagText.isEmpty());
        payload.put("bestValueTagText", bestValueTagText != null ? bestValueTagText : "");
        payload.put("passSaleEnabled", true);
        payload.put("segmentWiseDetails", buildPassSegmentDetails());
        payload.put("renewNudgeBeforeExpiryInDays", 3);
        payload.put("renewNudgeAfterExpiryInDays", 7);
        payload.put("tabId", tabId);
        payload.put("tabType", "tournament");

        // Offering details
        Map<String, Object> offeringDetails = new LinkedHashMap<>();
        offeringDetails.put("offerings", offerings);
        payload.put("offeringDetails", offeringDetails);
        payload.put("passCatchPhrase", "BIG REWARDS");

        // Billing details
        Map<String, Object> billingDetails = new LinkedHashMap<>();
        billingDetails.put("GOOGLE", Map.of("billingProductId", billingProductId, "fee", (double) grossPrice));
        billingDetails.put("APPLE", Map.of("billingProductId", billingProductId, "fee", (double) grossPrice + 20));
        payload.put("billingDetails", billingDetails);

        payload.put("tags", tags);
        payload.put("renewalType", "RENEWABLE");

        // Renewal details
        Map<String, Object> renewalDetails = new LinkedHashMap<>();
        renewalDetails.put("frequency", "WEEKLY");
        renewalDetails.put("gracePeriod", 10);
        renewalDetails.put("trialEnabled", false);
        renewalDetails.put("trialDurationDays", 4);
        payload.put("renewalDetails", renewalDetails);

        return payload;
    }

    /**
     * Build a TOURNAMENT-only offering.
     */
    public static Map<String, Object> buildTournamentOffering(int tournamentLimit, List<Integer> templates) {
        Map<String, Object> offering = new LinkedHashMap<>();
        offering.put("type", "TOURNAMENT");
        offering.put("description", "Access to Premium Tournaments on Junglee Rummy");
        offering.put("tournamentLimit", tournamentLimit);
        offering.put("templates", templates);
        return offering;
    }

    /**
     * Build an ADFREE-only offering.
     */
    public static Map<String, Object> buildAdFreeOffering() {
        Map<String, Object> offering = new LinkedHashMap<>();
        offering.put("type", "ADFREE");
        offering.put("description", "AdFree");
        offering.put("imageUrl", "");
        offering.put("placesToHideAds", List.of(
                "NC_HEADER_BANNER", "SIDEMENU_HEADER_BANNER", "SIDEMENU_BODY_BANNER", "RESULT_SCREEN_BOTTOM_BANNER",
                "WAITING_SCREEN_BANNER", "GAME_OVER_INTERSTITIAL", "PLAY_AGAIN_INTERSTITIAL", "PLAY_NOW_TAP_INTERSTITIAL",
                "LOBBY_BOTTOM_BANNER", "LOBBY_HEADER_BANNER", "RAF_BOTTOM_BANNER", "RESULT_SCREEN_CLOSE_BUTTON", "REWARD_AD_ON_HUD",
                "REWARD_AD_RESULT_SCREEN_BOTTOM", "REWARD_AD_GRATIFICATION_POPUP", "REWARD_AD_TRIGGERED_FROM_FLUTTER",
                "TOURNAMENT_DETAIL_BOTTOM_BANNER", "NNG_TIMER_BANNER_AD", "LOBBY_PRACTICE_BOTTOM_AD", "LOBBY_COINS_BOTTOM_AD",
                "LOBBY_TOURNAMENTS_BOTTOM_AD", "TOURNAMENT_LIST_BANNER_1", "TOURNAMENT_LIST_BANNER_2", "TOURNAMENT_LIST_BANNER_0",
                "TOURNAMENT_DETAIL_TIMER_BANNER", "TOURNAMENT_DETAIL_REWARD_BANNER", "BOTTOM_LEFT_BANNER_AD", "AUTO_DROP_BANNER_AD",
                "NNG_INTERSTITIAL_AD", "TOURNAMENT_JOIN_REWARD", "COINS_LIST_BANNER_0", "COINS_LIST_BANNER_1", "COINS_LIST_BANNER_2",
                "REWARDED_GAME_OVER"
        ));
        return offering;
    }

    /**
     * Build an update payload for an existing Tournament pass.
     */
    public static Map<String, Object> buildUpdateTournamentPassPayload(String mongoId, int passId, String updatedName,
                                                                       int tabId, int rank, int grossPrice,
                                                                       int discountedPrice,
                                                                       String passImageUrl, String bestValueTagText,
                                                                       String billingProductId,
                                                                       List<Map<String, Object>> offerings,
                                                                       String tags) {
        Map<String, Object> payload = buildTournamentPassPayload(updatedName, tabId, rank, grossPrice, discountedPrice, passImageUrl, bestValueTagText, billingProductId, offerings, tags);
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
