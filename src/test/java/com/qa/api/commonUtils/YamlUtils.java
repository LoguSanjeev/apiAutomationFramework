package com.qa.api.commonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public final class YamlUtils {
    private YamlUtils() { }

    public static Map<?, ?> readResource(String resourcePath) {
        TestLogger.info("Reading YAML resource: " + resourcePath);
        try (InputStream stream = YamlUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalArgumentException("YAML resource not found: " + resourcePath);
            }
            LoaderOptions options = new LoaderOptions();
            options.setAllowDuplicateKeys(false);
            Yaml yaml = new Yaml(new SafeConstructor(options));
            Object data = yaml.load(stream);
            if (!(data instanceof Map)) {
                throw new IllegalArgumentException("YAML root must contain key/value data: " + resourcePath);
            }
            TestLogger.info("YAML resource loaded: " + resourcePath);
            return (Map<?, ?>) data;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read YAML resource: " + resourcePath, exception);
        }
    }
}
