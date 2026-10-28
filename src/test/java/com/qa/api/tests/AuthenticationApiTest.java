package com.qa.api.tests;

import static io.restassured.RestAssured.given;

import com.qa.api.base.BaseTest;
import com.qa.api.client.ApiClient;
import com.qa.api.commonUtils.TestLogger;
import com.qa.api.endpoints.Endpoints;
import com.qa.api.pojo.AuthLoginRequest;
import com.qa.api.pojo.AuthRefreshRequest;
import com.qa.api.pojo.AuthRegisterRequest;
import com.qa.api.schemas.Schemas;
import com.qa.api.tests.assets.ApiAssertions;
import com.qa.api.tests.assets.ResponseAssert;
import io.restassured.response.Response;
import java.util.Map;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Authentication flow tests for the QA Automation Labs API playground. */
public class AuthenticationApiTest extends BaseTest {
    private static final String AUTH_BASE_URL = "https://api.qaautomationlabs.com/v1";
    private static final String DEMO_EMAIL = "qa@demo.io";
    private static final String DEMO_PASSWORD = "Password123";
    private static final String REGISTER_NAME = "New User";
    private static final String REGISTER_EMAIL = "new.user@example.com";

    private ApiClient authApiClient;
    private String loginAccessToken;
    private String refreshToken;
    private String refreshedAccessToken;

    @BeforeClass(alwaysRun = true)
    public void prepareAuthClient() {
        authApiClient = new ApiClient(AUTH_BASE_URL, config);
    }

    @Test(groups = {"qa-auth", "smoke", "regression"},
            description = "Register a user and validate the created user and access token")
    public void registerUser() {
        AuthRegisterRequest payload =
                new AuthRegisterRequest(REGISTER_NAME, REGISTER_EMAIL, DEMO_PASSWORD);

        TestLogger.info("STEP: Register a new user");
        Response response =
                given()
                    .spec(authApiClient.request())
                    .body(payload)
                .when()
                    .post(Endpoints.AUTH_REGISTER)
                .then()
                    .extract().response();

        TestLogger.info("STEP: Validate registration status, schema, submitted data, and generated values");
        ResponseAssert.assertThat(response)
                .returns_201_Created()
                .hasHeaderApplicationJson()
                .isWithinAcceptedTimeLimit(responseTimeLimit)
                .hasValidSchema(Schemas.AUTH_REGISTER_RESPONSE)
                .hasJsonValue("data.name", payload.getName())
                .hasJsonValue("data.email", payload.getEmail())
                .hasJsonValue("data.role", "user")
                .hasPositiveJsonNumber("data.id")
                .hasNonBlankJsonValue("data.accessToken")
                .hasNonBlankJsonValue("data.createdAt")
                .hasNonBlankJsonValue("meta.requestId");
        assertJwt(response.jsonPath().getString("data.accessToken"), "Registration access token");
    }

    @Test(groups = {"qa-auth", "smoke", "regression"},
            description = "Login with the demo user and capture access and refresh tokens")
    public void login() {
        AuthLoginRequest payload = new AuthLoginRequest(DEMO_EMAIL, DEMO_PASSWORD);

        TestLogger.info("STEP: Login with the documented demo credentials");
        Response response =
                given()
                    .spec(authApiClient.request())
                    .body(payload)
                .when()
                    .post(Endpoints.AUTH_LOGIN)
                .then()
                    .extract().response();

        TestLogger.info("STEP: Validate login data and capture both tokens for later requests");
        ResponseAssert.assertThat(response)
                .returns_200_OK()
                .hasHeaderApplicationJson()
                .isWithinAcceptedTimeLimit(responseTimeLimit)
                .hasValidSchema(Schemas.AUTH_LOGIN_RESPONSE)
                .hasJsonValue("data.tokenType", "Bearer")
                .hasJsonValue("data.expiresIn", 3600)
                .hasJsonValue("data.user.id", 1)
                .hasJsonValue("data.user.email", payload.getEmail())
                .hasJsonValue("data.user.name", "QA Demo User")
                .hasJsonValue("data.user.role", "admin")
                .hasNonBlankJsonValue("data.accessToken")
                .hasNonBlankJsonValue("data.refreshToken")
                .hasNonBlankJsonValue("meta.requestId");

        loginAccessToken = response.jsonPath().getString("data.accessToken");
        refreshToken = response.jsonPath().getString("data.refreshToken");
        assertJwt(loginAccessToken, "Login access token");
        assertJwt(refreshToken, "Login refresh token");
    }

