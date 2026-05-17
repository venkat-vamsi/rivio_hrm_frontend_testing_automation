package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * AIPolicyAssistantTest – Ask Rivi feature at /ask-rivi.
 *
 * Allowed for: Super Admin, Hr, Payroll Manager, Manager (NOT Employee — sidebar
 * hides the link, route is allowed-listed in roleGuard).
 */
public class AIPolicyAssistantTest extends BaseTest {

    @Test(priority = 1, description = "Admin can navigate to Ask Rivi (/ask-rivi)")
    public void adminCanOpenAskRivi() {
        new LoginPage(driver).login(AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD);
        driver.get(AppConstants.ASK_RIVI_URL);
        Assert.assertTrue(driver.getCurrentUrl().contains("/ask-rivi"),
                "Admin should reach /ask-rivi. URL: " + driver.getCurrentUrl());
        ExtentManager.getTest().pass("Admin can open Ask Rivi");
    }

    @Test(priority = 2, description = "Employee cannot access /ask-rivi (roleGuard)")
    public void employeeCannotOpenAskRivi() {
        new LoginPage(driver).login(AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD);
        driver.get(AppConstants.ASK_RIVI_URL);
        com.cts.rivio.utils.WaitUtils.waitForAngularLoad(driver);
        String url = com.cts.rivio.utils.WaitUtils.waitForUrlToBeStable(driver);
        Assert.assertFalse(url.endsWith("/ask-rivi"),
                "Employee should be redirected away from /ask-rivi. URL: " + url);
        ExtentManager.getTest().pass("Employee blocked from Ask Rivi at route level");
    }
}
