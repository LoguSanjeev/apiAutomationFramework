package com.qa.api.tests;

import static io.restassured.RestAssured.given;

import com.qa.api.base.BaseTest;
import com.qa.api.commonUtils.TestLogger;
import com.qa.api.endpoints.Endpoints;
import com.qa.api.payloads.PostPayloads;
import com.qa.api.pojo.PostPatchRequest;
import com.qa.api.pojo.PostRequest;
import com.qa.api.pojo.PostResponse;
import com.qa.api.schemas.Schemas;
import com.qa.api.tests.assets.ApiAssertions;
import com.qa.api.tests.assets.ResponseAssert;
import io.restassured.response.Response;
import java.util.List;
import org.testng.annotations.Test;

/** Public API contract tests. JSONPlaceholder simulates writes without saving changes. */
public class JsonPlaceholderPostTest extends BaseTest {
    @Test(groups = {"jsonplaceholder", "smoke", "regression"},
            description = "GET an existing post and validate its fields, message, and schema")
    public void getExistingPost() {
        TestLogger.info("STEP: JSONPlaceholder is public; 401/403 authentication scenarios are not applicable");
        int postId = existingPostId();

        TestLogger.info("STEP: Send GET request for post " + postId);
        Response response =
                given()
                    .spec(apiClient.request())
                    .pathParam("id", postId)
                .when()
                    .get(Endpoints.POST_BY_ID)
                .then()
                    .extract().response();

        TestLogger.info("STEP: Validate success status, JSON header, response time, schema, and post details");
        ResponseAssert.assertThat(response)
                .returns_200_OK()
                .hasHeaderApplicationJson()
                .isWithinAcceptedTimeLimit(responseTimeLimit)
                .hasValidSchema(Schemas.POST_RESPONSE)
                .hasJsonValue("id", postId)
                .hasJsonValue("userId", Integer.parseInt(testData.getString("posts.userId")))
                .hasNonBlankJsonValue("title")
                .hasNonBlankJsonValue("body");
    }

    @Test(groups = {"jsonplaceholder", "regression"},
            description = "GET posts filtered by user and validate every returned post")
    public void getPostsForUser() {
        int userId = Integer.parseInt(testData.getString("posts.userId"));
        TestLogger.info("STEP: GET posts belonging to user " + userId);
        Response response =
                given()
                    .spec(apiClient.request())
                    .queryParam("userId", userId)
                .when()
                    .get(Endpoints.POSTS)
                .then()
                    .extract().response();

        TestLogger.info("STEP: Validate the filtered list response");
        ResponseAssert.assertThat(response)
                .returns_200_OK()
                .hasHeaderApplicationJson()
                .isWithinAcceptedTimeLimit(responseTimeLimit)
                .hasValidSchema(Schemas.POST_LIST)
                .hasNonEmptyJsonList();

        TestLogger.info("STEP: Convert the filtered response into POJOs and verify each post's user ID");
        List<PostResponse> posts = response.jsonPath().getList("$", PostResponse.class);
        for (PostResponse post : posts) {
            ApiAssertions.equals(post.getUserId(), userId, "Post " + post.getId() + " belongs to the requested user");
        }
    }

    @Test(groups = {"jsonplaceholder", "smoke", "regression"},
            description = "POST a dynamic payload, then GET the returned ID to confirm the write is simulated")
    public void createPostAndCheckItIsNotPersisted() {
        PostRequest payload = PostPayloads.createPost(testData);
        TestLogger.info("STEP: POST a new post with a unique title and body");
        Response response =
                given()
                    .spec(apiClient.request())
                    .body(payload)
                .when()
                    .post(Endpoints.POSTS)
                .then()
                    .extract().response();

        TestLogger.info("STEP: Validate the created response and compare it with the request payload");
        ResponseAssert.assertThat(response)
                .returns_201_Created()
                .hasHeaderApplicationJson()
                .isWithinAcceptedTimeLimit(responseTimeLimit)
                .hasValidSchema(Schemas.POST_RESPONSE)
                .hasJsonValue("userId", payload.getUserId())
                .hasJsonValue("title", payload.getTitle())
                .hasJsonValue("body", payload.getBody())
                .hasPositiveJsonNumber("id");

        int createdId = response.jsonPath().getInt("id");
        TestLogger.info("STEP: GET returned post ID " + createdId + " after POST");
        TestLogger.info("EXPECTED LIMITATION: JSONPlaceholder does not save new posts; GET must return 404");
        Response followUp =
                given()
                    .spec(apiClient.request())
                    .pathParam("id", createdId)
                .when()
                    .get(Endpoints.POST_BY_ID)
                .then()
                    .extract().response();
        ResponseAssert.assertThat(followUp)
                .returns_404_NotFound()
                .hasHeaderApplicationJson()
                .isWithinAcceptedTimeLimit(responseTimeLimit)
                .hasValidSchema(Schemas.EMPTY_OBJECT)
                .hasEmptyJsonObject();
    }

