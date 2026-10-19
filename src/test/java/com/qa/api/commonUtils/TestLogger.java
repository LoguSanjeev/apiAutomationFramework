package com.qa.api.commonUtils;

import com.aventstack.extentreports.ExtentTest;
import com.qa.api.reporting.ReportManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Use this helper to put each action into both Log4j and the HTML report. */
public final class TestLogger {
    private static final Logger LOGGER = LogManager.getLogger(TestLogger.class);

    private TestLogger() { }

    public static void info(String message) {
        LOGGER.info(message);
        ExtentTest test = ReportManager.getCurrentTest();
        if (test != null) {
            test.info(escapeHtml(message));
        }
    }

    public static void pass(String message) {
        LOGGER.info("PASS: {}", message);
        ExtentTest test = ReportManager.getCurrentTest();
        if (test != null) {
            test.pass(escapeHtml(message));
        }
    }

    public static void fail(String message, Throwable error) {
        LOGGER.error(message, error);
        ExtentTest test = ReportManager.getCurrentTest();
        if (test != null) {
            test.fail(escapeHtml(message));
            if (error != null) {
                test.fail(error);
            }
        }
    }

    public static void skip(String message) {
        LOGGER.warn(message);
        ExtentTest test = ReportManager.getCurrentTest();
        if (test != null) {
            test.skip(escapeHtml(message));
        }
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
