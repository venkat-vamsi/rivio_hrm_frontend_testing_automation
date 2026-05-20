package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.DashboardPage;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * DashboardTest – Test Scenarios DASH-S-01..DASH-S-05.
 *
 * Mapped test cases:
 *   RV_DASH_001 – Four KPI cards render with live values
 *   RV_DASH_002 – Each KPI card navigates to its module (Present Today → Attendance,
 *                  On Leave → Leave Approvals,
 *                 Active Pay Cycles → Payroll)
 *   RV_DASH_003 – Headcount by Department donut chart
 *   RV_DASH_004 – 7-day Attendance Trend line chart
 *   RV_DASH_005 – Pending Leave Requests table
 *   RV_DASH_006 – SYSTEM ONLINE badge + refresh button always visible
 */
public class DashboardTest extends BaseTest {

    @Override protected String getRole() { return ROLE_ADMIN; }

    private DashboardPage dashboard;

    @BeforeMethod
    public void openDashboard() {
        // Bucket session is already logged in as Admin via BaseTest @BeforeClass.
        driver.get(AppConstants.DASHBOARD_URL);
        com.cts.rivio.utils.WaitUtils.waitForUrlContains(driver, "/dashboard");
        com.cts.rivio.utils.WaitUtils.waitForH1Text(driver, "Admin Overview", 15);
        com.cts.rivio.utils.WaitUtils.waitForAngularLoad(driver);
        dashboard = new DashboardPage(driver);
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

    /**
     * RV-BUG-NEW-03: UI defect — when the sidebar is collapsed to its narrow
     * rail (w-20 ≈ 80px), the Ask Rivio button does NOT collapse its content
     * like the other nav items do. Looking at Rivio_Angular-main
     * sidebar.component.html lines 58-67, the gradient Ask Rivi anchor has no
     * `[class.opacity-0]="layoutState.isSidebarCollapsed()"` toggle on its
     * inner "Ask Rivi AI" span (lines 38-40 of the same file apply that
     * toggle to every OTHER nav label). The result is the AI button content
     * gets squashed/clipped inside the narrow rail when collapsed.
     *
     * This test collapses the sidebar via the collapse button (line 113-118)
     * then asserts the "Ask Rivi" inner span is hidden / dimensionally
     * collapsed — matching the convention used by every other sidebar entry.
     */
    @Test(priority = 7, description =
        "RV_DASH_BUG_03 – Ask Rivio button must collapse cleanly when sidebar is minimised (no image compression)")
    public void RV_DASH_BUG_03_askRivioImageNotCompressedWhenSidebarMinimised() {
        // Collapse the sidebar via its dedicated toggle (pi-angle-left button).
        WebElement collapseBtn = null;
        try {
            collapseBtn = driver.findElement(By.xpath(
                "//aside//button[.//i[contains(@class,'pi-angle-left')]] | "
              + "//aside//button[contains(@class,'sidebar-toggle')]"));
        } catch (Exception ignored) {}
        if (collapseBtn != null) {
            WaitUtils.scrollAndClick(driver, collapseBtn);
            WaitUtils.hardWait(600);
        }

        // Confirm the aside is in the narrow / collapsed rail.
        int asideWidth = 0;
        try {
            WebElement aside = driver.findElement(By.cssSelector("aside"));
            asideWidth = aside.getSize().getWidth();
        } catch (Exception ignored) {}
        ExtentManager.getTest().info("Sidebar width after collapse: " + asideWidth + "px");

        // Locate the Ask Rivio anchor's inner text label ("Ask Rivi" with AI badge).
        List<WebElement> labels = driver.findElements(By.xpath(
            "//aside//a[contains(@href,'/ask-rivi') or contains(@ng-reflect-router-link,'/ask-rivi')]"
          + "//span[contains(normalize-space(.),'Ask Rivi')]"));
        Assert.assertFalse(labels.isEmpty(),
                "Ask Rivio button not found in the sidebar — cannot test compression");
        WebElement label = labels.get(0);

        int labelWidth  = 0;
        int labelHeight = 0;
        try {
            labelWidth  = label.getSize().getWidth();
            labelHeight = label.getSize().getHeight();
        } catch (Exception ignored) {}
        String labelClass = "";
        try { labelClass = label.getAttribute("class"); } catch (Exception ignored) {}

        ExtentManager.getTest().info("Ask Rivi label rendered " + labelWidth + "x"
            + labelHeight + " | class='" + labelClass + "'");

        // Other sidebar labels become opacity-0 when collapsed. Ask Rivi's label
        // SHOULD do the same. If it doesn't, the text squashes inside the rail.
        boolean labelProperlyHidden =
               (labelClass != null && labelClass.contains("opacity-0"))
            || labelWidth == 0
            || labelHeight == 0
            || !label.isDisplayed();

        Assert.assertTrue(labelProperlyHidden,
                "RV-BUG-NEW-03: When the sidebar is collapsed (width ≈ " + asideWidth
              + "px), the Ask Rivio button's 'Ask Rivi' label is still rendered at "
              + labelWidth + "x" + labelHeight + " inside the narrow rail. Every other "
              + "sidebar entry hides its text via `opacity-0` when collapsed — the "
              + "gradient Ask Rivio anchor (sidebar.component.html lines 58-67) is "
              + "missing that toggle, so its content gets compressed in the narrow rail.");
        ExtentManager.getTest().pass("Ask Rivio label correctly collapses with the sidebar");
    }
}
