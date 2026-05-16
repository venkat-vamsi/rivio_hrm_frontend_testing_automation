package com.cts.rivio.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.cts.rivio.constants.AppConstants;

/**
 * ExtentManager – singleton that owns the ExtentReports object.
 *
 * One ExtentReports instance is shared across all test threads.
 * Each test gets its own ExtentTest stored in a ThreadLocal so
 * parallel tests log to the correct node without race conditions.
 */
public class ExtentManager {

    private static ExtentReports extent;

    // ThreadLocal: each test thread has its own ExtentTest reference
    private static final ThreadLocal<ExtentTest> testThreadLocal = new ThreadLocal<>();

    private ExtentManager() {}

    public static synchronized ExtentReports getInstance() {
        if (extent == null) {
            // SparkReporter generates the interactive HTML report
            ExtentSparkReporter spark = new ExtentSparkReporter(AppConstants.REPORT_PATH);
            spark.config().setReportName("Rivio HRMS – Automation Test Report");
            spark.config().setDocumentTitle("Rivio Test Report");
            spark.config().setTheme(Theme.DARK);
            spark.config().setEncoding("UTF-8");

            extent = new ExtentReports();
            extent.attachReporter(spark);

            // System info shown in the report overview
            extent.setSystemInfo("Application", "Rivio HRMS");
            extent.setSystemInfo("Environment", "Production");
            extent.setSystemInfo("URL", AppConstants.BASE_URL);
            extent.setSystemInfo("Browser", "Chrome");
            extent.setSystemInfo("OS", System.getProperty("os.name"));
            extent.setSystemInfo("Java", System.getProperty("java.version"));
        }
        return extent;
    }

    /** Creates a new test node in the report for the given test name. */
    public static ExtentTest createTest(String testName) {
        ExtentTest test = getInstance().createTest(testName);
        testThreadLocal.set(test);
        return test;
    }

    /** Creates a test node with a description. */
    public static ExtentTest createTest(String testName, String description) {
        ExtentTest test = getInstance().createTest(testName, description);
        testThreadLocal.set(test);
        return test;
    }

    /** Returns the ExtentTest for the current thread. */
    public static ExtentTest getTest() {
        return testThreadLocal.get();
    }

    /** Writes all test results to the HTML file. Must be called once after all tests finish. */
    public static void flushReport() {
        if (extent != null) {
            extent.flush();
        }
    }
}
