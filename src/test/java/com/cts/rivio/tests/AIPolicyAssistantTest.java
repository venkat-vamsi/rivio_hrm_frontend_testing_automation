package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.pages.SidebarPage;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
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

    /**
     * RV-BUG-NEW-05: FRD §2.8/§2.9 explicitly grants the Employee role access
     * to Ask Rivio for policy questions. The current sidebar hides the entry
     * and the roleGuard rejects /ask-rivi for Employee — both contradict the
     * FRD role-based access matrix. This test fails until Employee can see and
     * open Ask Rivio.
     */
    @Test(priority = 3, description =
        "RV_AI_BUG_05 – Employee must be able to ask policy questions (Ask Rivio) per FRD")
    public void RV_AI_BUG_05_employeeCanAskPolicyQuestions() {
        new LoginPage(driver).login(AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD);
        WaitUtils.waitForAngularLoad(driver);

        SidebarPage sb = new SidebarPage(driver);
        boolean sidebarShowsAskRivio = sb.isAskRiviVisible();

        driver.get(AppConstants.ASK_RIVI_URL);
        WaitUtils.waitForAngularLoad(driver);
        String finalUrl = WaitUtils.waitForUrlToBeStable(driver);
        boolean routeAllowed = finalUrl.endsWith("/ask-rivi");

        Assert.assertTrue(sidebarShowsAskRivio && routeAllowed,
                "RV-BUG-NEW-05: FRD §2.8 grants Employee access to Ask Rivio for policy "
              + "questions, but Employee cannot use the feature. "
              + "sidebar shows Ask Rivio? " + sidebarShowsAskRivio + " ; "
              + "/ask-rivi route reachable? " + routeAllowed + " (URL=" + finalUrl + ").");
        ExtentManager.getTest().pass("Employee can ask policy questions");
    }
}
