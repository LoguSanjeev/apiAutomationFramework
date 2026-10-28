package com.qa.api.tests.assets;

import com.qa.api.commonUtils.TestLogger;
import io.restassured.response.Response;
import org.testng.Assert;

/** Simple assertion chaining: each method checks one thing, logs it, and returns this object. */
public final class ResponseAssert {
    private final Response response;

    private ResponseAssert(Response response) {
        if (response == null) {
            throw new IllegalArgumentException("Response must not be null");
        }
        this.response = response;
    }

    public static ResponseAssert assertThat(Response response) {
        return new ResponseAssert(response);
    }

    public ResponseAssert returns_200_OK() {
        return hasStatusCode(200).hasStatusMessage("OK");
    }

    public ResponseAssert returns_201_Created() {
        return hasStatusCode(201).hasStatusMessage("Created");
    }

    public ResponseAssert returns_404_NotFound() {
        return hasStatusCode(404).hasStatusMessage("Not Found");
    }

    public ResponseAssert hasStatusCode(int expected) {
        ApiAssertions.statusCode(response, expected);
        return this;
    }

    public ResponseAssert hasStatusMessage(String expected) {
        ApiAssertions.statusMessage(response, expected);
        return this;
    }

    public ResponseAssert hasHeaderApplicationJson() {
        ApiAssertions.jsonContentType(response);
        return this;
    }

    public ResponseAssert isWithinAcceptedTimeLimit(long maximumMilliseconds) {
        if (maximumMilliseconds <= 0) {
            throw new IllegalArgumentException("Response time limit must be greater than zero");
        }
        TestLogger.info("VALIDATION START: Response time must be within " + maximumMilliseconds + " ms");
        long actualMilliseconds = response.time();
        Assert.assertTrue(actualMilliseconds >= 0 && actualMilliseconds <= maximumMilliseconds,
                "Response time " + actualMilliseconds + " ms exceeds limit " + maximumMilliseconds
                        + " ms or was not recorded");
        TestLogger.pass("VALIDATION END: Response time " + actualMilliseconds + " ms is within the limit");
        return this;
    }

    public ResponseAssert hasValidSchema(String schemaResource) {
        ApiAssertions.matchesSchema(response, schemaResource);
        return this;
    }

    public ResponseAssert hasJsonValue(String jsonPath, Object expected) {
        ApiAssertions.jsonValue(response, jsonPath, expected);
        return this;
    }

    public ResponseAssert hasNonBlankJsonValue(String jsonPath) {
        TestLogger.info("VALIDATION START: JSON field '" + jsonPath + "' must not be blank");
        String value = response.jsonPath().getString(jsonPath);
        Assert.assertTrue(value != null && !value.isBlank(), "JSON field must not be blank: " + jsonPath);
        TestLogger.pass("VALIDATION END: JSON field '" + jsonPath + "' is not blank");
        return this;
    }

    public ResponseAssert hasPositiveJsonNumber(String jsonPath) {
        TestLogger.info("VALIDATION START: JSON field '" + jsonPath + "' must be a positive number");
        Number value = response.jsonPath().get(jsonPath);
        Assert.assertTrue(value != null && value.doubleValue() > 0, "Expected a positive number: " + jsonPath);
        TestLogger.pass("VALIDATION END: JSON field '" + jsonPath + "' is a positive number");
        return this;
    }

    public ResponseAssert hasEmptyJsonObject() {
        TestLogger.info("VALIDATION START: Response body must be an empty JSON object");
        Assert.assertTrue(response.jsonPath().getMap("$").isEmpty(), "Expected an empty JSON object");
        TestLogger.pass("VALIDATION END: Response body is an empty JSON object");
        return this;
    }

    public ResponseAssert hasEmptyJsonList() {
        TestLogger.info("VALIDATION START: Response body must be an empty JSON list");
        Assert.assertTrue(response.jsonPath().getList("$").isEmpty(), "Expected an empty JSON list");
        TestLogger.pass("VALIDATION END: Response body is an empty JSON list");
        return this;
    }

    public ResponseAssert hasNonEmptyJsonList() {
        TestLogger.info("VALIDATION START: Response body must contain at least one item");
        Assert.assertTrue(!response.jsonPath().getList("$").isEmpty(), "Expected at least one item in the JSON list");
        TestLogger.pass("VALIDATION END: Response body contains at least one item");
        return this;
    }
}
