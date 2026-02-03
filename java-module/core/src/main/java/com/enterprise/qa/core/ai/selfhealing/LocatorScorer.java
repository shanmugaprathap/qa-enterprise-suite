package com.enterprise.qa.core.ai.selfhealing;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;

import java.util.Map;

/**
 * Scores candidate elements against a reference snapshot to find the best match.
 * Uses multiple criteria including attributes, position, and visual similarity.
 */
@Slf4j
public class LocatorScorer {

    // Scoring weights
    private static final double WEIGHT_TAG_NAME = 0.15;
    private static final double WEIGHT_ID = 0.20;
    private static final double WEIGHT_CLASS = 0.15;
    private static final double WEIGHT_TEXT = 0.15;
    private static final double WEIGHT_POSITION = 0.10;
    private static final double WEIGHT_SIZE = 0.10;
    private static final double WEIGHT_ATTRIBUTES = 0.15;

    /**
     * Scores an element against a reference snapshot.
     *
     * @param element  the candidate element
     * @param snapshot the reference snapshot (can be null)
     * @return a score between 0 and 1
     */
    public double scoreElement(WebElement element, ElementSnapshot snapshot) {
        if (snapshot == null) {
            // Without a snapshot, return a base score for visible, enabled elements
            return scoreWithoutSnapshot(element);
        }

        double score = 0.0;

        // Tag name match
        score += scoreTagName(element, snapshot) * WEIGHT_TAG_NAME;

        // ID similarity
        score += scoreId(element, snapshot) * WEIGHT_ID;

        // Class similarity
        score += scoreClass(element, snapshot) * WEIGHT_CLASS;

        // Text content similarity
        score += scoreText(element, snapshot) * WEIGHT_TEXT;

        // Position similarity
        score += scorePosition(element, snapshot) * WEIGHT_POSITION;

        // Size similarity
        score += scoreSize(element, snapshot) * WEIGHT_SIZE;

        // Attribute similarity
        score += scoreAttributes(element, snapshot) * WEIGHT_ATTRIBUTES;

        return Math.min(1.0, Math.max(0.0, score));
    }

    /**
     * Scores an element without a reference snapshot.
     */
    private double scoreWithoutSnapshot(WebElement element) {
        double score = 0.5; // Base score

        // Bonus for being enabled and displayed
        if (element.isEnabled()) {
            score += 0.2;
        }
        if (element.isDisplayed()) {
            score += 0.2;
        }

        // Bonus for having an ID
        String id = element.getAttribute("id");
        if (id != null && !id.isEmpty()) {
            score += 0.1;
        }

        return Math.min(1.0, score);
    }

    /**
     * Scores tag name match.
     */
    private double scoreTagName(WebElement element, ElementSnapshot snapshot) {
        String elementTag = element.getTagName().toLowerCase();
        String snapshotTag = snapshot.getTagName() != null ? snapshot.getTagName().toLowerCase() : "";

        return elementTag.equals(snapshotTag) ? 1.0 : 0.0;
    }

    /**
     * Scores ID similarity.
     */
    private double scoreId(WebElement element, ElementSnapshot snapshot) {
        String elementId = element.getAttribute("id");
        String snapshotId = snapshot.getId();

        if (elementId == null || elementId.isEmpty() || snapshotId == null || snapshotId.isEmpty()) {
            return 0.5; // Neutral score when IDs are missing
        }

        if (elementId.equals(snapshotId)) {
            return 1.0;
        }

        // Partial match
        return calculateStringSimilarity(elementId, snapshotId);
    }

    /**
     * Scores class similarity.
     */
    private double scoreClass(WebElement element, ElementSnapshot snapshot) {
        String elementClass = element.getAttribute("class");
        String snapshotClass = snapshot.getClassName();

        if (elementClass == null || snapshotClass == null) {
            return 0.5;
        }

        String[] elementClasses = elementClass.split("\\s+");
        String[] snapshotClasses = snapshotClass.split("\\s+");

        int matches = 0;
        for (String ec : elementClasses) {
            for (String sc : snapshotClasses) {
                if (ec.equals(sc)) {
                    matches++;
                    break;
                }
            }
        }

        int total = Math.max(elementClasses.length, snapshotClasses.length);
        return total > 0 ? (double) matches / total : 0.5;
    }

    /**
     * Scores text content similarity.
     */
    private double scoreText(WebElement element, ElementSnapshot snapshot) {
        String elementText = element.getText();
        String snapshotText = snapshot.getText();

        if (elementText == null || elementText.isEmpty() ||
                snapshotText == null || snapshotText.isEmpty()) {
            return 0.5;
        }

        elementText = elementText.trim().toLowerCase();
        snapshotText = snapshotText.trim().toLowerCase();

        if (elementText.equals(snapshotText)) {
            return 1.0;
        }

        return calculateStringSimilarity(elementText, snapshotText);
    }

    /**
     * Scores position similarity.
     */
    private double scorePosition(WebElement element, ElementSnapshot snapshot) {
        try {
            Point location = element.getLocation();
            int dx = Math.abs(location.getX() - snapshot.getX());
            int dy = Math.abs(location.getY() - snapshot.getY());

            // Allow up to 100 pixels difference
            double distance = Math.sqrt(dx * dx + dy * dy);
            return Math.max(0, 1 - (distance / 200));
        } catch (Exception e) {
            return 0.5;
        }
    }

    /**
     * Scores size similarity.
     */
    private double scoreSize(WebElement element, ElementSnapshot snapshot) {
        try {
            int elementWidth = element.getSize().getWidth();
            int elementHeight = element.getSize().getHeight();
            int snapshotWidth = snapshot.getWidth();
            int snapshotHeight = snapshot.getHeight();

            if (snapshotWidth == 0 || snapshotHeight == 0) {
                return 0.5;
            }

            double widthRatio = Math.min(elementWidth, snapshotWidth) /
                               (double) Math.max(elementWidth, snapshotWidth);
            double heightRatio = Math.min(elementHeight, snapshotHeight) /
                                (double) Math.max(elementHeight, snapshotHeight);

            return (widthRatio + heightRatio) / 2;
        } catch (Exception e) {
            return 0.5;
        }
    }

    /**
     * Scores attribute similarity.
     */
    private double scoreAttributes(WebElement element, ElementSnapshot snapshot) {
        Map<String, String> snapshotAttrs = snapshot.getDataAttributes();
        if (snapshotAttrs.isEmpty()) {
            return 0.5;
        }

        int matches = 0;
        for (Map.Entry<String, String> entry : snapshotAttrs.entrySet()) {
            String attrValue = element.getAttribute(entry.getKey());
            if (entry.getValue().equals(attrValue)) {
                matches++;
            }
        }

        return (double) matches / snapshotAttrs.size();
    }

    /**
     * Calculates string similarity using Levenshtein distance.
     *
     * @param s1 first string
     * @param s2 second string
     * @return similarity score between 0 and 1
     */
    private double calculateStringSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }

        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) {
            return 1.0;
        }

        int distance = levenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLength);
    }

    /**
     * Calculates Levenshtein distance between two strings.
     */
    private int levenshteinDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[m][n];
    }
}
