package com.cts.rivio.base;

import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.utils.ConfigReader;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.JavascriptExecutor;
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

/**
 * BaseTest – parent class for every test.
 *
 * @BeforeSuite  → init ExtentReports.
 * @BeforeMethod → spin up a fresh browser, navigate to /login, clear stale session.
 * @AfterMethod  → screenshot on failure, quit browser.
 * @AfterSuite   → flush HTML report.
 */
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

        // 1. Land on /login directly (root redirects, but /login is canonical
        //    and the guestGuard will bounce already-logged-in sessions away).
        driver.get(AppConstants.LOGIN_URL);

        // 2. Ensure no stale auth from a previous run lingers in localStorage.
        clearAuthStorage();

        // 3. Reload to re-apply the cleared state through the auth.guard.
        driver.get(AppConstants.LOGIN_URL);

        WaitUtils.waitForAngularLoad(driver);
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

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Clears the Angular AuthState's localStorage keys so a previous role's
     * token doesn't leak across tests.
     */
    protected void clearAuthStorage() {
        try {
            ((JavascriptExecutor) driver).executeScript(
                "try { window.localStorage.clear(); window.sessionStorage.clear(); } catch(e) {}"
            );
        } catch (Exception ignored) {}
    }

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
