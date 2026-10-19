package com.qa.api.listeners;

import com.qa.api.commonUtils.TestLogger;
import com.qa.api.reporting.ReportManager;
import org.apache.logging.log4j.ThreadContext;
import org.testng.IConfigurationListener;
import org.testng.IExecutionListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestListener implements ITestListener, IConfigurationListener, IExecutionListener {
    @Override
    public void onTestStart(ITestResult result) {
        String name = testName(result);
        ReportManager.startTest(name).assignCategory(result.getMethod().getGroups());
        ThreadContext.put("testName", name);
        TestLogger.info("TEST START: " + name);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        TestLogger.pass("TEST END: " + testName(result) + " | PASSED | " + duration(result));
        finishTest();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ensureTest(result);
        TestLogger.fail("TEST END: " + testName(result) + " | FAILED | " + duration(result),
                result.getThrowable());
        finishTest();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ensureTest(result);
        String reason = result.getThrowable() == null ? "No reason supplied" : result.getThrowable().toString();
        TestLogger.skip("TEST END: " + testName(result) + " | SKIPPED | " + reason);
        finishTest();
    }

    @Override
    public void onConfigurationFailure(ITestResult result) {
        ReportManager.startTest("Configuration: " + testName(result));
        ThreadContext.put("testName", testName(result));
        TestLogger.fail("Setup/teardown failed: " + testName(result), result.getThrowable());
        finishTest();
    }

    @Override
    public void onExecutionFinish() {
        TestLogger.info("Test execution finished. Flushing Extent report.");
        ReportManager.flush();
    }

    private void ensureTest(ITestResult result) {
        if (ReportManager.getCurrentTest() == null) {
            ReportManager.startTest(testName(result));
            ThreadContext.put("testName", testName(result));
            TestLogger.info("TEST START: " + testName(result));
        }
    }

    private void finishTest() {
        ReportManager.flush();
        ReportManager.endTest();
        ThreadContext.remove("testName");
    }

    private String testName(ITestResult result) {
        return result.getTestClass().getRealClass().getSimpleName() + "." + result.getMethod().getMethodName();
    }

    private String duration(ITestResult result) {
        return (result.getEndMillis() - result.getStartMillis()) + " ms";
    }
}
