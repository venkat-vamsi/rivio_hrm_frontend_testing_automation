package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.*;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * PayrollManagerWorkflowTest – Payroll Manager has /employees, /attendance,
 * /payroll, /ask-rivi. No /dashboard, no /leave, no /ats, no /company.
 */
public class PayrollManagerWorkflowTest extends BaseTest {

    private String goAndStabilise(String url) {
        driver.get(url);
        WaitUtils.waitForAngularLoad(driver);
        return WaitUtils.waitForUrlToBeStable(driver);
    }

    @Test(description = "Payroll Manager opens /payroll")
    public void payroll_dashboardLoads() {
        new LoginPage(driver).login(AppConstants.PAYROLL_EMAIL, AppConstants.PAYROLL_PASSWORD);
        String url = goAndStabilise(AppConstants.PAYROLL_URL);
        Assert.assertTrue(url.contains("/payroll"),
                "Payroll Manager should reach /payroll. URL: " + url);
        PayrollDashboardPage p = new PayrollDashboardPage(driver);
        Assert.assertTrue(p.isPageLoaded(), "Payroll page should render");
        ExtentManager.getTest().pass("Payroll Manager can open /payroll");
    }

    @Test(description = "Payroll Manager opens Employees directory")
    public void payroll_employeesDirectoryLoads() {
        new LoginPage(driver).login(AppConstants.PAYROLL_EMAIL, AppConstants.PAYROLL_PASSWORD);
        String url = goAndStabilise(AppConstants.EMPLOYEE_DIR_URL);
        Assert.assertTrue(url.contains("/employees"),
                "Payroll Manager should reach Employees directory. URL: " + url);
        EmployeeDirectoryPage dir = new EmployeeDirectoryPage(driver);
        Assert.assertTrue(dir.isPageLoaded(), "Employees page should render");
        ExtentManager.getTest().pass("Payroll Manager can open Employees");
    }

    @Test(description = "Payroll Manager opens Attendance")
    public void payroll_attendanceLoads() {
        new LoginPage(driver).login(AppConstants.PAYROLL_EMAIL, AppConstants.PAYROLL_PASSWORD);
        String url = goAndStabilise(AppConstants.ATTENDANCE_URL);
        Assert.assertTrue(url.contains("/attendance"),
                "Payroll Manager should reach Attendance. URL: " + url);
        AttendancePage att = new AttendancePage(driver);
        Assert.assertTrue(att.isPageLoaded(), "Attendance page should render");
        ExtentManager.getTest().pass("Payroll Manager can open Attendance");
    }

    @Test(description = "Payroll Manager cannot reach /dashboard — redirected")
    public void payroll_dashboardIsBlocked() {
        new LoginPage(driver).login(AppConstants.PAYROLL_EMAIL, AppConstants.PAYROLL_PASSWORD);
        String url = goAndStabilise(AppConstants.DASHBOARD_URL);
        Assert.assertFalse(url.endsWith("/dashboard"),
                "Payroll Manager should be redirected from /dashboard. Final URL: " + url);
    }

    @Test(description = "Payroll Manager cannot reach /leave — redirected")
    public void payroll_leaveIsBlocked() {
        new LoginPage(driver).login(AppConstants.PAYROLL_EMAIL, AppConstants.PAYROLL_PASSWORD);
        String url = goAndStabilise(AppConstants.LEAVE_URL);
        Assert.assertFalse(url.endsWith("/leave"),
                "Payroll Manager should be redirected from /leave. Final URL: " + url);
    }

    @Test(description = "Payroll Manager cannot reach /ats — redirected")
    public void payroll_atsIsBlocked() {
        new LoginPage(driver).login(AppConstants.PAYROLL_EMAIL, AppConstants.PAYROLL_PASSWORD);
        String url = goAndStabilise(AppConstants.RECRUITMENT_URL);
        Assert.assertFalse(url.endsWith("/ats"),
                "Payroll Manager should be redirected from /ats. Final URL: " + url);
    }
}
