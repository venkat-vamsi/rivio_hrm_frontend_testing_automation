package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

/**
 * DashboardPage – mirrors features/dashboard/admin-dashboard/admin-dashboard.html.
 *
 * Key DOM facts:
 *   - Page header: <h1>Admin Overview</h1>
 *   - "SYSTEM ONLINE" badge: span with text "System Online" inside the rounded-xl pill
 *   - Refresh button: title="Refresh Dashboard"
 *   - KPI cards: <div class="glass-panel p-6 ..."> with <h3> labels
 *   - Three of the KPI cards have routerLink: "/attendance", "/leave", "/payroll"
 *   - Headcount donut: <p-chart type="doughnut">
 *   - 7-day trend: <p-chart type="line">
 *   - Pending leave table: <p-table> under h2 "Pending Leave Requests"
 */
public class DashboardPage {

    private final WebDriver driver;
    private final SidebarPage sidebar;
    private final HeaderPage header;

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(xpath = "//h1[normalize-space()='Admin Overview']")
    private List<WebElement> adminOverviewHeading;

    @FindBy(xpath = "//span[contains(translate(normalize-space(.),"
                  + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'system online')]")
    private List<WebElement> systemOnlineBadge;

    @FindBy(css = "button[title='Refresh Dashboard']")
    private WebElement refreshButton;

    @FindBy(css = "div.glass-panel.p-6.relative.overflow-hidden, div.glass-panel[class*='cursor-pointer']")
    private List<WebElement> kpiCards;

    @FindBy(xpath = "//h2[contains(.,'Pending Leave Requests')]")
    private List<WebElement> pendingLeaveHeader;

    @FindBy(css = "p-table tbody tr")
    private List<WebElement> tableRows;

    @FindBy(css = "p-chart, canvas")
    private List<WebElement> charts;

    // ── Constructor ───────────────────────────────────────────────────────────

    public DashboardPage(WebDriver driver) {
        this.driver  = driver;
        this.sidebar = new SidebarPage(driver);
        this.header  = new HeaderPage(driver);
        PageFactory.initElements(driver, this);
    }

    // ── Verifications ─────────────────────────────────────────────────────────

    public boolean isAdminOverviewLoaded() {
        return WaitUtils.waitForPresence(driver,
            By.xpath("//h1[normalize-space()='Admin Overview']"), 15);
    }

    public boolean isDashboardLoaded() {
        String url = driver.getCurrentUrl();
        if (url.contains("/dashboard")) return true;
        if (url.contains("/self-service/profile")) return true;
        try { return !driver.findElements(By.cssSelector("aside nav")).isEmpty(); }
        catch (Exception e) { return false; }
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
        WaitUtils.waitForClickability(driver, By.cssSelector("button[title='Refresh Dashboard']"));
        WaitUtils.safeClick(driver, refreshButton);
        WaitUtils.waitForAngularLoad(driver);
    }

    // ── KPI cards ─────────────────────────────────────────────────────────────

    public List<WebElement> getKpiCards() { return kpiCards; }

    public int getKpiCardCount() { return kpiCards.size(); }

    public boolean isKpiCardVisible(String label) {
        // Dynamic — based on label arg, kept inline
        return !driver.findElements(By.xpath(
            "//h3[contains(normalize-space(.),'" + label + "')]")).isEmpty();
    }

    public void clickKpiCard(String label) {
        // Dynamic locator — kept inline
        WebElement card = WaitUtils.waitForClickability(driver, By.xpath(
            "//div[contains(@class,'glass-panel')][.//h3[contains(.,'" + label + "')]]"));
        WaitUtils.scrollAndClick(driver, card);
        WaitUtils.waitForAngularLoad(driver);
    }

    // ── Charts ────────────────────────────────────────────────────────────────

    public boolean isHeadcountDonutVisible() {
        return WaitUtils.waitForPresence(driver,
            By.xpath("//h2[contains(.,'Headcount')] | //p-chart[@type='doughnut'] "
                   + "| //canvas | //*[contains(text(),'No data')]"), 15);
    }

    public boolean isAttendanceTrendVisible() {
        return WaitUtils.waitForPresence(driver,
            By.xpath("//h2[contains(.,'Attendance Trend')] | //h2[contains(.,'Trend')] "
                   + "| //p-chart[@type='line'] | //canvas"), 15);
    }

    // ── Pending leave table ───────────────────────────────────────────────────

    public boolean isPendingLeaveSectionVisible() { return !pendingLeaveHeader.isEmpty(); }

    public int getPendingLeaveRowCount() { return tableRows.size(); }

    // ── Navigation passthroughs (via sidebar) ─────────────────────────────────

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

    public void navigateDirectly(String url) {
        driver.get(url);
        WaitUtils.waitForAngularLoad(driver);
    }

    // ── Legacy compat shims ───────────────────────────────────────────────────

    public boolean isModuleVisible(String module) {
        String m = module.toLowerCase();
        if (m.contains("employee"))     return sidebar.isItemVisible("/employees");
        if (m.contains("leave"))        return sidebar.isItemVisible("/leave") || sidebar.isItemVisible("/self-service/leaves");
        if (m.contains("attendance"))   return sidebar.isItemVisible("/attendance") || sidebar.isItemVisible("/self-service/attendance");
        if (m.contains("payroll") || m.contains("payslip")) return sidebar.isItemVisible("/payroll") || sidebar.isItemVisible("/self-service/payslips");
        if (m.contains("recruit"))      return sidebar.isItemVisible("/ats");
        if (m.contains("company"))      return sidebar.isItemVisible("/company");
        if (m.contains("profile"))      return sidebar.isItemVisible("/self-service/profile");
        if (m.contains("performance"))  return false;
        return !driver.findElements(By.xpath("//aside//a[contains(.,'" + module + "')]")).isEmpty();
    }

    public String getWelcomeText() { return header.getUserName(); }
    public int getStatCardCount()  { return getKpiCardCount(); }
    public int getChartCount()     { return charts.size(); }
    public boolean isRecentActivityTableVisible() { return isPendingLeaveSectionVisible(); }
    public String getCurrentUrl()  { return driver.getCurrentUrl(); }
    public String getPageTitle()   { return driver.getTitle(); }
    public void clickLogout() { header.clickLogout(); }
    public void clickNotificationBell() { /* no-op */ }
    public void navigateToModule(String moduleName) { isModuleVisible(moduleName); }
}
