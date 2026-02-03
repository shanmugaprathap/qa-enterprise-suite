package com.enterprise.qa.core.ai.predictive;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Predictive test selector that analyzes code changes to determine
 * which tests should be run. Uses file mapping and dependency analysis
 * to select relevant tests.
 */
@Slf4j
public class PredictiveTestSelector {

    private final Map<String, Set<String>> fileToTestMapping;
    private final Map<String, Set<String>> packageToTestMapping;
    private final Map<String, Integer> testPriorityScores;

    public PredictiveTestSelector() {
        this.fileToTestMapping = new HashMap<>();
        this.packageToTestMapping = new HashMap<>();
        this.testPriorityScores = new HashMap<>();
        initializeDefaultMappings();
    }

    /**
     * Selects tests based on git diff output.
     *
     * @param gitDiff the output of git diff
     * @return list of test class names to run
     */
    public List<String> selectTests(String gitDiff) {
        Set<String> selectedTests = new LinkedHashSet<>();
        Set<String> changedFiles = parseChangedFiles(gitDiff);

        log.info("Analyzing {} changed files", changedFiles.size());

        for (String file : changedFiles) {
            // Direct file-to-test mapping
            if (fileToTestMapping.containsKey(file)) {
                selectedTests.addAll(fileToTestMapping.get(file));
            }

            // Package-based mapping
            String packageName = extractPackage(file);
            if (packageToTestMapping.containsKey(packageName)) {
                selectedTests.addAll(packageToTestMapping.get(packageName));
            }

            // Convention-based test discovery
            selectedTests.addAll(findConventionBasedTests(file));
        }

        // Sort by priority
        List<String> sortedTests = selectedTests.stream()
                .sorted((a, b) -> {
                    int priorityA = testPriorityScores.getOrDefault(a, 50);
                    int priorityB = testPriorityScores.getOrDefault(b, 50);
                    return Integer.compare(priorityB, priorityA);
                })
                .collect(Collectors.toList());

        log.info("Selected {} tests based on code changes", sortedTests.size());
        return sortedTests;
    }

    /**
     * Gets the git diff for uncommitted changes.
     *
     * @return the git diff output
     */
    public String getGitDiff() {
        return executeGitCommand("git", "diff", "HEAD");
    }

    /**
     * Gets the git diff between two commits.
     *
     * @param fromCommit the starting commit
     * @param toCommit   the ending commit
     * @return the git diff output
     */
    public String getGitDiff(String fromCommit, String toCommit) {
        return executeGitCommand("git", "diff", fromCommit, toCommit);
    }

    /**
     * Gets the git diff for the last N commits.
     *
     * @param commitCount number of commits to include
     * @return the git diff output
     */
    public String getGitDiffLastCommits(int commitCount) {
        return executeGitCommand("git", "diff", "HEAD~" + commitCount, "HEAD");
    }

