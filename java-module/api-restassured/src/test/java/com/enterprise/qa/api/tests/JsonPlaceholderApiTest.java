package com.enterprise.qa.api.tests;

import com.enterprise.qa.api.base.BaseAPITest;
import com.enterprise.qa.api.client.ApiClient;
import com.enterprise.qa.api.utils.ApiAssertions;
import io.qameta.allure.*;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API tests for JSONPlaceholder API (https://jsonplaceholder.typicode.com).
 * Demonstrates REST Assured testing patterns.
 */
@Slf4j
@Epic("API Testing")
@Feature("JSONPlaceholder API")
public class JsonPlaceholderApiTest extends BaseAPITest {

    private ApiClient client;

    @BeforeClass
    public void setupClient() {
        client = new ApiClient("https://jsonplaceholder.typicode.com");
    }

    @Test(groups = {"smoke", "api"})
    @Story("GET Endpoints")
    @Description("Verify GET /posts returns a list of posts")
    @Severity(SeverityLevel.CRITICAL)
    public void testGetAllPosts() {
        Response response = client.get("/posts");

        ApiAssertions.assertStatusCode(response, 200);
        ApiAssertions.assertJsonContentType(response);
        ApiAssertions.assertJsonArrayNotEmpty(response, "");

        // Verify array size (JSONPlaceholder returns 100 posts)
        ApiAssertions.assertJsonArraySize(response, "", 100);

        // Verify first post structure
        ApiAssertions.assertJsonPathExists(response, "[0].id");
        ApiAssertions.assertJsonPathExists(response, "[0].title");
        ApiAssertions.assertJsonPathExists(response, "[0].body");
        ApiAssertions.assertJsonPathExists(response, "[0].userId");

        log.info("Successfully retrieved {} posts", response.jsonPath().getList("").size());
    }

    @Test(groups = {"smoke", "api"})
    @Story("GET Endpoints")
    @Description("Verify GET /posts/{id} returns a specific post")
    @Severity(SeverityLevel.CRITICAL)
    public void testGetPostById() {
        int postId = 1;
        Response response = client.get("/posts/" + postId);

        ApiAssertions.assertStatusCode(response, 200);
        ApiAssertions.assertJsonContentType(response);
        ApiAssertions.assertJsonPathEquals(response, "id", postId);
        ApiAssertions.assertNotNull(response, "title");
        ApiAssertions.assertNotNull(response, "body");
        ApiAssertions.assertNotNull(response, "userId");

        log.info("Retrieved post: {}", response.jsonPath().getString("title"));
    }

    @Test(groups = {"smoke", "api"})
    @Story("POST Endpoints")
    @Description("Verify POST /posts creates a new post")
    @Severity(SeverityLevel.CRITICAL)
    public void testCreatePost() {
        Map<String, Object> newPost = new HashMap<>();
        newPost.put("title", "Test Post Title");
        newPost.put("body", "This is the body of the test post");
        newPost.put("userId", 1);

        Response response = client.post("/posts", newPost);

        ApiAssertions.assertStatusCode(response, 201);
        ApiAssertions.assertJsonContentType(response);
        ApiAssertions.assertJsonPathEquals(response, "title", "Test Post Title");
        ApiAssertions.assertJsonPathEquals(response, "body", "This is the body of the test post");
        ApiAssertions.assertJsonPathEquals(response, "userId", 1);
        ApiAssertions.assertJsonPathExists(response, "id");

        log.info("Created post with ID: {}", response.jsonPath().getInt("id"));
    }

    @Test(groups = {"regression", "api"})
    @Story("PUT Endpoints")
    @Description("Verify PUT /posts/{id} updates a post")
    @Severity(SeverityLevel.NORMAL)
    public void testUpdatePost() {
        int postId = 1;
        Map<String, Object> updatedPost = new HashMap<>();
        updatedPost.put("id", postId);
        updatedPost.put("title", "Updated Title");
        updatedPost.put("body", "Updated body content");
        updatedPost.put("userId", 1);

        Response response = client.put("/posts/" + postId, updatedPost);

        ApiAssertions.assertStatusCode(response, 200);
        ApiAssertions.assertJsonPathEquals(response, "id", postId);
        ApiAssertions.assertJsonPathEquals(response, "title", "Updated Title");

        log.info("Updated post {}", postId);
    }

    @Test(groups = {"regression", "api"})
    @Story("PATCH Endpoints")
    @Description("Verify PATCH /posts/{id} partially updates a post")
    @Severity(SeverityLevel.NORMAL)
    public void testPatchPost() {
        int postId = 1;
        Map<String, Object> patchData = new HashMap<>();
        patchData.put("title", "Patched Title");

        Response response = client.patch("/posts/" + postId, patchData);

        ApiAssertions.assertStatusCode(response, 200);
        ApiAssertions.assertJsonPathEquals(response, "title", "Patched Title");

        log.info("Patched post {}", postId);
    }

    @Test(groups = {"regression", "api"})
    @Story("DELETE Endpoints")
    @Description("Verify DELETE /posts/{id} deletes a post")
    @Severity(SeverityLevel.NORMAL)
    public void testDeletePost() {
        int postId = 1;
        Response response = client.delete("/posts/" + postId);

        ApiAssertions.assertStatusCode(response, 200);

        log.info("Deleted post {}", postId);
    }

    @Test(groups = {"smoke", "api"})
    @Story("GET Endpoints")
    @Description("Verify GET /posts with query parameters filters results")
    @Severity(SeverityLevel.NORMAL)
    public void testGetPostsByUserId() {
        int userId = 1;
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);

        Response response = client.get("/posts", params);

        ApiAssertions.assertStatusCode(response, 200);
        ApiAssertions.assertJsonArrayNotEmpty(response, "");

        // Verify all posts belong to the user
        response.jsonPath().getList("userId", Integer.class).forEach(id ->
                assertThat(id).isEqualTo(userId)
        );

        log.info("Found {} posts for user {}", response.jsonPath().getList("").size(), userId);
    }

    @Test(groups = {"regression", "api"})
    @Story("GET Endpoints")
    @Description("Verify GET /posts/{id}/comments returns post comments")
    @Severity(SeverityLevel.NORMAL)
    public void testGetPostComments() {
        int postId = 1;
        Response response = client.get("/posts/" + postId + "/comments");

        ApiAssertions.assertStatusCode(response, 200);
        ApiAssertions.assertJsonArrayNotEmpty(response, "");

        // Verify comment structure
        ApiAssertions.assertJsonPathExists(response, "[0].id");
        ApiAssertions.assertJsonPathExists(response, "[0].name");
        ApiAssertions.assertJsonPathExists(response, "[0].email");
        ApiAssertions.assertJsonPathExists(response, "[0].body");
        ApiAssertions.assertJsonPathEquals(response, "[0].postId", postId);

        log.info("Found {} comments for post {}", response.jsonPath().getList("").size(), postId);
    }

    @Test(groups = {"smoke", "api"})
    @Story("GET Endpoints")
    @Description("Verify GET /users returns list of users")
    @Severity(SeverityLevel.CRITICAL)
    public void testGetUsers() {
        Response response = client.get("/users");

        ApiAssertions.assertStatusCode(response, 200);
        ApiAssertions.assertJsonArrayNotEmpty(response, "");

        // Verify user structure
        ApiAssertions.assertJsonPathExists(response, "[0].id");
        ApiAssertions.assertJsonPathExists(response, "[0].name");
        ApiAssertions.assertJsonPathExists(response, "[0].username");
        ApiAssertions.assertJsonPathExists(response, "[0].email");

        log.info("Found {} users", response.jsonPath().getList("").size());
    }

    @Test(groups = {"regression", "api"})
    @Story("Error Handling")
    @Description("Verify GET /posts/{id} returns 404 for non-existent post")
    @Severity(SeverityLevel.NORMAL)
    public void testGetNonExistentPost() {
        int nonExistentId = 9999;
        Response response = client.get("/posts/" + nonExistentId);

        ApiAssertions.assertStatusCode(response, 404);

        log.info("Correctly returned 404 for non-existent post");
    }

    @Test(groups = {"regression", "api"})
    @Story("Performance")
    @Description("Verify API response time is acceptable")
    @Severity(SeverityLevel.MINOR)
    public void testApiResponseTime() {
        Response response = client.get("/posts/1");

        ApiAssertions.assertStatusCode(response, 200);
        ApiAssertions.assertResponseTime(response, 5000); // Max 5 seconds

        log.info("Response time: {} ms", response.getTime());
    }
}
