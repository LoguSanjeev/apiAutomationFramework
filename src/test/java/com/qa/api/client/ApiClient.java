package com.qa.api.client;

import com.qa.api.commonUtils.EnvironmentConfig;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class ApiClient {
    private final String baseUrl;
    private final RestAssuredConfig requestConfig;

    public ApiClient(String baseUrl, EnvironmentConfig config) {
        this.baseUrl = baseUrl;
        requestConfig = RestAssuredConfig.config().httpClient(HttpClientConfig.httpClientConfig()
                .setParam("http.connection.timeout", config.getPositiveInt("connection.timeout.ms"))
                .setParam("http.socket.timeout", config.getPositiveInt("read.timeout.ms")));
    }

    /** A fresh request each time avoids leaking bodies or headers between tests. */
    public RequestSpecification request() {
        return RestAssured.given()
                .baseUri(baseUrl)
                .config(requestConfig)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .filter(new ApiLoggingFilter());
    }

    public Response get(String path) {
        return request()
                .when().get(path)
                .then().extract().response();
    }

    public Response post(String path, Object payload) {
        return request().body(payload)
                .when().post(path)
                .then().extract().response();
    }

    public Response put(String path, Object payload) {
        return request().body(payload)
                .when().put(path)
                .then().extract().response();
    }

    public Response patch(String path, Object payload) {
        return request().body(payload)
                .when().patch(path)
                .then().extract().response();
    }

    public Response delete(String path) {
        return request()
                .when().delete(path)
                .then().extract().response();
    }
}
