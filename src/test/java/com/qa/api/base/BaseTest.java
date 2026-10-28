package com.qa.api.base;

import com.qa.api.client.ApiClient;
import com.qa.api.commonUtils.EnvironmentConfig;
import com.qa.api.commonUtils.TestData;
import com.qa.api.commonUtils.TestLogger;
import com.qa.api.listeners.TestListener;
import com.qa.api.reporting.ReportManager;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;

@Listeners(TestListener.class)
public class BaseTest {
    protected EnvironmentConfig config;
    protected TestData testData;
    protected ApiClient apiClient;
    protected int responseTimeLimit;

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        ReportManager.initialize();
        TestLogger.info("SUITE START: Initializing API automation framework");
    }

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        TestLogger.info("Preparing test class: " + getClass().getSimpleName());
        config = new EnvironmentConfig();
        testData = new TestData(config.getEnvironment());
        responseTimeLimit = config.getPositiveInt("response.time.limit.ms");
        apiClient = new ApiClient(config.get("base.url"), config);
        // Add shared authentication or other prerequisites here when real APIs are available.
    }

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        TestLogger.info("SUITE END: Saving report to " + ReportManager.getCurrentReportPath());
        ReportManager.flush();
    }
}
