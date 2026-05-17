package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.DashboardPage;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * DashboardTest – Test Scenarios DASH-S-01..DASH-S-05.
 *
 * Mapped test cases:
 *   RV_DASH_001 – Four KPI cards render with live values
 *   RV_DASH_002 – Each KPI card navigates to its module (Workforce → Employees,
 *                 Present Today → Attendance, On Leave → Leave Approvals,
 *                 Active Pay Cycles → Payroll)
 *   RV_DASH_003 – Headcount by Department donut chart
 *   RV_DASH_004 – 7-day Attendance Trend line chart
 *   RV_DASH_005 – Pending Leave Requests table
 *   RV_DASH_006 – SYSTEM ONLINE badge + refresh button always visible
 */
public class DashboardTest extends BaseTest {

    private DashboardPage dashboard;

    @BeforeMethod
    public void loginAsAdmin() {
        dashboard = new LoginPage(driver)
                .login(AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD);
        // Wait for /dashboard route + Admin Overview h1 to render before any test runs.
        com.cts.rivio.utils.WaitUtils.waitForUrlContains(driver, "/dashboard");
        com.cts.rivio.utils.WaitUtils.waitForH1Text(driver, "Admin Overview", 15);
        com.cts.rivio.utils.WaitUtils.waitForAngularLoad(driver);
    }

    @Test(priority = 1, description = "RV_DASH_001 – Four KPI cards visible on Admin Overview")
    public void RV_DASH_001_fourKpiCardsRender() {
        Assert.assertTrue(dashboard.isAdminOverviewLoaded(),
                "Admin Overview should be loaded for admin role");

        // Four KPI cards from admin-dashboard.html
        String[] kpis = {"Total Workforce", "Present Today", "On Leave Today", "Active Pay Cycles"};
        for (String kpi : kpis) {
            Assert.assertTrue(dashboard.isKpiCardVisible(kpi),
                    "KPI card '" + kpi + "' should be visible");
        }
        ExtentManager.getTest().pass("All four KPI cards render");
    }

    @Test(priority = 2, description = "RV_DASH_002 – KPI cards navigate to corresponding modules")
    public void RV_DASH_002_kpiCardsNavigate() {
        // 1. Present Today → /attendance
        dashboard.clickKpiCard("Present Today");
        String url = com.cts.rivio.utils.WaitUtils.waitForUrlToBeStable(driver);
        Assert.assertTrue(url.contains("/attendance"),
                "Present Today KPI should navigate to /attendance. URL: " + url);

        // 2. Return to dashboard and verify On Leave Today → /leave
        driver.get(AppConstants.DASHBOARD_URL);
        com.cts.rivio.utils.WaitUtils.waitForUrlToBeStable(driver);
        dashboard = new DashboardPage(driver);
        dashboard.clickKpiCard("On Leave Today");
        url = com.cts.rivio.utils.WaitUtils.waitForUrlToBeStable(driver);
        Assert.assertTrue(url.contains("/leave"),
                "On Leave Today KPI should navigate to /leave. URL: " + url);

        // 3. Verify Active Pay Cycles → /payroll
        driver.get(AppConstants.DASHBOARD_URL);
        com.cts.rivio.utils.WaitUtils.waitForUrlToBeStable(driver);
        dashboard = new DashboardPage(driver);
        dashboard.clickKpiCard("Active Pay Cycles");
        url = com.cts.rivio.utils.WaitUtils.waitForUrlToBeStable(driver);
        Assert.assertTrue(url.contains("/payroll"),
                "Active Pay Cycles KPI should navigate to /payroll. URL: " + url);

        ExtentManager.getTest().pass("KPI cards navigate to correct modules");
    }

    @Test(priority = 3, description = "RV_DASH_003 – Headcount by Department donut chart renders")
    public void RV_DASH_003_headcountDonut() {
        Assert.assertTrue(dashboard.isHeadcountDonutVisible(),
                "Headcount by Department chart (or its empty state) should be visible");
        ExtentManager.getTest().pass("Headcount chart area renders");
    }

    @Test(priority = 4, description = "RV_DASH_004 – 7-day Attendance Trend line chart renders")
    public void RV_DASH_004_attendanceTrend() {
        Assert.assertTrue(dashboard.isAttendanceTrendVisible(),
                "Attendance Trend chart should be visible");
        ExtentManager.getTest().pass("7-day attendance trend renders");
    }

    @Test(priority = 5, description = "RV_DASH_005 – Pending Leave Requests table renders")
    public void RV_DASH_005_pendingLeaveTable() {
        Assert.assertTrue(dashboard.isPendingLeaveSectionVisible(),
                "Pending Leave Requests section should be visible");
        ExtentManager.getTest().info("Pending leave rows: " + dashboard.getPendingLeaveRowCount());
        ExtentManager.getTest().pass("Pending Leave Requests section renders");
    }

    @Test(priority = 6, description = "RV_DASH_006 – SYSTEM ONLINE badge and refresh button visible")
    public void RV_DASH_006_systemOnlineAndRefresh() {
        Assert.assertTrue(dashboard.isSystemOnlineBadgeVisible(),
                "SYSTEM ONLINE badge should be visible on Admin Overview");
        Assert.assertTrue(dashboard.isRefreshButtonVisible(),
                "Refresh dashboard button should be visible");
        dashboard.clickRefresh();
        Assert.assertTrue(dashboard.isSystemOnlineBadgeVisible(),
                "SYSTEM ONLINE badge remains after refresh");
        ExtentManager.getTest().pass("System Online + Refresh visible");
    }
}
