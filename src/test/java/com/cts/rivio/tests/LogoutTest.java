package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.HeaderPage;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * LogoutTest – verifies that every role can log out and that the auth.guard
 * blocks subsequent deep-links.
 *
 * Both assertions wait for the URL to stabilise before reading it — without
 * this, we'd capture the URL mid-redirect and report false failures even
 * though logout/redirect are working.
 */
public class LogoutTest extends BaseTest {

    @DataProvider(name = "allRoles")
    public Object[][] allRoles() {
        return new Object[][]{
            {"Super Admin",     AppConstants.ADMIN_EMAIL,    AppConstants.ADMIN_PASSWORD},
            {"Hr",              AppConstants.HR_EMAIL,       AppConstants.HR_PASSWORD},
            {"Manager",         AppConstants.MANAGER_EMAIL,  AppConstants.MANAGER_PASSWORD},
            {"Payroll Manager", AppConstants.PAYROLL_EMAIL,  AppConstants.PAYROLL_PASSWORD},
            {"Employee",        AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD},
        };
    }

    @Test(dataProvider = "allRoles",
          description = "Each role can sign out — URL stabilises on /login")
    public void logoutLandsOnLogin(String roleLabel, String email, String password) {
        ExtentManager.getTest().info("[Logout-UI] Role: " + roleLabel);

        new LoginPage(driver).login(email, password);
        String afterLogin = WaitUtils.waitForUrlToBeStable(driver);
        Assert.assertFalse(afterLogin.contains("/login"),
                roleLabel + " should be logged in before we try to log out. URL: " + afterLogin);

        new HeaderPage(driver).clickLogout();
        String afterLogout = WaitUtils.waitForUrlToBeStable(driver);
        Assert.assertTrue(afterLogout.contains("/login"),
                roleLabel + " should land on /login after Sign Out. URL: " + afterLogout);
        ExtentManager.getTest().pass(roleLabel + " logout button → /login OK");
    }

    @Test(dataProvider = "allRoles",
          description = "After logout, auth.guard must block deep-link to /dashboard")
    public void authGuardBlocksAfterLogout(String roleLabel, String email, String password) {
        ExtentManager.getTest().info("[AuthGuard-Post-Logout] Role: " + roleLabel);

        new LoginPage(driver).login(email, password);
        WaitUtils.waitForUrlToBeStable(driver);

        new HeaderPage(driver).clickLogout();
        String afterLogout = WaitUtils.waitForUrlToBeStable(driver);
        Assert.assertTrue(afterLogout.contains("/login"),
                roleLabel + " precondition: should be on /login after logout. URL: " + afterLogout);

        // Try to deep-link a protected route. auth.guard must redirect back to /login.
        driver.get(AppConstants.DASHBOARD_URL);
        WaitUtils.waitForAngularLoad(driver);
        String afterDeepLink = WaitUtils.waitForUrlToBeStable(driver);

        Assert.assertTrue(afterDeepLink.contains("/login"),
                "auth.guard did not redirect after logout for " + roleLabel
              + ". Final URL: " + afterDeepLink);
        ExtentManager.getTest().pass(roleLabel + " auth.guard correctly redirected to /login");
    }
}
