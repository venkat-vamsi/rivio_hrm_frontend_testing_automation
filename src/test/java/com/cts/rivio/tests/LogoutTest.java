package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.HeaderPage;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * LogoutTest — verifies sign-out works and the auth guard blocks deep-links.
 *
 * Naming pattern: {@code auth_<scenario>}.
 *
 * Tested against the Admin role only — logout / auth-guard logic is identical
 * across roles, so verifying once is sufficient.
 *
 *   auth_logoutRedirect      – sign-out button lands the user on /login
 *   auth_logoutClearsSession – deep-linking a protected route after logout
 *                              is intercepted by the auth guard
 */
public class LogoutTest extends BaseTest {

    @Test(priority = 1, groups = {"regression", "positive"},
          description = "auth_logoutRedirect – Sign Out button redirects to /login")
    public void auth_logoutRedirect() {
        new LoginPage(driver).login(AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD);
        WaitUtils.waitForUrlToBeStable(driver);

        new HeaderPage(driver).clickLogout();
        String afterLogout = WaitUtils.waitForUrlToBeStable(driver);

        Assert.assertTrue(afterLogout.contains("/login"),
                "Logout should redirect to /login. URL: " + afterLogout);
        ExtentManager.getTest().pass("Logout → /login OK");
    }

    @Test(priority = 2, groups = {"regression", "negative"},
          description = "auth_logoutClearsSession – auth guard blocks deep-link after logout")
    public void auth_logoutClearsSession() {
        new LoginPage(driver).login(AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD);
        WaitUtils.waitForUrlToBeStable(driver);

        new HeaderPage(driver).clickLogout();
        WaitUtils.waitForUrlToBeStable(driver);

        // Deep-link a protected route — auth guard must redirect back to /login
        driver.get(AppConstants.DASHBOARD_URL);
        WaitUtils.waitForAngularLoad(driver);
        String finalUrl = WaitUtils.waitForUrlToBeStable(driver);

        Assert.assertTrue(finalUrl.contains("/login"),
                "Auth guard should redirect to /login after logout. URL: " + finalUrl);
        ExtentManager.getTest().pass("Auth guard correctly redirected to /login");
    }
}
