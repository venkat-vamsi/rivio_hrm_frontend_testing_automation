package com.cts.rivio.base;

import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.HeaderPage;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.utils.ConfigReader;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.ExtentReportListener;
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
 * Fast role-bucket lifecycle:
 *   @BeforeSuite  → init Extent + launch ONE Chrome instance for the whole suite.
 *   @BeforeClass  → if subclass declared a role via getRole(), login once for that
 *                   role (and skip if the previous class already logged in as it).
 *   @BeforeMethod → role-aware classes: just normalise window state.
 *                   mixed-role classes (getRole() == null): clear auth + return
 *                   to /login so the test's own login logic starts fresh.
 *   @AfterMethod  → screenshot on failure (browser stays alive).
 *   @AfterSuite   → flush HTML report + quit the single browser.
 *
 * Result: 148 tests run on 5 UI logins for the role-bucketed classes (plus the
 * few logins the auth/role-matrix tests perform as part of their own assertions).
 */
@Listeners(ExtentReportListener.class)
public class BaseTest {

    /** Role identifiers returned by getRole(). null = mixed/auth class. */
    public static final String ROLE_ADMIN    = "ADMIN";
    public static final String ROLE_HR       = "HR";
    public static final String ROLE_MANAGER  = "MANAGER";
    public static final String ROLE_PAYROLL  = "PAYROLL";
    public static final String ROLE_EMPLOYEE = "EMPLOYEE";

    protected WebDriver driver;

    /** Tracks which role the shared browser is currently logged in as. */
    private static String currentLoggedInRole = null;

    /**
     * Subclasses override this to declare which role they belong to. Returning
     * null (the default) means the class manages its own login/logout — useful
     * for LoginTest, LogoutTest, RoleMatrixTest, RoleAccessTest etc.
     */
    protected String getRole() {
        return null;
    }

    @Parameters({"browser"})
    @BeforeSuite(alwaysRun = true)
    public void setUpSuite(@Optional("chrome") String browser) {
        ExtentManager.getInstance();

        String configBrowser = ConfigReader.getProperty("browser");
        String selectedBrowser = (configBrowser != null && !configBrowser.isEmpty())
                                  ? configBrowser : browser;

        DriverFactory.initDriver(selectedBrowser);
    }

    @BeforeClass(alwaysRun = true)
    public void classSetUp() {
        driver = DriverFactory.getDriver();
        String role = getRole();

        if (role == null) {
            // Mixed-role / auth class. Force logged-out state so its own
            // login calls start from a clean slate. Then clear the cached
            // role marker — once this class touches the session, we can no
            // longer trust the previous bucket's login state.
            forceLoggedOut();
            currentLoggedInRole = null;
            return;
        }

        // Role-bucket class. Login once for the role if we aren't already.
        if (role.equals(currentLoggedInRole)) {
            return;
        }
        loginAsRole(role);
        currentLoggedInRole = role;
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        driver = DriverFactory.getDriver();

        // Reset window size — SecurityAuditTest's mobile viewport test shrinks
        // it to 375×812 and the shared browser would otherwise stay tiny.
        try {
            driver.manage().window().maximize();
        } catch (Exception ignored) {}

        if (getRole() == null) {
            // Mixed-role/auth class — every test starts logged out at /login.
            forceLoggedOut();
        }
        // For role-bucket classes the session from @BeforeClass is still
        // valid; the test's own @BeforeMethod/setup handles page navigation.
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE) {
            captureScreenshot(result.getName());
        }
    }

    @AfterSuite(alwaysRun = true)
    public void tearDownSuite() {
        try {
            ExtentManager.flushReport();
        } finally {
            currentLoggedInRole = null;
            DriverFactory.quitDriver();
        }
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

    /**
     * Navigate to /login, clear auth storage, reload — equivalent to what the
     * old per-method browser-restart used to achieve, but without quitting Chrome.
     */
    private void forceLoggedOut() {
        driver.get(AppConstants.LOGIN_URL);
        clearAuthStorage();
        driver.get(AppConstants.LOGIN_URL);
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.dismissPopupsIfPresent(driver);
    }

    /** Performs a fresh UI login for the given role. Includes a logout step if
     *  the session already holds another role's token. */
    private void loginAsRole(String role) {
        if (currentLoggedInRole != null) {
            // Try a clean UI logout first so the auth state is destroyed
            // server-side too. Fall back to storage wipe on any failure.
            try {
                new HeaderPage(driver).clickLogout();
                WaitUtils.waitForUrlContains(driver, "/login");
            } catch (Exception ignored) {}
        }
        forceLoggedOut();

        String[] creds = credentialsFor(role);
        new LoginPage(driver).login(creds[0], creds[1]);
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.waitForUrlToBeStable(driver);
    }

    private String[] credentialsFor(String role) {
        switch (role) {
            case ROLE_ADMIN:
                return new String[]{AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD};
            case ROLE_HR:
                return new String[]{AppConstants.HR_EMAIL, AppConstants.HR_PASSWORD};
            case ROLE_MANAGER:
                return new String[]{AppConstants.MANAGER_EMAIL, AppConstants.MANAGER_PASSWORD};
            case ROLE_PAYROLL:
                return new String[]{AppConstants.PAYROLL_EMAIL, AppConstants.PAYROLL_PASSWORD};
            case ROLE_EMPLOYEE:
                return new String[]{AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD};
            default:
                throw new IllegalStateException("Unknown role: " + role);
        }
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
