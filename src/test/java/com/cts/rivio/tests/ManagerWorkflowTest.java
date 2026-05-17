package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.*;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * ManagerWorkflowTest – Manager has /employees, /attendance, /leave, /ask-rivi.
 * No /dashboard, no /payroll, no /ats, no /company.
 *
 * URL assertions use waitForUrlToBeStable so Angular's roleGuard redirect
 * has time to settle before the URL is captured.
 */
public class ManagerWorkflowTest extends BaseTest {

    private String goAndStabilise(String url) {
        driver.get(url);
        WaitUtils.waitForAngularLoad(driver);
        return WaitUtils.waitForUrlToBeStable(driver);
    }

    @Test(description = "Manager does NOT see Admin Overview — should be redirected")
    public void manager_dashboardRedirectsAway() {
        new LoginPage(driver).login(AppConstants.MANAGER_EMAIL, AppConstants.MANAGER_PASSWORD);
        String url = goAndStabilise(AppConstants.DASHBOARD_URL);
        Assert.assertFalse(url.endsWith("/dashboard"),
                "Manager should NOT land on /dashboard. Final URL: " + url);
    }

    @Test(description = "Manager opens Employees directory")
    public void manager_employeesDirectoryLoads() {
        new LoginPage(driver).login(AppConstants.MANAGER_EMAIL, AppConstants.MANAGER_PASSWORD);
        String url = goAndStabilise(AppConstants.EMPLOYEE_DIR_URL);
        Assert.assertTrue(url.contains("/employees"),
                "Manager should reach Employees directory. URL: " + url);
        EmployeeDirectoryPage dir = new EmployeeDirectoryPage(driver);
        Assert.assertTrue(dir.isPageLoaded(), "Employees page should render");
        ExtentManager.getTest().pass("Manager can open Employees directory");
    }

    @Test(description = "Manager opens Leave Approvals (team approvals)")
    public void manager_leaveApprovalsLoads() {
        new LoginPage(driver).login(AppConstants.MANAGER_EMAIL, AppConstants.MANAGER_PASSWORD);
        String url = goAndStabilise(AppConstants.LEAVE_URL);
        Assert.assertTrue(url.contains("/leave"),
                "Manager should reach Leave Approvals. URL: " + url);
        LeaveDashboardPage lv = new LeaveDashboardPage(driver);
        Assert.assertTrue(lv.isPageLoaded(), "Leave Approvals page should render");
        ExtentManager.getTest().pass("Manager can open Leave Approvals");
    }

    @Test(description = "Manager opens Time & Attendance for team")
    public void manager_attendanceLoads() {
        new LoginPage(driver).login(AppConstants.MANAGER_EMAIL, AppConstants.MANAGER_PASSWORD);
        String url = goAndStabilise(AppConstants.ATTENDANCE_URL);
        Assert.assertTrue(url.contains("/attendance"),
                "Manager should reach Attendance. URL: " + url);
        AttendancePage att = new AttendancePage(driver);
        Assert.assertTrue(att.isPageLoaded(), "Attendance page should render");
        ExtentManager.getTest().pass("Manager can open Attendance");
    }

    @Test(description = "Manager cannot reach /payroll — redirected")
    public void manager_payrollIsBlocked() {
        new LoginPage(driver).login(AppConstants.MANAGER_EMAIL, AppConstants.MANAGER_PASSWORD);
        String url = goAndStabilise(AppConstants.PAYROLL_URL);
        Assert.assertFalse(url.endsWith("/payroll"),
                "Manager should be redirected from /payroll. Final URL: " + url);
    }

    @Test(description = "Manager cannot reach /ats — redirected")
    public void manager_atsIsBlocked() {
        new LoginPage(driver).login(AppConstants.MANAGER_EMAIL, AppConstants.MANAGER_PASSWORD);
        String url = goAndStabilise(AppConstants.RECRUITMENT_URL);
        Assert.assertFalse(url.endsWith("/ats"),
                "Manager should be redirected from /ats. Final URL: " + url);
    }
}
