package com.qa.api.tests;

import com.qa.api.base.BaseTest;
import com.qa.api.commonUtils.CsvUtils;
import com.qa.api.commonUtils.FileUtils;
import com.qa.api.commonUtils.PropertiesUtils;
import com.qa.api.tests.assets.ApiAssertions;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.testng.annotations.Test;

public class CommonUtilsTest extends BaseTest {
    private final Path output = Path.of("target", "utility-test-output");

    @Test(groups = "regression", description = "Property updates preserve other keys and Unicode")
    public void propertiesRoundTrip() {
        Path file = output.resolve("sample.properties");
        PropertiesUtils.writeProperty(file, "user.name", "QA User");
        PropertiesUtils.writeProperty(file, "user.job", "Ingénieur QA");
        PropertiesUtils.writeProperty(file, "user.name", "Updated User");
        Properties data = PropertiesUtils.readFile(file);
        ApiAssertions.equals(data.getProperty("user.name"), "Updated User", "Property update is saved");
        ApiAssertions.equals(data.getProperty("user.job"), "Ingénieur QA", "Other properties are preserved");
    }

    @Test(groups = "regression", description = "Text can be written, appended, read, and replaced")
    public void textRoundTrip() {
        Path file = output.resolve("sample.txt");
        FileUtils.writeText(file, "Test started\n");
        FileUtils.appendText(file, "Test ended ✓\n");
        ApiAssertions.equals(FileUtils.readText(file), "Test started\nTest ended ✓\n", "Text append is saved");
        FileUtils.writeText(file, "Replacement");
        ApiAssertions.equals(FileUtils.readText(file), "Replacement", "Text write replaces old content");
    }

    @Test(groups = "regression", description = "CSV round trip supports commas, quotes, newlines, empty values")
    public void csvRoundTrip() {
        Path file = output.resolve("sample.csv");
        List<List<String>> rows = Arrays.asList(
                Arrays.asList("name", "notes", "optional"),
                Arrays.asList("QA, User", "Uses \"quotes\"\nand newlines", ""));
        CsvUtils.write(file, rows);
        ApiAssertions.equals(CsvUtils.read(file), rows, "CSV data including quoted fields is preserved");
    }
}
