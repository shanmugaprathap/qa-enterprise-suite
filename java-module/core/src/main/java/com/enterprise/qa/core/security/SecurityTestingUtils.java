package com.enterprise.qa.core.security;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Security testing utilities for automated security validation.
 * Covers OWASP Top 10 basic checks and common security vulnerabilities.
 */
@Slf4j
public class SecurityTestingUtils {

    // Common XSS payloads
    private static final List<String> XSS_PAYLOADS = Arrays.asList(
            "<script>alert('XSS')</script>",
            "<img src=x onerror=alert('XSS')>",
            "javascript:alert('XSS')",
            "<svg onload=alert('XSS')>",
            "'\"><script>alert('XSS')</script>",
            "<body onload=alert('XSS')>",
            "<iframe src=\"javascript:alert('XSS')\">",
            "{{constructor.constructor('alert(1)')()}}"
    );

    // Common SQL injection payloads
    private static final List<String> SQL_PAYLOADS = Arrays.asList(
            "' OR '1'='1",
            "'; DROP TABLE users;--",
            "1' OR '1'='1' --",
            "1; SELECT * FROM users",
            "' UNION SELECT NULL,NULL,NULL--",
            "admin'--",
            "1' AND '1'='1",
            "' OR 1=1 --"
    );

    // Path traversal payloads
    private static final List<String> PATH_TRAVERSAL_PAYLOADS = Arrays.asList(
            "../../../etc/passwd",
            "..\\..\\..\\windows\\system32\\config\\sam",
            "....//....//....//etc/passwd",
            "%2e%2e%2f%2e%2e%2f%2e%2e%2fetc%2fpasswd",
            "..%252f..%252f..%252fetc/passwd"
    );

