package com.qa.api.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.qa.api.commonUtils.EnvironmentConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** One report and one current test, because the supplied runners run sequentially. */
public final class ReportManager {
    private static ExtentReports report;
    private static ExtentTest currentTest;

    private ReportManager() { }

    public static void initialize() {
        if (report != null) {
            return;
        }
        try {
            Files.createDirectories(Path.of("target", "reports"));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot create report folder", exception);
        }
        ExtentSparkReporter spark = new ExtentSparkReporter("target/reports/ExtentReport.html");
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

    public static void endTest() {
        currentTest = null;
    }

    public static void flush() {
        if (report != null) {
            report.flush();
        }
    }
}
