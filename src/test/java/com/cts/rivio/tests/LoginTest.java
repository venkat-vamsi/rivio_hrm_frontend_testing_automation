package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.DashboardPage;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.utils.ExcelUtils;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.RetryAnalyzer;
import org.testng.Assert;
import org.testng.annotations.*;

/**
 * LoginTest – tests for the login page.
 *
 * Concepts demonstrated:
 *   – @DataProvider reading credentials from Excel (Apache POI)
 *   – Positive login tests for all 5 roles
 *   – Negative tests (wrong password, empty fields, invalid email format)
 *   – ExtentReport logging inside test methods
 *   – RetryAnalyzer on flaky tests
 */
public class LoginTest extends BaseTest {

    private LoginPage loginPage;

    @BeforeMethod
    public void initPage() {
        loginPage = new LoginPage(driver);
    }

    // ── Positive Tests ────────────────────────────────────────────────────────

    @Test(priority = 1, description = "SuperAdmin login should navigate to dashboard")
    public void testSuperAdminLogin() {
        ExtentManager.getTest().info("Logging in as SuperAdmin: " + AppConstants.ADMIN_EMAIL);

        DashboardPage dashboard = loginPage.login(
                AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD);

        Assert.assertTrue(dashboard.isDashboardLoaded(),
                "Dashboard did not load after SuperAdmin login");
        Assert.assertTrue(driver.getCurrentUrl().contains("dashboard"),
                "URL should contain 'dashboard' after login");

        ExtentManager.getTest().pass("SuperAdmin login successful");
    }

    @Test(priority = 2, description = "HR login should navigate to dashboard")
    public void testHrLogin() {
        ExtentManager.getTest().info("Logging in as HR: " + AppConstants.HR_EMAIL);

        DashboardPage dashboard = loginPage.login(AppConstants.HR_EMAIL, AppConstants.HR_PASSWORD);

        Assert.assertTrue(dashboard.isDashboardLoaded(), "Dashboard did not load after HR login");
        ExtentManager.getTest().pass("HR login successful");
    }

    @Test(priority = 3, description = "Manager login should navigate to dashboard")
    public void testManagerLogin() {
        DashboardPage dashboard = loginPage.login(
                AppConstants.MANAGER_EMAIL, AppConstants.MANAGER_PASSWORD);
        Assert.assertTrue(dashboard.isDashboardLoaded());
    }

    @Test(priority = 4, description = "Payroll Manager login should succeed")
    public void testPayrollManagerLogin() {
        DashboardPage dashboard = loginPage.login(
                AppConstants.PAYROLL_EMAIL, AppConstants.PAYROLL_PASSWORD);
        Assert.assertTrue(dashboard.isDashboardLoaded());
    }

    @Test(priority = 5, description = "Employee login should succeed")
    public void testEmployeeLogin() {
        DashboardPage dashboard = loginPage.login(
                AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD);
        Assert.assertTrue(dashboard.isDashboardLoaded());
    }

    // ── Data-Driven Login Tests (reads from Excel) ────────────────────────────

    /**
     * @DataProvider reads test data from LoginData.xlsx → "ValidLogin" sheet.
     * Sheet columns: [email, password, expectedRole]
     */
    @DataProvider(name = "validLoginData")
    public Object[][] getValidLoginData() {
        return ExcelUtils.readDataExcludingHeader(
                AppConstants.LOGIN_DATA_PATH, AppConstants.SHEET_VALID_LOGIN);
    }

    @Test(dataProvider = "validLoginData", priority = 6,
          description = "Data-driven valid login from Excel",
          retryAnalyzer = RetryAnalyzer.class)
    public void testValidLoginFromExcel(String email, String password, String role) {
        ExtentManager.getTest().info("Data-driven login: " + email + " | Role: " + role);

        DashboardPage dashboard = loginPage.login(email, password);

        Assert.assertTrue(dashboard.isDashboardLoaded(),
                "Login failed for: " + email);
        ExtentManager.getTest().pass("Login successful for role: " + role);
    }

    /**
     * @DataProvider reads invalid credentials from Excel → "InvalidLogin" sheet.
     * Sheet columns: [email, password, expectedErrorMessage]
     */
    @DataProvider(name = "invalidLoginData")
    public Object[][] getInvalidLoginData() {
        return ExcelUtils.readDataExcludingHeader(
                AppConstants.LOGIN_DATA_PATH, AppConstants.SHEET_INVALID_LOGIN);
    }

    @Test(dataProvider = "invalidLoginData", priority = 7,
          description = "Data-driven invalid login from Excel")
    public void testInvalidLoginFromExcel(String email, String password, String expectedError) {
        ExtentManager.getTest().info("Invalid login attempt: " + email);

        loginPage.loginExpectingFailure(email, password);

        Assert.assertTrue(loginPage.isErrorDisplayed(),
                "Error message should be displayed for invalid credentials");

        if (!expectedError.isEmpty()) {
            String actual = loginPage.getErrorMessage();
            Assert.assertTrue(actual.toLowerCase().contains(expectedError.toLowerCase()),
                    "Expected error containing '" + expectedError + "' but got: " + actual);
        }

        ExtentManager.getTest().pass("Error correctly shown for invalid credentials");
    }

    // ── Negative Tests ────────────────────────────────────────────────────────

    @Test(priority = 8, description = "Login with empty email should show error")
    public void testLoginWithEmptyEmail() {
        loginPage.loginExpectingFailure("", AppConstants.ADMIN_PASSWORD);
        Assert.assertTrue(loginPage.isErrorDisplayed() || loginPage.isLoginPageDisplayed(),
                "Should stay on login page or show error for empty email");
    }

    @Test(priority = 9, description = "Login with empty password should show error")
    public void testLoginWithEmptyPassword() {
        loginPage.loginExpectingFailure(AppConstants.ADMIN_EMAIL, "");
        Assert.assertTrue(loginPage.isLoginPageDisplayed(),
                "Should stay on login page for empty password");
    }

    @Test(priority = 10, description = "Login with wrong password should show error")
    public void testLoginWithWrongPassword() {
        loginPage.loginExpectingFailure(AppConstants.ADMIN_EMAIL, "wrongpassword123");
        Assert.assertTrue(loginPage.isErrorDisplayed(),
                "Error message should be shown for wrong password");
    }

    @Test(priority = 11, description = "Login with invalid email format should show validation error")
    public void testLoginWithInvalidEmailFormat() {
        loginPage.loginExpectingFailure("notAnEmail", AppConstants.ADMIN_PASSWORD);
        Assert.assertTrue(loginPage.isErrorDisplayed() || loginPage.isLoginPageDisplayed(),
                "Validation error expected for malformed email");
    }

    // ── UI Verification Tests ─────────────────────────────────────────────────

    @Test(priority = 12, description = "Login page title should be correct")
    public void testLoginPageTitle() {
        Assert.assertTrue(loginPage.isLoginPageDisplayed(),
                "Login page elements should be visible");
    }

    @Test(priority = 13, description = "Login with Enter key should work")
    public void testLoginWithEnterKey() {
        DashboardPage dashboard = loginPage.loginUsingEnterKey(
                AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD);
        Assert.assertTrue(dashboard.isDashboardLoaded(),
                "Login via Enter key should navigate to dashboard");
    }
}
