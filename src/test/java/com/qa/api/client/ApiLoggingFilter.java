package com.qa.api.client;

import com.qa.api.commonUtils.TestLogger;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import java.util.concurrent.TimeUnit;

/** Logs each HTTP action without copying headers, query values, or bodies into logs. */
public class ApiLoggingFilter implements Filter {
    @Override
    public Response filter(FilterableRequestSpecification request,
                           FilterableResponseSpecification responseSpecification,
                           FilterContext context) {
        String action = request.getMethod() + " " + request.getUserDefinedPath();
        TestLogger.info("HTTP REQUEST START: " + action);
        long start = System.nanoTime();
        try {
            Response response = context.next(request, responseSpecification);
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            TestLogger.info("HTTP REQUEST END: " + action + " | Status: "
                    + response.statusCode() + " | Duration: " + elapsed + " ms");
            return response;
        } catch (RuntimeException exception) {
            TestLogger.fail("HTTP REQUEST FAILED: " + action, exception);
            throw exception;
        }
    }
}
