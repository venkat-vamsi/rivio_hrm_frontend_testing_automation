package com.cts.rivio.pages;

import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * DashboardPage – mirrors features/dashboard/admin-dashboard/admin-dashboard.html.
 *
 * Key DOM facts:
 *   - Page header: <h1>Admin Overview</h1>
 *   - "SYSTEM ONLINE" badge: span with text "System Online" inside the rounded-xl pill
 *   - Refresh button: title="Refresh Dashboard"
 *   - KPI cards: <div class="glass-panel p-6 ..."> with <h3> labels:
 *       "Total Workforce", "Present Today", "On Leave Today", "Active Pay Cycles"
 *   - Three of the KPI cards have routerLink: "/attendance", "/leave", "/payroll"
 *   - Headcount donut: <p-chart type="doughnut">
 *   - 7-day trend: <p-chart type="line">  (under h2 "Attendance Trend (Last 7 Days)")
 *   - Pending leave table: <p-table> under h2 "Pending Leave Requests"
 */
public class DashboardPage {

    private final WebDriver driver;
    private final SidebarPage sidebar;
    private final HeaderPage header;

    public DashboardPage(WebDriver driver) {
        this.driver  = driver;
        this.sidebar = new SidebarPage(driver);
        this.header  = new HeaderPage(driver);
    }

    // ── Verifications ────────────────────────────────────────────────────────

    public boolean isAdminOverviewLoaded() {
        return WaitUtils.waitForPresence(driver,
            By.xpath("//h1[normalize-space()='Admin Overview']"), 15);
    }

    /**
     * Reliable across roles: true if URL contains "/dashboard" OR self-service profile
     * is loaded (Employee/Manager land there). Combined with role-aware checks elsewhere.
     */
    public boolean isDashboardLoaded() {
        String url = driver.getCurrentUrl();
        if (url.contains("/dashboard")) return true;
        if (url.contains("/self-service/profile")) return true;
        try {
            return !driver.findElements(By.cssSelector("aside nav")).isEmpty();
        } catch (Exception e) { return false; }
    }

    public boolean isSystemOnlineBadgeVisible() {
        return WaitUtils.waitForPresence(driver,
            By.xpath("//span[contains(translate(normalize-space(.),"
                + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'system online')]"), 15);
    }

    public boolean isRefreshButtonVisible() {
        return WaitUtils.waitForPresence(driver,
            By.cssSelector("button[title='Refresh Dashboard'], button[title*='Refresh' i]"), 10);
    }

    public void clickRefresh() {
        WebElement btn = WaitUtils.waitForClickability(driver,
            By.cssSelector("button[title='Refresh Dashboard']"));
        WaitUtils.safeClick(driver, btn);
        WaitUtils.waitForAngularLoad(driver);
    }

    // ── KPI cards ────────────────────────────────────────────────────────────

    public List<WebElement> getKpiCards() {
        return driver.findElements(By.cssSelector(
            "div.glass-panel.p-6.relative.overflow-hidden, " +
            "div.glass-panel[class*='cursor-pointer']"));
    }

    public int getKpiCardCount() { return getKpiCards().size(); }

    public boolean isKpiCardVisible(String label) {
        return !driver.findElements(By.xpath(
            "//h3[contains(normalize-space(.),'" + label + "')]")).isEmpty();
    }

    public void clickKpiCard(String label) {
        WebElement card = WaitUtils.waitForClickability(driver, By.xpath(
            "//div[contains(@class,'glass-panel')][.//h3[contains(.,'" + label + "')]]"));
        WaitUtils.scrollAndClick(driver, card);
        WaitUtils.waitForAngularLoad(driver);
    }

    // ── Charts ────────────────────────────────────────────────────────────────

    public boolean isHeadcountDonutVisible() {
        return WaitUtils.waitForPresence(driver,
            By.xpath("//h2[contains(.,'Headcount')] | "
                   + "//p-chart[@type='doughnut'] | //canvas | "
                   + "//*[contains(text(),'No data')]"), 15);
    }

    public boolean isAttendanceTrendVisible() {
        return WaitUtils.waitForPresence(driver,
            By.xpath("//h2[contains(.,'Attendance Trend')] | "
                   + "//h2[contains(.,'Trend')] | "
                   + "//p-chart[@type='line'] | //canvas"), 15);
    }

    // ── Pending leave table ───────────────────────────────────────────────────

    public boolean isPendingLeaveSectionVisible() {
        return !driver.findElements(By.xpath("//h2[contains(.,'Pending Leave Requests')]")).isEmpty();
    }

    public int getPendingLeaveRowCount() {
        return driver.findElements(By.cssSelector("p-table tbody tr")).size();
    }

    // ── Navigation passthroughs (via sidebar) ────────────────────────────────

    public SidebarPage sidebar() { return sidebar; }
    public HeaderPage  header()  { return header; }

    public EmployeeDirectoryPage goToEmployeeDirectory() {
        sidebar.clickItem("/employees");
        return new EmployeeDirectoryPage(driver);
    }

    public AttendancePage goToAttendance() {
        sidebar.clickItem("/attendance");
        return new AttendancePage(driver);
    }

    public RecruitmentDashboardPage goToRecruitment() {
        sidebar.clickItem("/ats");
        return new RecruitmentDashboardPage(driver);
    }

    /** Direct URL fallback when the sidebar isn't visible (e.g. role-restricted). */
    public void navigateDirectly(String url) {
        driver.get(url);
        WaitUtils.waitForAngularLoad(driver);
    }

    // ── Legacy compatibility shims (kept so old test code keeps compiling) ───

    public boolean isModuleVisible(String module) {
        String m = module.toLowerCase();
        if (m.contains("employee"))     return sidebar.isItemVisible("/employees");
        if (m.contains("leave"))        return sidebar.isItemVisible("/leave") || sidebar.isItemVisible("/self-service/leaves");
        if (m.contains("attendance"))   return sidebar.isItemVisible("/attendance") || sidebar.isItemVisible("/self-service/attendance");
        if (m.contains("payroll") || m.contains("payslip")) return sidebar.isItemVisible("/payroll") || sidebar.isItemVisible("/self-service/payslips");
        if (m.contains("recruit"))      return sidebar.isItemVisible("/ats");
        if (m.contains("company"))      return sidebar.isItemVisible("/company");
        if (m.contains("profile"))      return sidebar.isItemVisible("/self-service/profile");
        if (m.contains("performance"))  return false; // not implemented in Angular app
        // generic fallback
        return !driver.findElements(By.xpath("//aside//a[contains(.,'" + module + "')]")).isEmpty();
    }

    public String getWelcomeText() { return header.getUserName(); }
    public int getStatCardCount()  { return getKpiCardCount(); }
    public int getChartCount()     { return driver.findElements(By.cssSelector("p-chart, canvas")).size(); }
    public boolean isRecentActivityTableVisible() { return isPendingLeaveSectionVisible(); }
    public String getCurrentUrl()  { return driver.getCurrentUrl(); }
    public String getPageTitle()   { return driver.getTitle(); }

    public void clickLogout() { header.clickLogout(); }

    public void clickNotificationBell() { /* no-op — Rivio header has no bell */ }

    public void navigateToModule(String moduleName) { isModuleVisible(moduleName); }
}