    @Test(groups = {"jsonplaceholder", "regression"},
            description = "PUT a dynamic replacement, then GET and verify the original stored post remains")
    public void updatePostAndCheckOriginalRemains() {
        int postId = existingPostId();
        TestLogger.info("STEP: Read the original post before PUT");
        PostResponse original = getValidatedPost(postId).as(PostResponse.class);
        PostRequest payload = PostPayloads.updatePost(testData);
        ApiAssertions.isTrue(!payload.getTitle().equals(original.getTitle()), "PUT title differs from the stored title");
        ApiAssertions.isTrue(!payload.getBody().equals(original.getBody()), "PUT body differs from the stored body");

        TestLogger.info("STEP: PUT a complete replacement for post " + postId);
        Response response =
                given()
                    .spec(apiClient.request())
                    .pathParam("id", postId)
                    .body(payload)
                .when()
                    .put(Endpoints.POST_BY_ID)
                .then()
                    .extract().response();

        TestLogger.info("STEP: Validate the replacement response and all submitted fields");
        ResponseAssert.assertThat(response)
                .returns_200_OK()
                .hasHeaderApplicationJson()
                .isWithinAcceptedTimeLimit(responseTimeLimit)
                .hasValidSchema(Schemas.POST_RESPONSE)
                .hasJsonValue("id", postId)
                .hasJsonValue("userId", payload.getUserId())
                .hasJsonValue("title", payload.getTitle())
                .hasJsonValue("body", payload.getBody());

        TestLogger.info("STEP: GET the same ID after PUT");
        TestLogger.info("EXPECTED LIMITATION: PUT is simulated; the stored post must still match the original");
        assertOriginalPostRemains(postId, original);
    }

    @Test(groups = {"jsonplaceholder", "regression"},
            description = "PATCH only the title, validate unchanged fields, then GET the original stored post")
    public void patchPostAndCheckOriginalRemains() {
        int postId = existingPostId();
        TestLogger.info("STEP: Read the original post before PATCH");
        PostResponse original = getValidatedPost(postId).as(PostResponse.class);
        PostPatchRequest payload = PostPayloads.patchPost(testData);
        ApiAssertions.isTrue(!payload.getTitle().equals(original.getTitle()), "PATCH title differs from the stored title");

        TestLogger.info("STEP: PATCH only the title of post " + postId);
        Response response =
                given()
                    .spec(apiClient.request())
                    .pathParam("id", postId)
                    .body(payload)
                .when()
                    .patch(Endpoints.POST_BY_ID)
                .then()
                    .extract().response();

        TestLogger.info("STEP: Validate the new title and verify the other response fields are unchanged");
        ResponseAssert.assertThat(response)
                .returns_200_OK()
                .hasHeaderApplicationJson()
                .isWithinAcceptedTimeLimit(responseTimeLimit)
                .hasValidSchema(Schemas.POST_RESPONSE)
                .hasJsonValue("id", postId)
                .hasJsonValue("title", payload.getTitle())
                .hasJsonValue("userId", original.getUserId())
                .hasJsonValue("body", original.getBody());

        TestLogger.info("STEP: GET the same ID after PATCH");
        TestLogger.info("EXPECTED LIMITATION: PATCH is simulated; the stored post must still match the original");
        assertOriginalPostRemains(postId, original);
    }