    @Test(dependsOnMethods = "login", groups = {"qa-auth", "smoke", "regression"},
            description = "Use the login refresh token to obtain a new access token")
    public void refreshAccessToken() {
        TestLogger.info("STEP: Send the refresh token captured from login");
        Response response =
                given()
                    .spec(authApiClient.request())
                    .body(new AuthRefreshRequest(refreshToken))
                .when()
                    .post(Endpoints.AUTH_REFRESH)
                .then()
                    .extract().response();

        TestLogger.info("STEP: Validate and capture the refreshed access token");
        ResponseAssert.assertThat(response)
                .returns_200_OK()
                .hasHeaderApplicationJson()
                .isWithinAcceptedTimeLimit(responseTimeLimit)
                .hasValidSchema(Schemas.AUTH_REFRESH_RESPONSE)
                .hasJsonValue("data.tokenType", "Bearer")
                .hasJsonValue("data.expiresIn", 3600)
                .hasNonBlankJsonValue("data.accessToken")
                .hasNonBlankJsonValue("meta.requestId");

        refreshedAccessToken = response.jsonPath().getString("data.accessToken");
        assertJwt(refreshedAccessToken, "Refreshed access token");
        ApiAssertions.isTrue(!refreshedAccessToken.equals(refreshToken),
                "Refresh response contains an access token, not the submitted refresh token");
        ApiAssertions.isTrue(!refreshedAccessToken.equals(loginAccessToken),
                "Refresh endpoint issued a new access token");
    }

    @Test(dependsOnMethods = "refreshAccessToken", groups = {"qa-auth", "smoke", "regression"},
            description = "Use the refreshed Bearer token to read the current user")
    public void getCurrentUserWithBearerToken() {
        TestLogger.info("STEP: Call the protected current-user endpoint with the refreshed access token");
        Response response =
                given()
                    .spec(authApiClient.request())
                    .auth().oauth2(refreshedAccessToken)
                .when()
                    .get(Endpoints.AUTH_ME)
                .then()
                    .extract().response();

        TestLogger.info("STEP: Validate authorization succeeded and the token belongs to the demo user");
        ResponseAssert.assertThat(response)
                .returns_200_OK()
                .hasHeaderApplicationJson()
                .isWithinAcceptedTimeLimit(responseTimeLimit)
                .hasValidSchema(Schemas.AUTH_ME_RESPONSE)
                .hasJsonValue("data.id", 1)
                .hasJsonValue("data.email", DEMO_EMAIL)
                .hasJsonValue("data.role", "admin")
                .hasNonBlankJsonValue("data.tokenExpiresAt")
                .hasNonBlankJsonValue("meta.requestId");
    }

    @Test(groups = {"qa-auth", "regression", "negative"},
            description = "Reject access to the current-user endpoint when the Bearer token is missing")
    public void getCurrentUserWithoutBearerTokenReturns401() {
        TestLogger.info("STEP: Call the protected current-user endpoint without Authorization");
        Response response =
                given()
                    .spec(authApiClient.request())
                .when()
                    .get(Endpoints.AUTH_ME)
                .then()
                    .extract().response();

        TestLogger.info("STEP: Validate the missing Bearer token is rejected");
        ResponseAssert.assertThat(response)
                .hasStatusCode(401)
                .hasStatusMessage("Unauthorized")
                .hasHeaderApplicationJson()
                .isWithinAcceptedTimeLimit(responseTimeLimit)
                .hasValidSchema(Schemas.AUTH_ERROR_RESPONSE)
                .hasJsonValue("error.code", "UNAUTHORIZED")
                .hasJsonValue("error.status", 401)
                .hasNonBlankJsonValue("error.message")
                .hasNonBlankJsonValue("meta.requestId");
    }

    @Test(dependsOnMethods = "getCurrentUserWithBearerToken",
            groups = {"qa-auth", "smoke", "regression"},
            description = "Logout and validate the success message")
    public void logout() {
        TestLogger.info("STEP: Logout after completing the authenticated flow");
        Response response =
                given()
                    .spec(authApiClient.request())
                    .body(Map.of())
                .when()
                    .post(Endpoints.AUTH_LOGOUT)
                .then()
                    .extract().response();

        TestLogger.info("STEP: Validate logout status, schema, and confirmation message");
        ResponseAssert.assertThat(response)
                .returns_200_OK()
                .hasHeaderApplicationJson()
                .isWithinAcceptedTimeLimit(responseTimeLimit)
                .hasValidSchema(Schemas.AUTH_LOGOUT_RESPONSE)
                .hasJsonValue("data.message", "Logged out. Discard your tokens client-side.")
                .hasNonBlankJsonValue("meta.requestId");
    }

    private void assertJwt(String token, String description) {
        ApiAssertions.isTrue(token != null && token.split("\\.").length == 3,
                description + " has the three JWT sections");
    }
}
