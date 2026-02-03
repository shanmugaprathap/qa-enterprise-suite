package com.enterprise.qa.api.client;

import com.enterprise.qa.core.config.ConfigManager;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * REST API client wrapper using REST Assured.
 * Provides simplified methods for common HTTP operations with built-in logging and reporting.
 */
@Slf4j
public class ApiClient {

    private final String baseUrl;
    private final RequestSpecification requestSpec;
    private final ResponseSpecification responseSpec;

    public ApiClient() {
        this(ConfigManager.getInstance().getApiBaseUrl());
    }

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.requestSpec = buildRequestSpec();
        this.responseSpec = buildResponseSpec();

        log.info("API client initialized with base URL: {}", baseUrl);
    }

    /**
     * Builds the default request specification.
     */
    private RequestSpecification buildRequestSpec() {
        return new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new AllureRestAssured())
                .log(LogDetail.ALL)
                .build();
    }

    /**
     * Builds the default response specification.
     */
    private ResponseSpecification buildResponseSpec() {
        return new ResponseSpecBuilder()
                .log(LogDetail.ALL)
                .build();
    }

    /**
     * Performs a GET request.
     *
     * @param endpoint the API endpoint
     * @return the response
     */
    public Response get(String endpoint) {
        log.info("GET {}{}", baseUrl, endpoint);
        return RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(endpoint)
                .then()
                .spec(responseSpec)
                .extract()
                .response();
    }

    /**
     * Performs a GET request with query parameters.
     *
     * @param endpoint    the API endpoint
     * @param queryParams the query parameters
     * @return the response
     */
    public Response get(String endpoint, Map<String, ?> queryParams) {
        log.info("GET {}{} with params: {}", baseUrl, endpoint, queryParams);
        return RestAssured.given()
                .spec(requestSpec)
                .queryParams(queryParams)
                .when()
                .get(endpoint)
                .then()
                .spec(responseSpec)
                .extract()
                .response();
    }

    /**
     * Performs a GET request with path parameters.
     *
     * @param endpoint   the API endpoint with placeholders
     * @param pathParams the path parameters
     * @return the response
     */
    public Response getWithPathParams(String endpoint, Map<String, ?> pathParams) {
        log.info("GET {}{} with path params: {}", baseUrl, endpoint, pathParams);
        return RestAssured.given()
                .spec(requestSpec)
                .pathParams(pathParams)
                .when()
                .get(endpoint)
                .then()
                .spec(responseSpec)
                .extract()
                .response();
    }

    /**
     * Performs a POST request with a JSON body.
     *
     * @param endpoint the API endpoint
     * @param body     the request body (will be serialized to JSON)
     * @return the response
     */
    public Response post(String endpoint, Object body) {
        log.info("POST {}{}", baseUrl, endpoint);
        return RestAssured.given()
                .spec(requestSpec)
                .body(body)
                .when()
                .post(endpoint)
                .then()
                .spec(responseSpec)
                .extract()
                .response();
    }

    /**
     * Performs a POST request without a body.
     *
     * @param endpoint the API endpoint
     * @return the response
     */
    public Response post(String endpoint) {
        log.info("POST {}{}", baseUrl, endpoint);
        return RestAssured.given()
                .spec(requestSpec)
                .when()
                .post(endpoint)
                .then()
                .spec(responseSpec)
                .extract()
                .response();
    }

    /**
     * Performs a PUT request with a JSON body.
     *
     * @param endpoint the API endpoint
     * @param body     the request body
     * @return the response
     */
    public Response put(String endpoint, Object body) {
        log.info("PUT {}{}", baseUrl, endpoint);
        return RestAssured.given()
                .spec(requestSpec)
                .body(body)
                .when()
                .put(endpoint)
                .then()
                .spec(responseSpec)
                .extract()
                .response();
    }

    /**
     * Performs a PATCH request with a JSON body.
     *
     * @param endpoint the API endpoint
     * @param body     the request body
     * @return the response
     */
    public Response patch(String endpoint, Object body) {
        log.info("PATCH {}{}", baseUrl, endpoint);
        return RestAssured.given()
                .spec(requestSpec)
                .body(body)
                .when()
                .patch(endpoint)
                .then()
                .spec(responseSpec)
                .extract()
                .response();
    }

    /**
     * Performs a DELETE request.
     *
     * @param endpoint the API endpoint
     * @return the response
     */
    public Response delete(String endpoint) {
        log.info("DELETE {}{}", baseUrl, endpoint);
        return RestAssured.given()
                .spec(requestSpec)
                .when()
                .delete(endpoint)
                .then()
                .spec(responseSpec)
                .extract()
                .response();
    }

    /**
     * Performs a DELETE request with a body.
     *
     * @param endpoint the API endpoint
     * @param body     the request body
     * @return the response
     */
    public Response delete(String endpoint, Object body) {
        log.info("DELETE {}{}", baseUrl, endpoint);
        return RestAssured.given()
                .spec(requestSpec)
                .body(body)
                .when()
                .delete(endpoint)
                .then()
                .spec(responseSpec)
                .extract()
                .response();
    }

    /**
     * Adds a header to requests.
     *
     * @param name  the header name
     * @param value the header value
     * @return a new ApiClient with the header
     */
    public ApiClient withHeader(String name, String value) {
        ApiClient newClient = new ApiClient(this.baseUrl);
        RestAssured.given().spec(newClient.requestSpec).header(name, value);
        return newClient;
    }

    /**
     * Adds authentication header.
     *
     * @param token the bearer token
     * @return a new ApiClient with auth
     */
    public ApiClient withBearerToken(String token) {
        return withHeader("Authorization", "Bearer " + token);
    }

    /**
     * Adds basic authentication.
     *
     * @param username the username
     * @param password the password
     * @return the response
     */
    public Response getWithBasicAuth(String endpoint, String username, String password) {
        return RestAssured.given()
                .spec(requestSpec)
                .auth()
                .basic(username, password)
                .when()
                .get(endpoint)
                .then()
                .spec(responseSpec)
                .extract()
                .response();
    }

    /**
     * Uploads a file.
     *
     * @param endpoint  the API endpoint
     * @param paramName the form parameter name
     * @param filePath  the file path
     * @return the response
     */
    public Response uploadFile(String endpoint, String paramName, String filePath) {
        log.info("Uploading file to {}{}", baseUrl, endpoint);
        return RestAssured.given()
                .spec(requestSpec)
                .contentType(ContentType.MULTIPART)
                .multiPart(paramName, new java.io.File(filePath))
                .when()
                .post(endpoint)
                .then()
                .spec(responseSpec)
                .extract()
                .response();
    }

    /**
     * Gets the base URL.
     *
     * @return the base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }
}