    /**
     * Validates security headers on a URL.
     */
    public static SecurityHeadersResult checkSecurityHeaders(String urlString) {
        Map<String, HeaderCheckResult> results = new LinkedHashMap<>();

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            Map<String, List<String>> headers = connection.getHeaderFields();

            // Check required security headers
            results.put("Content-Security-Policy", checkHeader(headers, "Content-Security-Policy",
                    true, "Prevents XSS and injection attacks"));

            results.put("X-Content-Type-Options", checkHeader(headers, "X-Content-Type-Options",
                    true, "Should be 'nosniff'", "nosniff"));

            results.put("X-Frame-Options", checkHeader(headers, "X-Frame-Options",
                    true, "Prevents clickjacking", "DENY", "SAMEORIGIN"));

            results.put("X-XSS-Protection", checkHeader(headers, "X-XSS-Protection",
                    false, "Browser XSS filter (deprecated but still useful)", "1; mode=block"));

            results.put("Strict-Transport-Security", checkHeader(headers, "Strict-Transport-Security",
                    true, "Enforces HTTPS"));

            results.put("Referrer-Policy", checkHeader(headers, "Referrer-Policy",
                    true, "Controls referrer information"));

            results.put("Permissions-Policy", checkHeader(headers, "Permissions-Policy",
                    false, "Controls browser features"));

            // Check for headers that should NOT be present
            results.put("Server", checkAbsentHeader(headers, "Server",
                    "Should not expose server information"));

            results.put("X-Powered-By", checkAbsentHeader(headers, "X-Powered-By",
                    "Should not expose technology stack"));

            connection.disconnect();

            int passed = (int) results.values().stream().filter(HeaderCheckResult::isPassed).count();

            return SecurityHeadersResult.builder()
                    .url(urlString)
                    .passed(passed == results.size())
                    .totalChecks(results.size())
                    .passedChecks(passed)
                    .headerChecks(results)
                    .build();

        } catch (Exception e) {
            log.error("Failed to check security headers: {}", e.getMessage());
            return SecurityHeadersResult.builder()
                    .url(urlString)
                    .passed(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * Tests for XSS vulnerabilities by checking if payloads are reflected.
     */
    public static List<VulnerabilityResult> testForXSS(String baseUrl, String parameter) {
        List<VulnerabilityResult> results = new ArrayList<>();

        for (String payload : XSS_PAYLOADS) {
            try {
                String testUrl = baseUrl + "?" + parameter + "=" +
                        java.net.URLEncoder.encode(payload, "UTF-8");

                URL url = new URL(testUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Check if payload is reflected without encoding
                boolean vulnerable = response.toString().contains(payload);

                results.add(VulnerabilityResult.builder()
                        .type("XSS")
                        .payload(payload)
                        .parameter(parameter)
                        .vulnerable(vulnerable)
                        .severity(vulnerable ? "HIGH" : "NONE")
                        .description(vulnerable ?
                                "Payload reflected in response without encoding" :
                                "Payload properly sanitized")
                        .build());

                connection.disconnect();

            } catch (Exception e) {
                log.debug("XSS test failed for payload: {}", payload);
            }
        }

        return results;
    }

    /**
     * Tests for SQL injection vulnerabilities.
     */
    public static List<VulnerabilityResult> testForSQLInjection(String baseUrl, String parameter) {
        List<VulnerabilityResult> results = new ArrayList<>();

        for (String payload : SQL_PAYLOADS) {
            try {
                String testUrl = baseUrl + "?" + parameter + "=" +
                        java.net.URLEncoder.encode(payload, "UTF-8");

                URL url = new URL(testUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);

                int responseCode = connection.getResponseCode();
                String response = "";

                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    response = sb.toString();
                } catch (Exception e) {
                    // Error response might indicate SQL error
                }

                // Check for SQL error indicators
                boolean vulnerable = containsSQLError(response);

                results.add(VulnerabilityResult.builder()
                        .type("SQL_INJECTION")
                        .payload(payload)
                        .parameter(parameter)
                        .vulnerable(vulnerable)
                        .severity(vulnerable ? "CRITICAL" : "NONE")
                        .description(vulnerable ?
                                "SQL error detected in response" :
                                "No SQL error indicators found")
                        .build());

                connection.disconnect();

            } catch (Exception e) {
                log.debug("SQL injection test failed for payload: {}", payload);
            }
        }

        return results;
    }

    /**
     * Tests for sensitive data exposure in responses.
     */
    public static SensitiveDataResult checkForSensitiveData(String responseBody) {
        List<SensitiveDataMatch> matches = new ArrayList<>();

        // Check for common sensitive patterns
        Map<String, Pattern> patterns = new LinkedHashMap<>();
        patterns.put("Email", Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"));
        patterns.put("Credit Card", Pattern.compile("\\b(?:\\d{4}[- ]?){3}\\d{4}\\b"));
        patterns.put("SSN", Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"));
        patterns.put("API Key", Pattern.compile("(?i)(api[_-]?key|apikey)['\"]?\\s*[:=]\\s*['\"]?([a-zA-Z0-9_-]{20,})"));
        patterns.put("Password in URL", Pattern.compile("(?i)(password|passwd|pwd)[=:]([^&\\s]+)"));
        patterns.put("Bearer Token", Pattern.compile("Bearer\\s+[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+"));
        patterns.put("AWS Key", Pattern.compile("AKIA[0-9A-Z]{16}"));
        patterns.put("Private Key", Pattern.compile("-----BEGIN (RSA |EC )?PRIVATE KEY-----"));

        for (Map.Entry<String, Pattern> entry : patterns.entrySet()) {
            java.util.regex.Matcher matcher = entry.getValue().matcher(responseBody);
            while (matcher.find()) {
                matches.add(SensitiveDataMatch.builder()
                        .type(entry.getKey())
                        .value(maskSensitiveData(matcher.group()))
                        .position(matcher.start())
                        .build());
            }
        }

        return SensitiveDataResult.builder()
                .hasSensitiveData(!matches.isEmpty())
                .matchCount(matches.size())
                .matches(matches)
                .severity(matches.isEmpty() ? "NONE" :
                        matches.size() > 3 ? "CRITICAL" : "HIGH")
                .build();
    }

    /**
     * Validates CORS configuration.
     */
    public static CORSCheckResult checkCORS(String urlString, String origin) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("OPTIONS");
            connection.setRequestProperty("Origin", origin);
            connection.setRequestProperty("Access-Control-Request-Method", "GET");

            String allowOrigin = connection.getHeaderField("Access-Control-Allow-Origin");
            String allowMethods = connection.getHeaderField("Access-Control-Allow-Methods");
            String allowCredentials = connection.getHeaderField("Access-Control-Allow-Credentials");

            boolean isWildcard = "*".equals(allowOrigin);
            boolean allowsCredentialsWithWildcard = isWildcard && "true".equals(allowCredentials);

            connection.disconnect();

            return CORSCheckResult.builder()
                    .url(urlString)
                    .testedOrigin(origin)
                    .allowOrigin(allowOrigin)
                    .allowMethods(allowMethods)
                    .allowCredentials(allowCredentials)
                    .hasWildcardOrigin(isWildcard)
                    .vulnerable(allowsCredentialsWithWildcard)
                    .severity(allowsCredentialsWithWildcard ? "HIGH" : "NONE")
                    .recommendation(allowsCredentialsWithWildcard ?
                            "Do not use wildcard origin with credentials" :
                            isWildcard ? "Consider restricting allowed origins" : "CORS properly configured")
                    .build();

        } catch (Exception e) {
            log.error("CORS check failed: {}", e.getMessage());
            return CORSCheckResult.builder()
                    .url(urlString)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * Gets XSS test payloads for manual testing.
     */
    public static List<String> getXSSPayloads() {
        return new ArrayList<>(XSS_PAYLOADS);
    }

    /**
     * Gets SQL injection payloads for manual testing.
     */
    public static List<String> getSQLPayloads() {
        return new ArrayList<>(SQL_PAYLOADS);
    }

    private static HeaderCheckResult checkHeader(Map<String, List<String>> headers, String headerName,
                                                  boolean required, String description, String... expectedValues) {
        String value = getHeaderValue(headers, headerName);

        if (value == null) {
            return HeaderCheckResult.builder()
                    .headerName(headerName)
                    .present(false)
                    .passed(!required)
                    .description(description)
                    .recommendation(required ? "Add this security header" : "Consider adding this header")
                    .build();
        }

        boolean correctValue = expectedValues.length == 0 ||
                Arrays.stream(expectedValues).anyMatch(value::contains);

        return HeaderCheckResult.builder()
                .headerName(headerName)
                .present(true)
                .value(value)
                .passed(correctValue)
                .description(description)
                .build();
    }

    private static HeaderCheckResult checkAbsentHeader(Map<String, List<String>> headers,
                                                        String headerName, String description) {
        String value = getHeaderValue(headers, headerName);
        return HeaderCheckResult.builder()
                .headerName(headerName)
                .present(value != null)
                .value(value)
                .passed(value == null)
                .description(description)
                .recommendation(value != null ? "Remove or obfuscate this header" : null)
                .build();
    }

    private static String getHeaderValue(Map<String, List<String>> headers, String headerName) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (headerName.equalsIgnoreCase(entry.getKey()) && entry.getValue() != null) {
                return String.join(", ", entry.getValue());
            }
        }
        return null;
    }

    private static boolean containsSQLError(String response) {
        String lower = response.toLowerCase();
        return lower.contains("sql syntax") ||
               lower.contains("mysql") ||
               lower.contains("ora-") ||
               lower.contains("postgresql") ||
               lower.contains("sqlite") ||
               lower.contains("syntax error") ||
               lower.contains("unclosed quotation") ||
               lower.contains("unterminated string");
    }

    private static String maskSensitiveData(String data) {
        if (data.length() <= 8) return "****";
        return data.substring(0, 4) + "****" + data.substring(data.length() - 4);
    }

    // Result classes

    @Data
    @Builder
    public static class SecurityHeadersResult {
        private String url;
        private boolean passed;
        private int totalChecks;
        private int passedChecks;
        private Map<String, HeaderCheckResult> headerChecks;
        private String error;
    }

    @Data
    @Builder
    public static class HeaderCheckResult {
        private String headerName;
        private boolean present;
        private String value;
        private boolean passed;
        private String description;
        private String recommendation;
    }

    @Data
    @Builder
    public static class VulnerabilityResult {
        private String type;
        private String payload;
        private String parameter;
        private boolean vulnerable;
        private String severity;
        private String description;
    }

    @Data
    @Builder
    public static class SensitiveDataResult {
        private boolean hasSensitiveData;
        private int matchCount;
        private List<SensitiveDataMatch> matches;
        private String severity;
    }

    @Data
    @Builder
    public static class SensitiveDataMatch {
        private String type;
        private String value;
        private int position;
    }

    @Data
    @Builder
    public static class CORSCheckResult {
        private String url;
        private String testedOrigin;
        private String allowOrigin;
        private String allowMethods;
        private String allowCredentials;
        private boolean hasWildcardOrigin;
        private boolean vulnerable;
        private String severity;
        private String recommendation;
        private String error;
    }
}