    @Test(groups = {"jsonplaceholder", "regression"},
            description = "DELETE an existing post, validate its empty response, then GET the unchanged post")
    public void deletePostAndCheckOriginalRemains() {
        int postId = existingPostId();
        TestLogger.info("STEP: Read the original post before DELETE");
        PostResponse original = getValidatedPost(postId).as(PostResponse.class);
        TestLogger.info("STEP: DELETE post " + postId);
        Response response =
                given()
                    .spec(apiClient.request())
                    .pathParam("id", postId)
                .when()
                    .delete(Endpoints.POST_BY_ID)
                .then()
                    .extract().response();

        TestLogger.info("STEP: Validate the delete status and empty response body");
        ResponseAssert.assertThat(response)
                .returns_200_OK()
                .hasHeaderApplicationJson()
                .isWithinAcceptedTimeLimit(responseTimeLimit)
                .hasValidSchema(Schemas.EMPTY_OBJECT)
                .hasEmptyJsonObject();

        TestLogger.info("STEP: GET the same ID after DELETE");
        TestLogger.info("EXPECTED LIMITATION: DELETE is simulated; the stored post must still be present");
        assertOriginalPostRemains(postId, original);
    }

    @Test(groups = {"jsonplaceholder", "regression", "negative"},
            description = "GET a missing post and validate HTTP 404, message, and empty-object schema")
    public void getMissingPostReturns404() {
        String missingId = testData.getString("posts.missingId");
        TestLogger.info("STEP: GET a post that does not exist: " + missingId);
        Response response =
                given()
                    .spec(apiClient.request())
                    .pathParam("id", missingId)
                .when()
                    .get(Endpoints.POST_BY_ID)
                .then()
                    .extract().response();

        TestLogger.info("STEP: Validate the missing-post response");
        ResponseAssert.assertThat(response)
                .returns_404_NotFound()
                .hasHeaderApplicationJson()
                .isWithinAcceptedTimeLimit(responseTimeLimit)
                .hasValidSchema(Schemas.EMPTY_OBJECT)
                .hasEmptyJsonObject();
    }

    @Test(groups = {"jsonplaceholder", "regression", "negative"},
            description = "GET an unknown route and validate HTTP 404, message, and empty-object schema")
    public void getInvalidRouteReturns404() {
        TestLogger.info("STEP: GET an API route that does not exist");
        Response response =
                given()
                    .spec(apiClient.request())
                .when()
                    .get(Endpoints.INVALID_ROUTE)
                .then()
                    .extract().response();

        TestLogger.info("STEP: Validate the unknown-route response");
        ResponseAssert.assertThat(response)
                .returns_404_NotFound()
                .hasHeaderApplicationJson()
                .isWithinAcceptedTimeLimit(responseTimeLimit)
                .hasValidSchema(Schemas.EMPTY_OBJECT)
                .hasEmptyJsonObject();
    }

    @Test(groups = {"jsonplaceholder", "regression", "negative"},
            description = "Filter posts by a missing user and validate HTTP 200 with an empty list")
    public void missingUserReturnsEmptyPostList() {
        TestLogger.info("STEP: GET posts using a user ID that does not exist");
        Response response =
                given()
                    .spec(apiClient.request())
                    .queryParam("userId", testData.getString("posts.missingId"))
                .when()
                    .get(Endpoints.POSTS)
                .then()
                    .extract().response();

        TestLogger.info("STEP: Validate the response is successful and contains no posts");
        ResponseAssert.assertThat(response)
                .returns_200_OK()
                .hasHeaderApplicationJson()
                .isWithinAcceptedTimeLimit(responseTimeLimit)
                .hasValidSchema(Schemas.POST_LIST)
                .hasEmptyJsonList();
    }

    private int existingPostId() {
        return Integer.parseInt(testData.getString("posts.existingId"));
    }

    private Response getValidatedPost(int postId) {
        TestLogger.info("STEP: GET and validate post " + postId);
        Response response =
                given()
                    .spec(apiClient.request())
                    .pathParam("id", postId)
                .when()
                    .get(Endpoints.POST_BY_ID)
                .then()
                    .extract().response();
        ResponseAssert.assertThat(response)
                .returns_200_OK()
                .hasHeaderApplicationJson()
                .isWithinAcceptedTimeLimit(responseTimeLimit)
                .hasValidSchema(Schemas.POST_RESPONSE)
                .hasJsonValue("id", postId);
        return response;
    }

    private void assertOriginalPostRemains(int postId, PostResponse original) {
        Response followUp = getValidatedPost(postId);
        TestLogger.info("STEP: Compare every stored field with the original post");
        ResponseAssert.assertThat(followUp)
                .hasJsonValue("userId", original.getUserId())
                .hasJsonValue("title", original.getTitle())
                .hasJsonValue("body", original.getBody());
    }
}
