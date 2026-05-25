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
 * RoleAccessTest – core RBAC validation.
 *
 * Uses waitForUrlToBeStable everywhere because Angular's roleGuard runs after
 * the initial document load; without it we capture the URL mid-redirect and
 * report false positives.
 */
public class RoleAccessTest extends BaseTest {

    private String goAndStabilise(String url) {
        driver.get(url);
        WaitUtils.waitForAngularLoad(driver);
        return WaitUtils.waitForUrlToBeStable(driver);
    }

    @Test(priority = 1, groups = {"smoke", "regression"}, description = "Unauthenticated access to /dashboard redirects to /login")
    public void unauthenticatedRedirectsToLogin() {
        clearAuthStorage();
        String url = goAndStabilise(AppConstants.DASHBOARD_URL);
        Assert.assertTrue(url.contains("/login"),
                "auth.guard should redirect to /login. Final URL: " + url);
        ExtentManager.getTest().pass("Auth guard redirect verified");
    }

    @Test(priority = 2, groups = {"regression"}, description = "SuperAdmin sees all admin nav items")
    public void superAdminSeesAllAdminNav() {
        new LoginPage(driver).login(AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD);
        SidebarPage sb = new SidebarPage(driver);
        String[] expected = {"/dashboard", "/employees", "/attendance", "/leave",
                             "/payroll", "/ats", "/company"};
        for (String r : expected) {
            Assert.assertTrue(sb.isItemVisible(r),
                    "SuperAdmin sidebar should include route: " + r);
        }
        ExtentManager.getTest().pass("SuperAdmin sees all expected admin nav items");
    }

    @Test(priority = 3, groups = {"regression"}, description = "Employee role cannot access /payroll – roleGuard redirects")
    public void RV_RBAC_employeeCannotAccessPayroll() {
        new LoginPage(driver).login(AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD);
        String url = goAndStabilise(AppConstants.PAYROLL_URL);
        Assert.assertTrue(url.contains("/self-service") || url.contains("/login"),
                "Employee hitting /payroll should be redirected to /self-service. URL: " + url);
        ExtentManager.getTest().pass("Employee blocked from /payroll");
    }

    @Test(priority = 4, groups = {"regression"}, description = "Manager cannot access /payroll")
    public void managerCannotAccessPayroll() {
        new LoginPage(driver).login(AppConstants.MANAGER_EMAIL, AppConstants.MANAGER_PASSWORD);
        String url = goAndStabilise(AppConstants.PAYROLL_URL);
        Assert.assertFalse(url.endsWith("/payroll"),
                "Manager attempting to access /payroll should be redirected. URL: " + url);
        ExtentManager.getTest().pass("Manager blocked from /payroll");
    }

    @Test(priority = 5, groups = {"regression"}, description = "Manager cannot access /company")
    public void managerCannotAccessCompany() {
        new LoginPage(driver).login(AppConstants.MANAGER_EMAIL, AppConstants.MANAGER_PASSWORD);
        String url = goAndStabilise(AppConstants.COMPANY_URL);
        Assert.assertFalse(url.endsWith("/company"),
                "Only SuperAdmin can access /company. URL: " + url);
        ExtentManager.getTest().pass("Manager blocked from /company");
    }

    @Test(priority = 6, groups = {"smoke", "regression"}, description = "RV_SSP_001 – Self Service sub-menu shows the 4 employee links")
    public void RV_SSP_001_selfServiceSubMenu() {
        new LoginPage(driver).login(AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD);
        SidebarPage sb = new SidebarPage(driver);
        sb.openSelfServiceGroup();

        Assert.assertTrue(sb.isItemVisible("/self-service/profile"),    "My Profile link");
        Assert.assertTrue(sb.isItemVisible("/self-service/attendance"), "My Attendance link");
        Assert.assertTrue(sb.isItemVisible("/self-service/leaves"),     "My Leaves link");
        Assert.assertTrue(sb.isItemVisible("/self-service/payslips"),   "My Payslips link");
        ExtentManager.getTest().pass("All four Self Service items visible");
    }
}
