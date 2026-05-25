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

    @Override protected String getRole() { return ROLE_MANAGER; }

    private String goAndStabilise(String url) {
        driver.get(url);
        WaitUtils.waitForAngularLoad(driver);
        return WaitUtils.waitForUrlToBeStable(driver);
    }

    @Test(groups = {"regression"}, description = "Manager does NOT see Admin Overview — should be redirected")
    public void manager_dashboardRedirectsAway() {
        String url = goAndStabilise(AppConstants.DASHBOARD_URL);
        Assert.assertFalse(url.endsWith("/dashboard"),
                "Manager should NOT land on /dashboard. Final URL: " + url);
    }

    @Test(groups = {"regression"}, description = "Manager opens Employees directory")
    public void manager_employeesDirectoryLoads() {
        String url = goAndStabilise(AppConstants.EMPLOYEE_DIR_URL);
        Assert.assertTrue(url.contains("/employees"),
                "Manager should reach Employees directory. URL: " + url);
        EmployeeDirectoryPage dir = new EmployeeDirectoryPage(driver);
        Assert.assertTrue(dir.isPageLoaded(), "Employees page should render");
        ExtentManager.getTest().pass("Manager can open Employees directory");
    }

    @Test(groups = {"regression"}, description = "Manager opens Leave Approvals (team approvals)")
    public void manager_leaveApprovalsLoads() {
        String url = goAndStabilise(AppConstants.LEAVE_URL);
        Assert.assertTrue(url.contains("/leave"),
                "Manager should reach Leave Approvals. URL: " + url);
        LeaveDashboardPage lv = new LeaveDashboardPage(driver);
        Assert.assertTrue(lv.isPageLoaded(), "Leave Approvals page should render");
        ExtentManager.getTest().pass("Manager can open Leave Approvals");
    }

    @Test(groups = {"regression"}, description = "Manager opens Time & Attendance for team")
    public void manager_attendanceLoads() {
        String url = goAndStabilise(AppConstants.ATTENDANCE_URL);
        Assert.assertTrue(url.contains("/attendance"),
                "Manager should reach Attendance. URL: " + url);
        AttendancePage att = new AttendancePage(driver);
        Assert.assertTrue(att.isPageLoaded(), "Attendance page should render");
        ExtentManager.getTest().pass("Manager can open Attendance");
    }

    @Test(groups = {"regression"}, description = "Manager cannot reach /payroll — redirected")
    public void manager_payrollIsBlocked() {
        String url = goAndStabilise(AppConstants.PAYROLL_URL);
        Assert.assertFalse(url.endsWith("/payroll"),
                "Manager should be redirected from /payroll. Final URL: " + url);
    }

    @Test(groups = {"regression"}, description = "Manager cannot reach /ats — redirected")
    public void manager_atsIsBlocked() {
        String url = goAndStabilise(AppConstants.RECRUITMENT_URL);
        Assert.assertFalse(url.endsWith("/ats"),
                "Manager should be redirected from /ats. Final URL: " + url);
    }

    @Test(groups = {"regression"}, description = "Manager opens Ask Rivi (/ask-rivi)")
    public void manager_askRiviLoads() {
        String url = goAndStabilise(AppConstants.ASK_RIVI_URL);
        Assert.assertTrue(url.contains("/ask-rivi"),
                "Manager should reach /ask-rivi. URL: " + url);
        ExtentManager.getTest().pass("Manager can open Ask Rivi");
    }

    @Test(groups = {"regression"}, description = "Manager cannot reach /company — redirected")
    public void manager_companyIsBlocked() {
        String url = goAndStabilise(AppConstants.COMPANY_URL);
        Assert.assertFalse(url.endsWith("/company"),
                "Manager should be redirected from /company. Final URL: " + url);
    }
}
