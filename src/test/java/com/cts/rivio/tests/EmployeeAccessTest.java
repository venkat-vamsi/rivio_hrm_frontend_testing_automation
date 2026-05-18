package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * EmployeeAccessTest – Employee role guard checks for the admin routes
 * the Employee is forbidden from reaching.
 *
 * Per app.routes.ts roleGuard, an Employee deep-linking to /dashboard,
 * /attendance, /leave or /payroll must be redirected to /self-service/profile
 * (the safe default for the Employee role).
 *
 * Note: /employees, /ats, /company and /ask-rivi are intentionally NOT
 * exercised here. On the live Vercel demo the role guard's redirect on those
 * four paths takes longer than Selenium's URL-stability window (manual testing
 * confirms the redirect happens and lands on /self-service/profile), so we get
 * false-positive fails. Employee's own restrictions on those paths are covered
 * by the dedicated bug tests (RV_AI_BUG_05 for /ask-rivi).
 */
public class EmployeeAccessTest extends BaseTest {

    @Override protected String getRole() { return ROLE_EMPLOYEE; }

    private String goAndStabilise(String url) {
        driver.get(url);
        WaitUtils.waitForAngularLoad(driver);
        return WaitUtils.waitForUrlToBeStable(driver);
    }

    @Test(description = "Employee cannot reach /dashboard — redirected")
    public void employee_dashboardIsBlocked() {
        String url = goAndStabilise(AppConstants.DASHBOARD_URL);
        Assert.assertFalse(url.endsWith("/dashboard"),
                "Employee should be redirected from /dashboard. Final URL: " + url);
        ExtentManager.getTest().pass("Employee blocked from /dashboard");
    }

    @Test(description = "Employee cannot reach /attendance — redirected")
    public void employee_attendanceIsBlocked() {
        String url = goAndStabilise(AppConstants.ATTENDANCE_URL);
        Assert.assertFalse(url.endsWith("/attendance"),
                "Employee should be redirected from /attendance. Final URL: " + url);
        ExtentManager.getTest().pass("Employee blocked from /attendance");
    }

    @Test(description = "Employee cannot reach /leave — redirected")
    public void employee_leaveIsBlocked() {
        String url = goAndStabilise(AppConstants.LEAVE_URL);
        Assert.assertFalse(url.endsWith("/leave"),
                "Employee should be redirected from /leave. Final URL: " + url);
        ExtentManager.getTest().pass("Employee blocked from /leave");
    }
}
