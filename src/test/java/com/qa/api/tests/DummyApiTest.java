package com.qa.api.tests;

import com.qa.api.base.BaseTest;
import com.qa.api.client.ApiClient;
import com.qa.api.commonUtils.TestLogger;
import com.qa.api.payloads.UserPayloads;
import com.qa.api.pojo.UserRequest;
import com.qa.api.pojo.UserResponse;
import com.qa.api.tests.assets.ApiAssertions;
import com.qa.api.tests.support.DummyApiServer;
import io.restassured.response.Response;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class DummyApiTest extends BaseTest {
    private final DummyApiServer dummyApi = new DummyApiServer();

    @BeforeClass(alwaysRun = true)
    public void startDummyApi() throws Exception {
        dummyApi.start(testData);
        // Only this demonstration class uses a local API instead of the configured URL.
        apiClient = new ApiClient(dummyApi.getBaseUrl(), config);
    }

    @Test(groups = {"smoke", "regression"}, description = "Check that the dummy API is healthy")
    public void healthCheck() {
        Response response = apiClient.get("/health");
        ApiAssertions.statusCode(response, 200);
        ApiAssertions.jsonValue(response, "status", "UP");
    }

    @Test(groups = {"smoke", "regression"}, description = "Create a user with a POJO request body")
    public void createUser() {
        UserRequest payload = UserPayloads.createUser(testData);
        Response response = apiClient.post("/users", payload);

        ApiAssertions.statusCode(response, 201);
        ApiAssertions.matchesSchema(response, "schemas/user-response.schema.json");
        TestLogger.info("Converting response JSON into UserResponse POJO");
        UserResponse user = response.as(UserResponse.class);
        ApiAssertions.equals(user.getName(), payload.getName(), "Created user name matches request");
        ApiAssertions.equals(user.getJob(), payload.getJob(), "Created user job matches request");
        ApiAssertions.equals(user.getId(), testData.getString("existingUser.id"), "Created user ID matches");
    }

    @Test(groups = "regression", description = "Fetch an existing dummy user")
    public void getUser() {
        Response response = apiClient.request()
                .pathParam("id", testData.getString("existingUser.id"))
                .get("/users/{id}");

        ApiAssertions.statusCode(response, 200);
        ApiAssertions.matchesSchema(response, "schemas/user-response.schema.json");
        ApiAssertions.jsonValue(response, "name", testData.getString("createUser.name"));
        ApiAssertions.jsonValue(response, "job", testData.getString("createUser.job"));
    }

    @Test(groups = "regression", description = "Check an unknown user returns HTTP 404")
    public void unknownUser() {
        Response response = apiClient.get("/users/does-not-exist");
        ApiAssertions.statusCode(response, 404);
        ApiAssertions.jsonValue(response, "message", "Not found");
    }

    @AfterClass(alwaysRun = true)
    public void stopDummyApi() {
        dummyApi.stop();
    }
}
