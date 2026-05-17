package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.DashboardPage;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * ReportsAnalyticsTest – Rivio_Angular doesn't have a dedicated Reports module;
 * analytics live on the Admin Overview (donut chart + 7-day trend). These tests
 * use the dashboard charts as the canonical "analytics" surface.
 */
public class ReportsAnalyticsTest extends BaseTest {

    @Test(description = "Admin Overview shows headcount donut + 7-day trend (analytics surface)")
    public void analyticsLiveOnDashboard() {
        DashboardPage dash = new LoginPage(driver)
                .login(AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD);
        Assert.assertTrue(dash.isHeadcountDonutVisible(),
                "Headcount donut should be visible (acts as the Reports surface in Rivio_Angular)");
        Assert.assertTrue(dash.isAttendanceTrendVisible(),
                "7-day Attendance Trend should be visible");
        ExtentManager.getTest().pass("Analytics visuals render on Admin Overview");
    }
}
