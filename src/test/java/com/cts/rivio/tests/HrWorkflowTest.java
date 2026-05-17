package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.*;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * HrWorkflowTest – HR role access checks.
 *
 * Per FRD 2.9 + app.routes.ts, HR should access: dashboard, employees,
 * attendance, leave, /ats (recruitment), ask-rivi. HR should NOT access
 * /payroll or /company.
 *
 * All URL assertions wait for client-side routing to settle (waitForUrlToBeStable)
 * to avoid race conditions with Angular's roleGuard redirect.
 */
public class HrWorkflowTest extends BaseTest {

    private String goAndStabilise(String url) {
        driver.get(url);
        WaitUtils.waitForAngularLoad(driver);
        return WaitUtils.waitForUrlToBeStable(driver);
    }

    @Test(description = "HR sees Admin Overview on /dashboard")
    public void hr_dashboardLoads() {
        new LoginPage(driver).login(AppConstants.HR_EMAIL, AppConstants.HR_PASSWORD);
        String url = goAndStabilise(AppConstants.DASHBOARD_URL);
        Assert.assertTrue(url.contains("/dashboard"),
                "HR should reach /dashboard per app.routes.ts. URL: " + url);
        ExtentManager.getTest().pass("HR dashboard access verified");
    }

    @Test(description = "HR opens Employees directory")
    public void hr_employeesDirectoryLoads() {
        new LoginPage(driver).login(AppConstants.HR_EMAIL, AppConstants.HR_PASSWORD);
        String url = goAndStabilise(AppConstants.EMPLOYEE_DIR_URL);
        Assert.assertTrue(url.contains("/employees"),
                "HR should reach /employees. URL: " + url);
        EmployeeDirectoryPage dir = new EmployeeDirectoryPage(driver);
        Assert.assertTrue(dir.isPageLoaded(), "Employees page should render");
        ExtentManager.getTest().pass("HR can open Employees directory");
    }

    @Test(description = "HR opens Leave Approvals")
    public void hr_leaveApprovalsLoads() {
        new LoginPage(driver).login(AppConstants.HR_EMAIL, AppConstants.HR_PASSWORD);
        String url = goAndStabilise(AppConstants.LEAVE_URL);
        Assert.assertTrue(url.contains("/leave"),
                "HR should reach /leave. URL: " + url);
        LeaveDashboardPage lv = new LeaveDashboardPage(driver);
        Assert.assertTrue(lv.isPageLoaded(), "Leave Approvals page should render");
        ExtentManager.getTest().pass("HR can open Leave Approvals");
    }

    @Test(description = "HR opens Recruitment Pipeline (/ats)")
    public void hr_recruitmentLoads() {
        new LoginPage(driver).login(AppConstants.HR_EMAIL, AppConstants.HR_PASSWORD);
        String url = goAndStabilise(AppConstants.RECRUITMENT_URL);
        Assert.assertTrue(url.contains("/ats"),
                "HR should reach /ats. URL: " + url);
        RecruitmentDashboardPage rec = new RecruitmentDashboardPage(driver);
        Assert.assertTrue(rec.isPageLoaded(), "Recruitment page should render");
        ExtentManager.getTest().pass("HR can open Recruitment");
    }

    @Test(description = "HR cannot reach /payroll — redirected")
    public void hr_payrollIsBlocked() {
        new LoginPage(driver).login(AppConstants.HR_EMAIL, AppConstants.HR_PASSWORD);
        String url = goAndStabilise(AppConstants.PAYROLL_URL);
        Assert.assertFalse(url.endsWith("/payroll"),
                "HR should be redirected away from /payroll. Final URL: " + url);
    }

    @Test(description = "HR cannot reach Company Config — redirected")
    public void hr_companyIsBlocked() {
        new LoginPage(driver).login(AppConstants.HR_EMAIL, AppConstants.HR_PASSWORD);
        String url = goAndStabilise(AppConstants.COMPANY_URL);
        Assert.assertFalse(url.endsWith("/company"),
                "HR should be redirected away from /company. Final URL: " + url);
    }
}
