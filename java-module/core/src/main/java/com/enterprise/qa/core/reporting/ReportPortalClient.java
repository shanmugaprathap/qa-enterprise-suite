package com.enterprise.qa.core.reporting;

import com.enterprise.qa.core.config.ConfigManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * REST client for Report Portal API.
 * Handles authentication and provides methods for launching and logging tests.
 */
@Slf4j
public class ReportPortalClient {

    private final String endpoint;
    private final String apiKey;
    private final String project;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ReportPortalClient() {
        ConfigManager config = ConfigManager.getInstance();
        this.endpoint = normalizeEndpoint(config.getReportPortalEndpoint());
        this.apiKey = config.getReportPortalApiKey();
        this.project = config.getReportPortalProject();
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Starts a new launch in Report Portal.
     *
     * @param name        the launch name
     * @param description the launch description
     * @return the launch ID
     */
    public String startLaunch(String name, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("description", description);
        body.put("startTime", Instant.now().toString());
        body.put("mode", "DEFAULT");

        String url = String.format("%s/api/v1/%s/launch", endpoint, project);
        JsonNode response = sendRequest("POST", url, body);

        if (response != null && response.has("id")) {
            return response.get("id").asText();
        }

        throw new RuntimeException("Failed to start launch");
    }

    /**
     * Finishes a launch in Report Portal.
     *
     * @param launchId the launch ID
     */
    public void finishLaunch(String launchId) {
        Map<String, Object> body = new HashMap<>();
        body.put("endTime", Instant.now().toString());

        String url = String.format("%s/api/v1/%s/launch/%s/finish", endpoint, project, launchId);
        sendRequest("PUT", url, body);
    }

    /**
     * Starts a test item within a launch.
     *
     * @param launchId the launch ID
     * @param name     the test item name
     * @param type     the item type (suite, test, step)
     * @return the test item ID
     */
    public String startTestItem(String launchId, String name, String type) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("type", type.toUpperCase());
        body.put("launchUuid", launchId);
        body.put("startTime", Instant.now().toString());

        String url = String.format("%s/api/v1/%s/item", endpoint, project);
        JsonNode response = sendRequest("POST", url, body);

        if (response != null && response.has("id")) {
            return response.get("id").asText();
        }

        throw new RuntimeException("Failed to start test item");
    }

    /**
     * Starts a nested test item (child of another item).
     *
     * @param launchId     the launch ID
     * @param parentItemId the parent item ID
     * @param name         the test item name
     * @param type         the item type
     * @return the test item ID
     */
    public String startNestedItem(String launchId, String parentItemId, String name, String type) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("type", type.toUpperCase());
        body.put("launchUuid", launchId);
        body.put("startTime", Instant.now().toString());

        String url = String.format("%s/api/v1/%s/item/%s", endpoint, project, parentItemId);
        JsonNode response = sendRequest("POST", url, body);

        if (response != null && response.has("id")) {
            return response.get("id").asText();
        }

        throw new RuntimeException("Failed to start nested item");
    }

    /**
     * Finishes a test item.
     *
     * @param itemId the item ID
     * @param status the final status (PASSED, FAILED, SKIPPED)
     */
    public void finishTestItem(String itemId, String status) {
        Map<String, Object> body = new HashMap<>();
        body.put("endTime", Instant.now().toString());
        body.put("status", status.toUpperCase());

        String url = String.format("%s/api/v1/%s/item/%s", endpoint, project, itemId);
        sendRequest("PUT", url, body);
    }

    /**
     * Logs a message to a test item.
     *
     * @param itemId  the item ID
     * @param message the log message
     * @param level   the log level (TRACE, DEBUG, INFO, WARN, ERROR)
     */
    public void logMessage(String itemId, String message, String level) {
        Map<String, Object> body = new HashMap<>();
        body.put("itemUuid", itemId);
        body.put("message", message);
        body.put("level", level.toUpperCase());
        body.put("time", Instant.now().toString());

        String url = String.format("%s/api/v1/%s/log", endpoint, project);
        sendRequest("POST", url, body);
    }

    /**
     * Attaches a file to a test item.
     *
     * @param itemId      the item ID
     * @param fileName    the file name
     * @param contentType the MIME type
     * @param content     the file content
     */
    public void attachFile(String itemId, String fileName, String contentType, byte[] content) {
        // Note: File attachments require multipart form data
        // This is a simplified implementation
        log.debug("File attachment would be sent: {} ({} bytes)", fileName, content.length);
    }

    /**
     * Sends an HTTP request to Report Portal.
     */
    private JsonNode sendRequest(String method, String url, Map<String, Object> body) {
        try {
            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(30));

            HttpRequest request = switch (method) {
                case "POST" -> requestBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
                case "PUT" -> requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
                default -> requestBuilder.GET().build();
            };

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                if (response.body() != null && !response.body().isEmpty()) {
                    return objectMapper.readTree(response.body());
                }
                return objectMapper.createObjectNode();
            } else {
                log.error("Report Portal request failed: {} - {}",
                        response.statusCode(), response.body());
                return null;
            }

        } catch (Exception e) {
            log.error("Failed to send request to Report Portal: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Normalizes the endpoint URL.
     */
    private String normalizeEndpoint(String endpoint) {
        if (endpoint == null) {
            return null;
        }
        return endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
    }

    /**
     * Checks if the client is properly configured.
     *
     * @return true if endpoint and API key are set
     */
    public boolean isConfigured() {
        return endpoint != null && apiKey != null;
    }

    /**
     * Tests the connection to Report Portal.
     *
     * @return true if connection is successful
     */
    public boolean testConnection() {
        try {
            String url = String.format("%s/api/v1/project/%s", endpoint, project);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200;

        } catch (Exception e) {
            log.error("Failed to test Report Portal connection: {}", e.getMessage());
            return false;
        }
    }
}
