package com.qa.api.payloads;

import com.qa.api.commonUtils.TestData;
import com.qa.api.commonUtils.TestLogger;
import com.qa.api.pojo.PostPatchRequest;
import com.qa.api.pojo.PostRequest;
import java.util.UUID;

public final class PostPayloads {
    private PostPayloads() { }

    public static PostRequest createPost(TestData testData) {
        return buildPost(testData, "create");
    }

    public static PostRequest updatePost(TestData testData) {
        return buildPost(testData, "update");
    }

    public static PostPatchRequest patchPost(TestData testData) {
        TestLogger.info("STEP: Building a PATCH payload with a unique title; other fields are omitted");
        PostPatchRequest payload = new PostPatchRequest();
        payload.setTitle(testData.getString("posts.titlePrefix") + " patch " + UUID.randomUUID());
        return payload;
    }

    private static PostRequest buildPost(TestData testData, String action) {
        TestLogger.info("STEP: Building a dynamic " + action + " payload from environment test data");
        String uniqueValue = UUID.randomUUID().toString();
        PostRequest payload = new PostRequest();
        payload.setUserId(Integer.parseInt(testData.getString("posts.userId")));
        payload.setTitle(testData.getString("posts.titlePrefix") + " " + action + " " + uniqueValue);
        payload.setBody(testData.getString("posts.bodyPrefix") + " " + action + " " + uniqueValue);
        TestLogger.info("STEP: Dynamic post payload ready; title: " + payload.getTitle());
        return payload;
    }
}
