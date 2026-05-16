package com.cts.rivio.base;

import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.utils.ConfigReader;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BaseTest {

    protected WebDriver driver;

    @BeforeSuite(alwaysRun = true)
    public void setUpSuite() {
        ExtentManager.getInstance();
    }

    @Parameters({"browser"})
    @BeforeMethod(alwaysRun = true)
    public void setUp(@Optional("chrome") String browser) {
        String configBrowser = ConfigReader.getProperty("browser");
        String selectedBrowser = (configBrowser != null && !configBrowser.isEmpty())
                                  ? configBrowser : browser;

        DriverFactory.initDriver(selectedBrowser);
        driver = DriverFactory.getDriver();

        // Navigate to base URL
        driver.get(AppConstants.BASE_URL);

        // Wait for Angular app to fully load
        WaitUtils.waitForAngularLoad(driver);

        // Dismiss any popup that appears on first load (cookie banners, tour tips, etc.)
        WaitUtils.dismissPopupsIfPresent(driver);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE) {
            captureScreenshot(result.getName());
        }
        DriverFactory.quitDriver();
    }

    @AfterSuite(alwaysRun = true)
    public void tearDownSuite() {
        ExtentManager.flushReport();
    }

    // ── Helper: login shortcut for tests that need an authenticated session ──

    /**
     * Performs login and waits for the dashboard URL.
     * Call from @BeforeMethod in test classes that need a logged-in state.
     */
    protected void loginAs(String email, String password) {
        // Navigate to login page explicitly
        if (!driver.getCurrentUrl().contains("login") && !driver.getCurrentUrl().contains("auth")) {
            driver.get(AppConstants.LOGIN_URL);
            WaitUtils.waitForAngularLoad(driver);
            WaitUtils.dismissPopupsIfPresent(driver);
        }

        // Enter credentials using JavaScript to avoid Angular intercept issues
        try {
            org.openqa.selenium.WebElement emailEl = WaitUtils.waitForVisibility(
                driver, org.openqa.selenium.By.cssSelector(
                    "input[formcontrolname='email'], input[type='email'], input[name='email']"));
            WaitUtils.jsSetValue(driver, emailEl, email);
            emailEl.sendKeys("");  // trigger Angular binding

            org.openqa.selenium.WebElement passEl = WaitUtils.waitForVisibility(
                driver, org.openqa.selenium.By.cssSelector(
                    "input[formcontrolname='password'], input[type='password'], input[name='password']"));
            WaitUtils.jsSetValue(driver, passEl, password);
            passEl.sendKeys("");

            org.openqa.selenium.WebElement btn = WaitUtils.waitForClickability(
                driver, org.openqa.selenium.By.cssSelector("button[type='submit']"));
            WaitUtils.safeClick(driver, btn);
        } catch (Exception e) {
            System.err.println("[BaseTest.loginAs] Error: " + e.getMessage());
        }

        // Wait until URL leaves the login/auth page
        try {
            WaitUtils.waitForUrlNotContains(driver, "auth");
        } catch (Exception ignored) {}
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.dismissPopupsIfPresent(driver);
    }

    // ── Screenshot helper ─────────────────────────────────────────────────────

    public String captureScreenshot(String testName) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName  = testName + "_" + timestamp + ".png";
        String destPath  = AppConstants.SCREENSHOT_PATH + fileName;

        try {
            File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.createDirectories(Paths.get(AppConstants.SCREENSHOT_PATH));
            Files.copy(srcFile.toPath(), Paths.get(destPath));
            System.out.println("[Screenshot] Saved: " + destPath);
        } catch (IOException e) {
            System.err.println("[Screenshot] Failed to save: " + e.getMessage());
        }
        return new File(destPath).getAbsolutePath();
    }
}
