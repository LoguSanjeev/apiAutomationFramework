package com.qa.api.tests.assets;

import com.qa.api.commonUtils.TestLogger;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import io.restassured.http.ContentType;
import org.testng.Assert;

/** Reusable assertions. Log before checking so a failed validation is easy to locate. */
public final class ApiAssertions {
    private ApiAssertions() { }

    public static void statusCode(Response response, int expected) {
        TestLogger.info("VALIDATION START: HTTP status should be " + expected);
        Assert.assertEquals(response.statusCode(), expected, "Unexpected HTTP status");
        TestLogger.pass("VALIDATION END: HTTP status is " + expected);
    }

    public static void jsonValue(Response response, String jsonPath, Object expected) {
        TestLogger.info("VALIDATION START: JSON field '" + jsonPath + "'");
        Assert.assertEquals(response.jsonPath().get(jsonPath), expected, "Unexpected JSON value: " + jsonPath);
        TestLogger.pass("VALIDATION END: JSON field '" + jsonPath + "' matches");
    }

    public static void statusMessage(Response response, String expectedMessage) {
        TestLogger.info("VALIDATION START: HTTP response message should be '" + expectedMessage + "'");
        Assert.assertTrue(response.statusLine().endsWith(" " + expectedMessage),
                "Unexpected HTTP response message: " + response.statusLine());
        TestLogger.pass("VALIDATION END: HTTP response message is '" + expectedMessage + "'");
    }

    public static void jsonContentType(Response response) {
        TestLogger.info("VALIDATION START: Response content type is JSON");
        response.then().assertThat().contentType(ContentType.JSON);
        TestLogger.pass("VALIDATION END: Response content type is JSON");
    }

    public static void matchesSchema(Response response, String schemaResource) {
        TestLogger.info("VALIDATION START: Response matches schema " + schemaResource);
        response.then().assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath(schemaResource));
        TestLogger.pass("VALIDATION END: Response matches schema " + schemaResource);
    }

    public static void equals(Object actual, Object expected, String description) {
        TestLogger.info("VALIDATION START: " + description);
        Assert.assertEquals(actual, expected, description);
        TestLogger.pass("VALIDATION END: " + description);
    }

    public static void isTrue(boolean condition, String description) {
        TestLogger.info("VALIDATION START: " + description);
        Assert.assertTrue(condition, description);
        TestLogger.pass("VALIDATION END: " + description);
    }
}
