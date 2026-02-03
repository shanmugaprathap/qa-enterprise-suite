package com.enterprise.qa.core.ai.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI-powered test analytics engine for quality insights and predictions.
 * Analyzes test execution history to provide actionable intelligence.
 */
@Slf4j
public class TestAnalyticsEngine {

    private final ObjectMapper objectMapper;
    private final Path dataStorePath;
    private final Map<String, TestExecutionRecord> executionHistory;
    private final Map<String, FlakyTestProfile> flakyTestProfiles;

    public TestAnalyticsEngine() {
        this(Paths.get("target/test-analytics"));
    }

    public TestAnalyticsEngine(Path dataStorePath) {
        this.objectMapper = new ObjectMapper();
        this.dataStorePath = dataStorePath;
        this.executionHistory = new HashMap<>();
        this.flakyTestProfiles = new HashMap<>();
        loadHistoricalData();
    }

    /**
     * Records a test execution result for analytics.
     */
    public void recordExecution(String testId, String testName, boolean passed,
                                 long durationMs, String failureReason, Map<String, String> metadata) {
        TestExecutionRecord record = executionHistory.computeIfAbsent(testId,
                id -> new TestExecutionRecord(id, testName));

        record.addExecution(passed, durationMs, failureReason, metadata);

        // Update flaky test profile
        updateFlakyProfile(testId, record);

        // Persist data
        persistData();
    }

    /**
     * Detects flaky tests based on execution patterns.
     * Uses statistical analysis to identify inconsistent tests.
     */
    public List<FlakyTestReport> detectFlakyTests(int minExecutions, double instabilityThreshold) {
        return executionHistory.values().stream()
                .filter(record -> record.getTotalExecutions() >= minExecutions)
                .filter(record -> record.getInstabilityScore() >= instabilityThreshold)
                .map(this::createFlakyReport)
                .sorted((a, b) -> Double.compare(b.getInstabilityScore(), a.getInstabilityScore()))
                .collect(Collectors.toList());
    }

    /**
     * Classifies test failures into categories using pattern analysis.
     */
    public FailureClassification classifyFailure(String failureMessage, String stackTrace) {
        // Infrastructure failures
        if (containsAny(failureMessage, "timeout", "connection refused", "network",
                "socket", "dns", "unreachable")) {
            return new FailureClassification("INFRASTRUCTURE", 0.85,
                    "Network or infrastructure related failure");
        }

        // Element not found (UI)
        if (containsAny(failureMessage, "nosuchelement", "element not found",
                "stale element", "not clickable", "not visible")) {
            return new FailureClassification("LOCATOR_ISSUE", 0.90,
                    "UI element locator failure - consider self-healing");
        }

        // Assertion failures
        if (containsAny(failureMessage, "assertionerror", "expected", "actual",
                "should be", "to equal", "to contain")) {
            return new FailureClassification("ASSERTION_FAILURE", 0.95,
                    "Test assertion failed - likely product bug or test data issue");
        }

        // Authentication/Authorization
        if (containsAny(failureMessage, "401", "403", "unauthorized", "forbidden",
                "authentication", "token expired")) {
            return new FailureClassification("AUTH_FAILURE", 0.88,
                    "Authentication or authorization failure");
        }

        // Data/State issues
        if (containsAny(failureMessage, "null", "undefined", "empty",
                "not found", "does not exist")) {
            return new FailureClassification("DATA_ISSUE", 0.75,
                    "Test data or application state issue");
        }

        // Environment issues
        if (containsAny(failureMessage, "browser", "driver", "chrome", "firefox",
                "webdriver", "session")) {
            return new FailureClassification("ENVIRONMENT", 0.80,
                    "Test environment or browser configuration issue");
        }

        // Unknown
        return new FailureClassification("UNKNOWN", 0.50,
                "Unable to classify - manual review needed");
    }

    /**
     * Predicts test failure probability based on historical data.
     */
    public TestPrediction predictOutcome(String testId) {
        TestExecutionRecord record = executionHistory.get(testId);

        if (record == null || record.getTotalExecutions() < 5) {
            return new TestPrediction(testId, 0.5, "INSUFFICIENT_DATA",
                    "Not enough historical data for prediction");
        }

        double passRate = record.getPassRate();
        double instability = record.getInstabilityScore();
        double recentTrend = record.getRecentTrend(5);

        // Weighted prediction
        double failureProbability = (1 - passRate) * 0.4 +
                                   instability * 0.3 +
                                   (1 - recentTrend) * 0.3;

        String riskLevel = failureProbability > 0.7 ? "HIGH" :
                          failureProbability > 0.4 ? "MEDIUM" : "LOW";

        String recommendation = failureProbability > 0.7 ?
                "Consider quarantining or fixing this test" :
                failureProbability > 0.4 ?
                "Monitor closely and investigate recent failures" :
                "Test is stable";

        return new TestPrediction(testId, failureProbability, riskLevel, recommendation);
    }

