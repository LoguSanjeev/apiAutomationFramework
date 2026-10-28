package com.qa.api.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.qa.api.commonUtils.EnvironmentConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** One report and one current test, because the supplied runners run sequentially. */
public final class ReportManager {
    private static final Path REPORT_DIRECTORY = Path.of("test-output");
    private static final int MAX_REPORTS = 30;
    private static final DateTimeFormatter REPORT_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS");

    private static ExtentReports report;
    private static ExtentTest currentTest;
    private static Path currentReportPath;

    private ReportManager() { }

    public static void initialize() {
        if (report != null) {
            return;
        }
        try {
            Files.createDirectories(REPORT_DIRECTORY);
            deleteOldReports();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot create report folder", exception);
        }
        currentReportPath = REPORT_DIRECTORY.resolve(
                "ExtentReport-" + LocalDateTime.now().format(REPORT_TIMESTAMP) + ".html");
        ExtentSparkReporter spark = new ExtentSparkReporter(currentReportPath.toString());
        spark.config().setDocumentTitle("API Automation Report");
        spark.config().setReportName("API Automation Framework");
        report = new ExtentReports();
        report.attachReporter(spark);
        report.setSystemInfo("Environment", System.getProperty("env", EnvironmentConfig.DEFAULT_ENVIRONMENT));
        report.setSystemInfo("Java", System.getProperty("java.version"));
    }

    public static ExtentTest startTest(String name) {
        initialize();
        currentTest = report.createTest(name);
        return currentTest;
    }

    public static ExtentTest getCurrentTest() {
        return currentTest;
    }

    public static Path getCurrentReportPath() {
        initialize();
        return currentReportPath;
    }

    public static void endTest() {
        currentTest = null;
    }

    public static void flush() {
        if (report != null) {
            report.flush();
        }
    }

    private static void deleteOldReports() throws IOException {
        List<Path> reports;
        try (Stream<Path> paths = Files.list(REPORT_DIRECTORY)) {
            reports = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("ExtentReport-"))
                    .filter(path -> path.getFileName().toString().endsWith(".html"))
                    .sorted(Comparator.comparingLong(ReportManager::lastModifiedTime))
                    .toList();
        }

        int reportsToDelete = Math.max(0, reports.size() - MAX_REPORTS + 1);
        for (int index = 0; index < reportsToDelete; index++) {
            Files.deleteIfExists(reports.get(index));
        }
    }

    private static long lastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read report timestamp: " + path, exception);
        }
    }
}
