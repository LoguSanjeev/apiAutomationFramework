package com.qa.api.payloads;

import com.qa.api.commonUtils.TestData;
import com.qa.api.commonUtils.TestLogger;
import com.qa.api.pojo.UserRequest;

public final class UserPayloads {
    private UserPayloads() { }

    public static UserRequest createUser(TestData testData) {
        TestLogger.info("Building create-user request payload from YAML test data");
        UserRequest request = new UserRequest();
        request.setName(testData.getString("createUser.name"));
        request.setJob(testData.getString("createUser.job"));
        TestLogger.info("Create-user payload ready");
        return request;
    }
}