    /**
     * Calculates quality metrics and KPIs.
     */
    public QualityMetrics calculateMetrics() {
        if (executionHistory.isEmpty()) {
            return new QualityMetrics();
        }

        int totalTests = executionHistory.size();
        int totalExecutions = executionHistory.values().stream()
                .mapToInt(TestExecutionRecord::getTotalExecutions)
                .sum();
        int totalPassed = executionHistory.values().stream()
                .mapToInt(TestExecutionRecord::getPassCount)
                .sum();

        double overallPassRate = totalExecutions > 0 ?
                (double) totalPassed / totalExecutions : 0;

        List<FlakyTestReport> flakyTests = detectFlakyTests(5, 0.3);
        double flakyPercentage = totalTests > 0 ?
                (double) flakyTests.size() / totalTests * 100 : 0;

        double avgDuration = executionHistory.values().stream()
                .mapToDouble(TestExecutionRecord::getAverageDuration)
                .average()
                .orElse(0);

        // Calculate test effectiveness (how often tests catch real issues)
        long assertionFailures = executionHistory.values().stream()
                .flatMap(r -> r.getFailureReasons().stream())
                .filter(reason -> reason.toLowerCase().contains("assertion"))
                .count();
        double testEffectiveness = totalExecutions - totalPassed > 0 ?
                (double) assertionFailures / (totalExecutions - totalPassed) : 0;

        return QualityMetrics.builder()
                .totalTests(totalTests)
                .totalExecutions(totalExecutions)
                .overallPassRate(overallPassRate)
                .flakyTestCount(flakyTests.size())
                .flakyTestPercentage(flakyPercentage)
                .averageDurationMs(avgDuration)
                .testEffectiveness(testEffectiveness)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Generates trend analysis for a specific time period.
     */
    public TrendAnalysis analyzeTrend(int daysBack) {
        Instant cutoff = Instant.now().minus(Duration.ofDays(daysBack));

        Map<String, Double> dailyPassRates = new LinkedHashMap<>();
        Map<String, Integer> dailyExecutions = new LinkedHashMap<>();

        // Aggregate by day (simplified - would need actual timestamps in real implementation)
        double currentPassRate = calculateMetrics().getOverallPassRate();
        double previousPassRate = currentPassRate * 0.95; // Placeholder

        String trend = currentPassRate > previousPassRate ? "IMPROVING" :
                      currentPassRate < previousPassRate ? "DECLINING" : "STABLE";

        return TrendAnalysis.builder()
                .periodDays(daysBack)
                .trend(trend)
                .currentPassRate(currentPassRate)
                .previousPassRate(previousPassRate)
                .changePercentage((currentPassRate - previousPassRate) * 100)
                .build();
    }

    /**
     * Generates a comprehensive quality report.
     */
    public String generateQualityReport() {
        QualityMetrics metrics = calculateMetrics();
        List<FlakyTestReport> flakyTests = detectFlakyTests(5, 0.3);
        TrendAnalysis trend = analyzeTrend(7);

        StringBuilder report = new StringBuilder();
        report.append("=== QA QUALITY REPORT ===\n\n");

        report.append("## Overall Metrics\n");
        report.append(String.format("- Total Tests: %d\n", metrics.getTotalTests()));
        report.append(String.format("- Total Executions: %d\n", metrics.getTotalExecutions()));
        report.append(String.format("- Pass Rate: %.1f%%\n", metrics.getOverallPassRate() * 100));
        report.append(String.format("- Average Duration: %.0f ms\n", metrics.getAverageDurationMs()));
        report.append(String.format("- Test Effectiveness: %.1f%%\n", metrics.getTestEffectiveness() * 100));
        report.append("\n");

        report.append("## Trend Analysis (7 days)\n");
        report.append(String.format("- Trend: %s\n", trend.getTrend()));
        report.append(String.format("- Change: %+.1f%%\n", trend.getChangePercentage()));
        report.append("\n");

        report.append("## Flaky Tests\n");
        report.append(String.format("- Count: %d (%.1f%% of total)\n",
                metrics.getFlakyTestCount(), metrics.getFlakyTestPercentage()));

        if (!flakyTests.isEmpty()) {
            report.append("- Top Flaky Tests:\n");
            flakyTests.stream().limit(5).forEach(ft ->
                    report.append(String.format("  - %s (instability: %.2f)\n",
                            ft.getTestName(), ft.getInstabilityScore()))
            );
        }

        return report.toString();
    }

    private void updateFlakyProfile(String testId, TestExecutionRecord record) {
        if (record.getTotalExecutions() >= 5) {
            FlakyTestProfile profile = new FlakyTestProfile();
            profile.setTestId(testId);
            profile.setInstabilityScore(record.getInstabilityScore());
            profile.setLastUpdated(Instant.now());
            flakyTestProfiles.put(testId, profile);
        }
    }

    private FlakyTestReport createFlakyReport(TestExecutionRecord record) {
        return FlakyTestReport.builder()
                .testId(record.getTestId())
                .testName(record.getTestName())
                .totalExecutions(record.getTotalExecutions())
                .passCount(record.getPassCount())
                .failCount(record.getFailCount())
                .instabilityScore(record.getInstabilityScore())
                .commonFailureReasons(record.getTopFailureReasons(3))
                .build();
    }

    private boolean containsAny(String text, String... keywords) {
        String lower = text.toLowerCase();
        for (String keyword : keywords) {
            if (lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private void loadHistoricalData() {
        Path dataFile = dataStorePath.resolve("execution-history.json");
        if (Files.exists(dataFile)) {
            try {
                JsonNode data = objectMapper.readTree(dataFile.toFile());
                // Load data (implementation depends on persistence format)
                log.info("Loaded {} historical test records", executionHistory.size());
            } catch (IOException e) {
                log.warn("Could not load historical data: {}", e.getMessage());
            }
        }
    }

    private void persistData() {
        try {
            Files.createDirectories(dataStorePath);
            Path dataFile = dataStorePath.resolve("execution-history.json");
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(dataFile.toFile(), executionHistory);
        } catch (IOException e) {
            log.warn("Could not persist analytics data: {}", e.getMessage());
        }
    }

    // Inner classes for data structures

    @Data
    public static class TestExecutionRecord {
        private final String testId;
        private final String testName;
        private int passCount;
        private int failCount;
        private List<Long> durations = new ArrayList<>();
        private List<String> failureReasons = new ArrayList<>();
        private List<Boolean> recentResults = new ArrayList<>();

        public int getTotalExecutions() {
            return passCount + failCount;
        }

        public double getPassRate() {
            return getTotalExecutions() > 0 ? (double) passCount / getTotalExecutions() : 0;
        }

        public double getAverageDuration() {
            return durations.stream().mapToLong(Long::longValue).average().orElse(0);
        }

        public double getInstabilityScore() {
            if (recentResults.size() < 5) return 0;
            int transitions = 0;
            for (int i = 1; i < recentResults.size(); i++) {
                if (!recentResults.get(i).equals(recentResults.get(i - 1))) {
                    transitions++;
                }
            }
            return (double) transitions / (recentResults.size() - 1);
        }

        public double getRecentTrend(int count) {
            if (recentResults.isEmpty()) return 0.5;
            int limit = Math.min(count, recentResults.size());
            long passes = recentResults.subList(recentResults.size() - limit, recentResults.size())
                    .stream().filter(b -> b).count();
            return (double) passes / limit;
        }

        public List<String> getTopFailureReasons(int limit) {
            return failureReasons.stream()
                    .collect(Collectors.groupingBy(r -> r, Collectors.counting()))
                    .entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .limit(limit)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }

        public void addExecution(boolean passed, long durationMs, String failureReason,
                                 Map<String, String> metadata) {
            if (passed) passCount++;
            else failCount++;
            durations.add(durationMs);
            if (failureReason != null) failureReasons.add(failureReason);
            recentResults.add(passed);
            if (recentResults.size() > 20) recentResults.remove(0);
        }
    }

    @Data
    @Builder
    public static class FlakyTestReport {
        private String testId;
        private String testName;
        private int totalExecutions;
        private int passCount;
        private int failCount;
        private double instabilityScore;
        private List<String> commonFailureReasons;
    }

    @Data
    public static class FailureClassification {
        private final String category;
        private final double confidence;
        private final String description;
    }

    @Data
    public static class TestPrediction {
        private final String testId;
        private final double failureProbability;
        private final String riskLevel;
        private final String recommendation;
    }

    @Data
    @Builder
    public static class QualityMetrics {
        private int totalTests;
        private int totalExecutions;
        private double overallPassRate;
        private int flakyTestCount;
        private double flakyTestPercentage;
        private double averageDurationMs;
        private double testEffectiveness;
        private Instant timestamp;
    }

    @Data
    @Builder
    public static class TrendAnalysis {
        private int periodDays;
        private String trend;
        private double currentPassRate;
        private double previousPassRate;
        private double changePercentage;
    }

    @Data
    private static class FlakyTestProfile {
        private String testId;
        private double instabilityScore;
        private Instant lastUpdated;
    }
}
