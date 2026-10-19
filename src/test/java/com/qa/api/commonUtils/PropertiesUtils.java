package com.qa.api.commonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class PropertiesUtils {
    private PropertiesUtils() { }

    public static Properties readResource(String resourcePath) {
        TestLogger.info("Reading properties resource: " + resourcePath);
        try (InputStream stream = PropertiesUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalArgumentException("Properties resource not found: " + resourcePath);
            }
            try (Reader reader = new java.io.InputStreamReader(stream, StandardCharsets.UTF_8)) {
                Properties properties = new Properties();
                properties.load(reader);
                TestLogger.info("Properties resource loaded: " + resourcePath);
                return properties;
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read properties resource: " + resourcePath, exception);
        }
    }

    public static Properties readFile(Path file) {
        TestLogger.info("Reading properties file: " + file);
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Properties properties = new Properties();
            properties.load(reader);
            TestLogger.info("Properties file loaded: " + file);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read properties file: " + file, exception);
        }
    }

    /** Updates one key without removing other keys. Creates the file if needed. */
    public static void writeProperty(Path file, String key, String value) {
        TestLogger.info("Writing property '" + key + "' to: " + file);
        Properties properties = Files.exists(file) ? readFile(file) : new Properties();
        properties.setProperty(key, value);
        try {
            FileUtils.createParentFolder(file);
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                properties.store(writer, "Updated by API Automation Framework");
            }
            TestLogger.info("Property written successfully: " + key);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write properties file: " + file, exception);
        }
    }
}
