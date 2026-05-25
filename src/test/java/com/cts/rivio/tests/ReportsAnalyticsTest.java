package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.DashboardPage;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * ReportsAnalyticsTest – Rivio_Angular doesn't have a dedicated Reports module;
 * analytics live on the Admin Overview (donut chart + 7-day trend). These tests
 * use the dashboard charts as the canonical "analytics" surface.
 */
public class ReportsAnalyticsTest extends BaseTest {

    @Override protected String getRole() { return ROLE_ADMIN; }

    @Test(groups = {"regression"}, description = "Admin Overview shows headcount donut + 7-day trend (analytics surface)")
    public void analyticsLiveOnDashboard() {
        // Bucket session is already logged in as Admin via BaseTest @BeforeClass.
        driver.get(AppConstants.DASHBOARD_URL);
        com.cts.rivio.utils.WaitUtils.waitForAngularLoad(driver);
        DashboardPage dash = new DashboardPage(driver);
        Assert.assertTrue(dash.isHeadcountDonutVisible(),
                "Headcount donut should be visible (acts as the Reports surface in Rivio_Angular)");
        Assert.assertTrue(dash.isAttendanceTrendVisible(),
                "7-day Attendance Trend should be visible");
        ExtentManager.getTest().pass("Analytics visuals render on Admin Overview");
    }
}
