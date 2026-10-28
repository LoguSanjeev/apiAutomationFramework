package com.qa.api.commonUtils;

import java.util.Map;

public class TestData {
    private final Map<?, ?> data;

    public TestData(String environment) {
        Map<?, ?> allData = YamlUtils.readResource("testdata/test-data.yml");
        Object environmentData = allData.get(environment);
        if (!(environmentData instanceof Map)) {
            throw new IllegalArgumentException("Test data is missing for environment: " + environment);
        }
        data = (Map<?, ?>) environmentData;
        TestLogger.info("Selected test data for environment: " + environment);
    }

    /** Example: getString("posts.titlePrefix"). */
    public String getString(String keyPath) {
        TestLogger.info("Reading test data key: " + keyPath);
        Object value = data;
        for (String key : keyPath.split("\\.")) {
            if (!(value instanceof Map)) {
                throw new IllegalArgumentException("Invalid test data path: " + keyPath);
            }
            value = ((Map<?, ?>) value).get(key);
        }
        if (value == null || value instanceof Map || value instanceof Iterable) {
            throw new IllegalArgumentException("Missing or non-scalar test data: " + keyPath);
        }
        return String.valueOf(value);
    }
}
