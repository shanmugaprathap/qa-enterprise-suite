package com.enterprise.qa.api.utils;

import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Utility class providing common API response assertions.
 */
@Slf4j
public class ApiAssertions {

    private ApiAssertions() {
        // Utility class
    }

    /**
     * Asserts that the response status code matches the expected value.
     *
     * @param response       the API response
     * @param expectedStatus the expected status code
     */
    public static void assertStatusCode(Response response, int expectedStatus) {
        assertThat(response.getStatusCode())
                .as("Status code should be %d", expectedStatus)
                .isEqualTo(expectedStatus);
    }

    /**
     * Asserts that the response status code is in the 2xx range.
     *
     * @param response the API response
     */
    public static void assertSuccess(Response response) {
        assertThat(response.getStatusCode())
                .as("Response should be successful (2xx)")
                .isBetween(200, 299);
    }

    /**
     * Asserts that the response time is within acceptable limits.
     *
     * @param response      the API response
     * @param maxTimeMillis the maximum acceptable response time in milliseconds
     */
    public static void assertResponseTime(Response response, long maxTimeMillis) {
        long responseTime = response.getTimeIn(TimeUnit.MILLISECONDS);
        assertThat(responseTime)
                .as("Response time should be less than %d ms (was %d ms)", maxTimeMillis, responseTime)
                .isLessThan(maxTimeMillis);
    }

    /**
     * Asserts that the response content type matches.
     *
     * @param response            the API response
     * @param expectedContentType the expected content type
     */
    public static void assertContentType(Response response, String expectedContentType) {
        String contentType = response.getContentType();
        assertThat(contentType)
                .as("Content-Type should contain %s", expectedContentType)
                .contains(expectedContentType);
    }

    /**
     * Asserts that the response is JSON.
     *
     * @param response the API response
     */
    public static void assertJsonContentType(Response response) {
        assertContentType(response, "application/json");
    }

    /**
     * Asserts that a JSON path exists in the response.
     *
     * @param response the API response
     * @param jsonPath the JSON path to check
     */
    public static void assertJsonPathExists(Response response, String jsonPath) {
        Object value = response.jsonPath().get(jsonPath);
        assertThat(value)
                .as("JSON path '%s' should exist", jsonPath)
                .isNotNull();
    }

    /**
     * Asserts that a JSON path has a specific value.
     *
     * @param response      the API response
     * @param jsonPath      the JSON path
     * @param expectedValue the expected value
     */
    public static void assertJsonPathEquals(Response response, String jsonPath, Object expectedValue) {
        Object actualValue = response.jsonPath().get(jsonPath);
        assertThat(actualValue)
                .as("JSON path '%s' should equal '%s'", jsonPath, expectedValue)
                .isEqualTo(expectedValue);
    }

    /**
     * Asserts that a JSON path contains a specific value.
     *
     * @param response      the API response
     * @param jsonPath      the JSON path
     * @param expectedValue the expected value to contain
     */
    public static void assertJsonPathContains(Response response, String jsonPath, String expectedValue) {
        String actualValue = response.jsonPath().getString(jsonPath);
        assertThat(actualValue)
                .as("JSON path '%s' should contain '%s'", jsonPath, expectedValue)
                .contains(expectedValue);
    }

    /**
     * Asserts that a JSON array has a specific size.
     *
     * @param response     the API response
     * @param jsonPath     the JSON path to the array
     * @param expectedSize the expected size
     */
    public static void assertJsonArraySize(Response response, String jsonPath, int expectedSize) {
        List<?> array = response.jsonPath().getList(jsonPath);
        assertThat(array)
                .as("JSON array at '%s' should have size %d", jsonPath, expectedSize)
                .hasSize(expectedSize);
    }

    /**
     * Asserts that a JSON array is not empty.
     *
     * @param response the API response
     * @param jsonPath the JSON path to the array
     */
    public static void assertJsonArrayNotEmpty(Response response, String jsonPath) {
        List<?> array = response.jsonPath().getList(jsonPath);
        assertThat(array)
                .as("JSON array at '%s' should not be empty", jsonPath)
                .isNotEmpty();
    }

    /**
     * Asserts that a header exists in the response.
     *
     * @param response   the API response
     * @param headerName the header name
     */
    public static void assertHeaderExists(Response response, String headerName) {
        String headerValue = response.getHeader(headerName);
        assertThat(headerValue)
                .as("Header '%s' should exist", headerName)
                .isNotNull();
    }

    /**
     * Asserts that a header has a specific value.
     *
     * @param response      the API response
     * @param headerName    the header name
     * @param expectedValue the expected value
     */
    public static void assertHeaderEquals(Response response, String headerName, String expectedValue) {
        String actualValue = response.getHeader(headerName);
        assertThat(actualValue)
                .as("Header '%s' should equal '%s'", headerName, expectedValue)
                .isEqualTo(expectedValue);
    }

    /**
     * Asserts that the response body is not empty.
     *
     * @param response the API response
     */
    public static void assertBodyNotEmpty(Response response) {
        String body = response.getBody().asString();
        assertThat(body)
                .as("Response body should not be empty")
                .isNotEmpty();
    }

    /**
     * Asserts that the response matches a JSON schema.
     *
     * @param response       the API response
     * @param schemaFilePath the path to the JSON schema file
     */
    public static void assertMatchesSchema(Response response, String schemaFilePath) {
        response.then().assertThat()
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(schemaFilePath));
    }

    /**
     * Asserts that specific fields are present in the response.
     *
     * @param response the API response
     * @param fields   the fields to check
     */
    public static void assertFieldsPresent(Response response, String... fields) {
        Map<String, Object> body = response.jsonPath().getMap("");
        for (String field : fields) {
            assertThat(body)
                    .as("Field '%s' should be present", field)
                    .containsKey(field);
        }
    }

    /**
     * Asserts that a field is not null.
     *
     * @param response the API response
     * @param jsonPath the JSON path
     */
    public static void assertNotNull(Response response, String jsonPath) {
        Object value = response.jsonPath().get(jsonPath);
        assertThat(value)
                .as("Value at '%s' should not be null", jsonPath)
                .isNotNull();
    }

    /**
     * Asserts that a numeric field is positive.
     *
     * @param response the API response
     * @param jsonPath the JSON path
     */
    public static void assertPositive(Response response, String jsonPath) {
        Number value = response.jsonPath().get(jsonPath);
        assertThat(value.doubleValue())
                .as("Value at '%s' should be positive", jsonPath)
                .isPositive();
    }

    /**
     * Logs the response details for debugging.
     *
     * @param response the API response
     */
    public static void logResponse(Response response) {
        log.info("Response Status: {}", response.getStatusCode());
        log.info("Response Time: {} ms", response.getTimeIn(TimeUnit.MILLISECONDS));
        log.info("Response Body: {}", response.getBody().asPrettyString());
    }
}
