package com.enterprise.qa.api.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Consumer Contract Tests using Pact
 *
 * Demonstrates contract testing between consumer and provider services.
 * These tests define the contract expectations from the consumer's perspective.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "UserService", pactVersion = PactSpecVersion.V3)
public class ConsumerContractTest {

    /**
     * Define contract for getting a user by ID
     */
    @Pact(consumer = "QATestSuite")
    public RequestResponsePact getUserByIdPact(PactDslWithProvider builder) {
        DslPart responseBody = new PactDslJsonBody()
                .integerType("id", 1)
                .stringType("username", "testuser")
                .stringType("email", "test@example.com")
                .stringType("firstName", "Test")
                .stringType("lastName", "User")
                .booleanType("active", true)
                .stringType("createdAt")
                .object("address")
                    .stringType("street", "123 Main St")
                    .stringType("city", "Test City")
                    .stringType("zipCode", "12345")
                    .stringType("country", "US")
                .closeObject();

        return builder
                .given("user with id 1 exists")
                .uponReceiving("a request for user with id 1")
                .path("/api/users/1")
                .method("GET")
                .headers(getDefaultHeaders())
                .willRespondWith()
                .status(200)
                .headers(getResponseHeaders())
                .body(responseBody)
                .toPact();
    }

    /**
     * Define contract for creating a new user
     */
    @Pact(consumer = "QATestSuite")
    public RequestResponsePact createUserPact(PactDslWithProvider builder) {
        DslPart requestBody = new PactDslJsonBody()
                .stringType("username", "newuser")
                .stringType("email", "newuser@example.com")
                .stringType("firstName", "New")
                .stringType("lastName", "User")
                .stringType("password", "SecurePass123!");

        DslPart responseBody = new PactDslJsonBody()
                .integerType("id")
                .stringValue("username", "newuser")
                .stringValue("email", "newuser@example.com")
                .stringValue("firstName", "New")
                .stringValue("lastName", "User")
                .booleanValue("active", true)
                .stringType("createdAt");

        return builder
                .given("system is ready to accept new users")
                .uponReceiving("a request to create a new user")
                .path("/api/users")
                .method("POST")
                .headers(getDefaultHeaders())
                .body(requestBody)
                .willRespondWith()
                .status(201)
                .headers(getResponseHeaders())
                .body(responseBody)
                .toPact();
    }

    /**
     * Define contract for getting list of users
     */
    @Pact(consumer = "QATestSuite")
    public RequestResponsePact getUserListPact(PactDslWithProvider builder) {
        DslPart responseBody = new PactDslJsonBody()
                .array("users")
                    .object()
                        .integerType("id", 1)
                        .stringType("username", "user1")
                        .stringType("email", "user1@example.com")
                    .closeObject()
                    .object()
                        .integerType("id", 2)
                        .stringType("username", "user2")
                        .stringType("email", "user2@example.com")
                    .closeObject()
                .closeArray()
                .integerType("total", 2)
                .integerType("page", 1)
                .integerType("pageSize", 10);

        return builder
                .given("multiple users exist in the system")
                .uponReceiving("a request for user list")
                .path("/api/users")
                .method("GET")
                .matchQuery("page", "\\d+", "1")
                .matchQuery("size", "\\d+", "10")
                .headers(getDefaultHeaders())
                .willRespondWith()
                .status(200)
                .headers(getResponseHeaders())
                .body(responseBody)
                .toPact();
    }

    /**
     * Define contract for user not found scenario
     */
    @Pact(consumer = "QATestSuite")
    public RequestResponsePact userNotFoundPact(PactDslWithProvider builder) {
        DslPart responseBody = new PactDslJsonBody()
                .stringValue("error", "Not Found")
                .stringValue("message", "User not found")
                .integerValue("statusCode", 404)
                .stringType("timestamp");

        return builder
                .given("user with id 99999 does not exist")
                .uponReceiving("a request for non-existent user")
                .path("/api/users/99999")
                .method("GET")
                .headers(getDefaultHeaders())
                .willRespondWith()
                .status(404)
                .headers(getResponseHeaders())
                .body(responseBody)
                .toPact();
    }

