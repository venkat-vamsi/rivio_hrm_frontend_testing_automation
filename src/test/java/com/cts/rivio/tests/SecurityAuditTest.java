package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.utils.ExtentManager;
import org.openqa.selenium.Dimension;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * SecurityAuditTest – security and responsive checks.
 *
 *   RV_SSP_003 – Cross-employee data isolation (RV-BUG-009: server-side
 *                authorization missing — manually altering /employees/:id should
 *                redirect, but currently leaks data).
 *   Mobile     – Login page should render correctly at 375px viewport.
 */
public class SecurityAuditTest extends BaseTest {

    @Test(priority = 1, groups = {"regression"}, description = "RV_SSP_003 – Employee cannot view another employee's profile via URL")
    public void RV_SSP_003_employeeCannotViewOtherProfile() {
        new LoginPage(driver).login(AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD);

        // Try to navigate to another employee's profile by ID guess.
        driver.get(AppConstants.BASE_URL + "employees/1");
        com.cts.rivio.utils.WaitUtils.waitForAngularLoad(driver);
        String url = com.cts.rivio.utils.WaitUtils.waitForUrlToBeStable(driver);
        ExtentManager.getTest().info("URL after attempted access: " + url);

        // Per the design + roleGuard: Employee role isn't in the /employees allowlist,
        // so they should land on /self-service/profile.
        Assert.assertTrue(url.contains("/self-service") || url.contains("/login"),
                "Employee accessing /employees/* should be redirected (current URL: " + url + ")");
        ExtentManager.getTest().pass("Employee blocked from arbitrary employee profile URLs");
    }

    @Test(priority = 2, groups = {"regression"}, description = "Login page renders correctly at 375×812 mobile viewport")
    public void mobileViewportLoginRenders() {
        driver.manage().window().setSize(new Dimension(375, 812));
        driver.navigate().refresh();
        LoginPage lp = new LoginPage(driver);
        Assert.assertTrue(lp.isLoginPageDisplayed(),
                "Login form should render at 375px width");
        captureScreenshot("mobile_login_375x812");
        ExtentManager.getTest().pass("Login page mobile-responsive");
    }
}
