package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * LoginTest — authentication flows.
 *
 * Naming pattern: {@code auth_<scenario>}.
 *
 *   auth_validAdminLogin – admin credentials land on /dashboard
 *   auth_invalidLogin    – wrong password keeps the user on /login with an inline error
 */
public class LoginTest extends BaseTest {

    private LoginPage loginPage;

    @BeforeMethod(alwaysRun = true)
    public void initPage() {
        loginPage = new LoginPage(driver);
    }

    @Test(priority = 1, groups = {"smoke", "regression", "positive"},
          description = "auth_validAdminLogin – Admin credentials redirect to /dashboard")
    public void auth_validAdminLogin() {
        ExtentManager.getTest().info("Logging in: " + AppConstants.ADMIN_EMAIL);

        loginPage.login(AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD);

        Assert.assertTrue(driver.getCurrentUrl().contains("/dashboard"),
                "Admin should land on /dashboard. Actual URL: " + driver.getCurrentUrl());
        ExtentManager.getTest().pass("Admin login routes to Admin Overview");
    }

    @Test(priority = 2, groups = {"smoke", "regression", "negative"},
          description = "auth_invalidLogin – Wrong password shows inline error and stays on /login")
    public void auth_invalidLogin() {
        ExtentManager.getTest().info("Attempting login with wrong password");

        loginPage.loginExpectingFailure(AppConstants.ADMIN_EMAIL, "wrongPassword123");

        Assert.assertTrue(driver.getCurrentUrl().contains("/login"),
                "User must remain on /login after invalid credentials. URL: " + driver.getCurrentUrl());
        Assert.assertTrue(loginPage.isErrorDisplayed(),
                "Inline error message should be displayed for invalid credentials");
        ExtentManager.getTest().pass("Inline error rendered without page reload");
    }
}
