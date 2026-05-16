package com.cts.rivio.utils;

import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.cts.rivio.base.BaseTest;
import com.cts.rivio.base.DriverFactory;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.*;

import java.util.Arrays;

/**
 * ExtentReportListener – a TestNG ITestListener that automatically logs
 * every test start, pass, fail, and skip into the ExtentReport.
 *
 * Register this listener in testng.xml:
 *   <listeners>
 *       <listener class-name="com.cts.rivio.utils.ExtentReportListener"/>
 *   </listeners>
 *
 * It also embeds a Base64 screenshot directly in the HTML report on failure.
 */
public class ExtentReportListener implements ITestListener {

    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String description = result.getMethod().getDescription();
        ExtentManager.createTest(testName, description);
        ExtentManager.getTest().log(Status.INFO, "Test Started: " + testName);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentManager.getTest().log(Status.PASS,
                "Test PASSED: " + result.getMethod().getMethodName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        // Log the exception stack trace
        ExtentManager.getTest().log(Status.FAIL,
                "Test FAILED: " + result.getThrowable().getMessage());
        ExtentManager.getTest().log(Status.FAIL,
                "Stack Trace: " + Arrays.toString(result.getThrowable().getStackTrace()));

        // Embed screenshot as Base64 directly in the report HTML
        try {
            String base64 = ((TakesScreenshot) DriverFactory.getDriver())
                    .getScreenshotAs(OutputType.BASE64);
            ExtentManager.getTest().fail("Screenshot on Failure:",
                    MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
        } catch (Exception e) {
            ExtentManager.getTest().log(Status.WARNING,
                    "Screenshot capture failed: " + e.getMessage());
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentManager.getTest().log(Status.SKIP,
                "Test SKIPPED: " + result.getMethod().getMethodName());
        if (result.getThrowable() != null) {
            ExtentManager.getTest().log(Status.SKIP, result.getThrowable().getMessage());
        }
    }

    @Override
    public void onFinish(ITestContext context) {
        ExtentManager.flushReport();
    }
}