    /**
     * Define contract for updating a user
     */
    @Pact(consumer = "QATestSuite")
    public RequestResponsePact updateUserPact(PactDslWithProvider builder) {
        DslPart requestBody = new PactDslJsonBody()
                .stringType("firstName", "Updated")
                .stringType("lastName", "Name");

        DslPart responseBody = new PactDslJsonBody()
                .integerValue("id", 1)
                .stringType("username")
                .stringType("email")
                .stringValue("firstName", "Updated")
                .stringValue("lastName", "Name")
                .booleanType("active")
                .stringType("updatedAt");

        return builder
                .given("user with id 1 exists")
                .uponReceiving("a request to update user 1")
                .path("/api/users/1")
                .method("PATCH")
                .headers(getDefaultHeaders())
                .body(requestBody)
                .willRespondWith()
                .status(200)
                .headers(getResponseHeaders())
                .body(responseBody)
                .toPact();
    }

    // ========== Contract Tests ==========

    @Test
    @PactTestFor(pactMethod = "getUserByIdPact")
    void testGetUserById(MockServer mockServer) {
        RestAssured.baseURI = mockServer.getUrl();

        given()
                .headers(getDefaultHeaders())
        .when()
                .get("/api/users/1")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(1))
                .body("username", notNullValue())
                .body("email", containsString("@"))
                .body("active", equalTo(true))
                .body("address.city", notNullValue());
    }

    @Test
    @PactTestFor(pactMethod = "createUserPact")
    void testCreateUser(MockServer mockServer) {
        RestAssured.baseURI = mockServer.getUrl();

        Map<String, Object> newUser = new HashMap<>();
        newUser.put("username", "newuser");
        newUser.put("email", "newuser@example.com");
        newUser.put("firstName", "New");
        newUser.put("lastName", "User");
        newUser.put("password", "SecurePass123!");

        given()
                .headers(getDefaultHeaders())
                .contentType(ContentType.JSON)
                .body(newUser)
        .when()
                .post("/api/users")
        .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", notNullValue())
                .body("username", equalTo("newuser"))
                .body("email", equalTo("newuser@example.com"))
                .body("active", equalTo(true));
    }

    @Test
    @PactTestFor(pactMethod = "getUserListPact")
    void testGetUserList(MockServer mockServer) {
        RestAssured.baseURI = mockServer.getUrl();

        given()
                .headers(getDefaultHeaders())
                .queryParam("page", 1)
                .queryParam("size", 10)
        .when()
                .get("/api/users")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("users", hasSize(greaterThan(0)))
                .body("total", greaterThanOrEqualTo(1))
                .body("page", equalTo(1));
    }

    @Test
    @PactTestFor(pactMethod = "userNotFoundPact")
    void testUserNotFound(MockServer mockServer) {
        RestAssured.baseURI = mockServer.getUrl();

        given()
                .headers(getDefaultHeaders())
        .when()
                .get("/api/users/99999")
        .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("error", equalTo("Not Found"))
                .body("message", equalTo("User not found"))
                .body("statusCode", equalTo(404));
    }

    @Test
    @PactTestFor(pactMethod = "updateUserPact")
    void testUpdateUser(MockServer mockServer) {
        RestAssured.baseURI = mockServer.getUrl();

        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", "Updated");
        updates.put("lastName", "Name");

        given()
                .headers(getDefaultHeaders())
                .contentType(ContentType.JSON)
                .body(updates)
        .when()
                .patch("/api/users/1")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(1))
                .body("firstName", equalTo("Updated"))
                .body("lastName", equalTo("Name"));
    }

    // ========== Helper Methods ==========

    private Map<String, String> getDefaultHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        return headers;
    }

    private Map<String, String> getResponseHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }
}
