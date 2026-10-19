package com.qa.api.commonUtils;

import java.util.Locale;
import java.util.Properties;

public class EnvironmentConfig {
    public static final String DEFAULT_ENVIRONMENT = "development";

    private final String environment;
    private final Properties properties;

    public EnvironmentConfig() {
        String selectedEnvironment = System.getProperty("env", DEFAULT_ENVIRONMENT)
                .trim().toLowerCase(Locale.ROOT);
        switch (selectedEnvironment) {
            case "development":
                environment = "dev";
                break;
            case "staging":
                environment = "stag";
                break;
            case "production":
                environment = "prod";
                break;
            default:
                throw new IllegalArgumentException("Invalid environment: " + selectedEnvironment
                        + ". Use -Denv=development, -Denv=staging, or -Denv=production.");
        }
        properties = PropertiesUtils.readResource("environments/" + environment + ".properties");
        TestLogger.info("Environment configuration ready: " + selectedEnvironment + " (" + environment + ")");
    }

    public String getEnvironment() {
        return environment;
    }

    /** A JVM property such as -Dbase.url takes priority over the file. */
    public String get(String key) {
        String value = System.getProperty(key, properties.getProperty(key));
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing environment property: " + key);
        }
        TestLogger.info("Read configuration key: " + key);
        return value.trim();
    }

    public int getPositiveInt(String key) {
        try {
            int value = Integer.parseInt(get(key));
            if (value <= 0) {
                throw new IllegalArgumentException("Property must be greater than zero: " + key);
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Property must be a whole number: " + key, exception);
        }
    }
}