    /**
     * Parses changed files from git diff output.
     */
    private Set<String> parseChangedFiles(String gitDiff) {
        Set<String> files = new HashSet<>();
        Pattern pattern = Pattern.compile("^diff --git a/(.*) b/(.*)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(gitDiff);

        while (matcher.find()) {
            files.add(matcher.group(2));
        }

        return files;
    }

    /**
     * Extracts the package name from a file path.
     */
    private String extractPackage(String filePath) {
        if (filePath.contains("src/main/java/") || filePath.contains("src/test/java/")) {
            String packagePath = filePath
                    .replaceAll("src/(main|test)/java/", "")
                    .replaceAll("/[^/]+\\.java$", "")
                    .replace("/", ".");
            return packagePath;
        }
        return "";
    }

    /**
     * Finds tests based on naming conventions.
     */
    private Set<String> findConventionBasedTests(String filePath) {
        Set<String> tests = new HashSet<>();

        if (!filePath.endsWith(".java")) {
            return tests;
        }

        // Extract class name
        String className = filePath
                .replaceAll(".*/", "")
                .replace(".java", "");

        // Skip if it's already a test file
        if (className.endsWith("Test") || className.startsWith("Test")) {
            tests.add(className);
            return tests;
        }

        // Convention: ClassName -> ClassNameTest
        tests.add(className + "Test");

        // Convention: ClassName -> ClassNameTests
        tests.add(className + "Tests");

        // Convention: ClassName -> TestClassName
        tests.add("Test" + className);

        // Convention: ClassName -> ClassNameIT (integration tests)
        tests.add(className + "IT");

        return tests;
    }

    /**
     * Adds a direct file-to-test mapping.
     *
     * @param sourceFile the source file path
     * @param testClass  the test class name
     */
    public void addFileMapping(String sourceFile, String testClass) {
        fileToTestMapping.computeIfAbsent(sourceFile, k -> new HashSet<>()).add(testClass);
    }

    /**
     * Adds a package-to-test mapping.
     *
     * @param packageName the package name
     * @param testClass   the test class name
     */
    public void addPackageMapping(String packageName, String testClass) {
        packageToTestMapping.computeIfAbsent(packageName, k -> new HashSet<>()).add(testClass);
    }

    /**
     * Sets the priority score for a test (higher = runs first).
     *
     * @param testClass the test class name
     * @param priority  the priority score (0-100)
     */
    public void setTestPriority(String testClass, int priority) {
        testPriorityScores.put(testClass, priority);
    }

    /**
     * Initializes default mappings for common patterns.
     */
    private void initializeDefaultMappings() {
        // Core configuration changes should run smoke tests
        addPackageMapping("com.enterprise.qa.core.config", "SmokeTest");
        addPackageMapping("com.enterprise.qa.core.drivers", "SmokeTest");

        // API changes should run API tests
        addPackageMapping("com.enterprise.qa.api", "ApiSmokeTest");

        // UI changes should run UI smoke tests
        addPackageMapping("com.enterprise.qa.ui", "UISmokeTest");

        // Set high priority for smoke tests
        setTestPriority("SmokeTest", 100);
        setTestPriority("ApiSmokeTest", 90);
        setTestPriority("UISmokeTest", 90);
    }

    /**
     * Loads file-to-test mappings from a configuration file.
     *
     * @param mappingFile path to the mapping file
     */
    public void loadMappingsFromFile(Path mappingFile) {
        try {
            List<String> lines = Files.readAllLines(mappingFile);
            for (String line : lines) {
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("=>");
                if (parts.length == 2) {
                    String source = parts[0].trim();
                    String test = parts[1].trim();
                    addFileMapping(source, test);
                }
            }
            log.info("Loaded {} mappings from file", lines.size());
        } catch (IOException e) {
            log.error("Failed to load mappings from file: {}", e.getMessage());
        }
    }

    /**
     * Executes a git command and returns the output.
     */
    private String executeGitCommand(String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            process.waitFor();
            return output.toString();

        } catch (Exception e) {
            log.error("Failed to execute git command: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Analyzes test history to improve predictions.
     * This would typically read from a test results database.
     *
     * @param testClass the test class name
     * @return analysis result with failure rate and average duration
     */
    public TestAnalysis analyzeTestHistory(String testClass) {
        // This is a placeholder for actual implementation
        // In a real system, this would query a test results database
        return new TestAnalysis(testClass, 0.05, 5000);
    }

    /**
     * Test analysis result containing historical metrics.
     */
    public static class TestAnalysis {
        private final String testClass;
        private final double failureRate;
        private final long averageDurationMs;

        public TestAnalysis(String testClass, double failureRate, long averageDurationMs) {
            this.testClass = testClass;
            this.failureRate = failureRate;
            this.averageDurationMs = averageDurationMs;
        }

        public String getTestClass() { return testClass; }
        public double getFailureRate() { return failureRate; }
        public long getAverageDurationMs() { return averageDurationMs; }
    }
}
