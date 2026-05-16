package com.cts.rivio.pages;

import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

public class DashboardPage {

    private WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────

    // Welcome / username in header — broad selector covering multiple Angular patterns
    @FindBy(css = ".user-name, .welcome-text, [class*='username'], [class*='user-name'], " +
                  ".p-menubar .user, [class*='profile-name']")
    private WebElement welcomeText;

    // Stat / KPI cards — very broad so it matches any card-style widget
    @FindBy(css = ".stat-card, .kpi-card, .summary-card, [class*='stat-card'], " +
                  "[class*='dashboard-card'], .p-card, [class*='metric-card']")
    private List<WebElement> statCards;

    // User avatar / profile picture in header
    @FindBy(css = ".avatar, .user-avatar, img.profile-pic, [class*='avatar'], " +
                  ".p-avatar, [class*='header-user'], [class*='profile-icon']")
    private WebElement userAvatar;

    // Notification bell — broad fallback chain
    @FindBy(css = ".notification-bell, [class*='notification'], button[aria-label*='notification'], " +
                  ".p-button[icon*='bell'], [class*='bell-icon'], [class*='notif-btn']")
    private WebElement notificationBell;

    // Module quick-access tiles
    @FindBy(css = ".module-card, .quick-access-card, [class*='module-tile'], " +
                  "[class*='menu-item'], .p-card[routerlink]")
    private List<WebElement> moduleTiles;

    // Charts
    @FindBy(css = "canvas, .chart-container, [class*='chart'], p-chart")
    private List<WebElement> charts;

    // Recent activity rows
    @FindBy(css = "table tbody tr, .activity-list li")
    private List<WebElement> recentActivityRows;

    // ── Constructor ───────────────────────────────────────────────────────────

    public DashboardPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    /**
     * Logout: tries profile menu click → logout link/button, with JS fallback.
     * Rivio may use a header dropdown menu for logout.
     */
    public void clickLogout() {
        // 1. Try to open profile/user menu
        try {
            WaitUtils.safeClick(driver, userAvatar);
            WaitUtils.hardWait(500);
        } catch (Exception ignored) {}

        // 2. Look for logout in visible menu
        By[] logoutLocators = {
            By.xpath("//a[normalize-space()='Logout' or normalize-space()='Sign Out' or normalize-space()='Log Out']"),
            By.xpath("//button[normalize-space()='Logout' or normalize-space()='Sign Out']"),
            By.xpath("//li[normalize-space()='Logout' or normalize-space()='Sign Out']"),
            By.xpath("//*[contains(@class,'logout') or contains(@id,'logout')]"),
            By.cssSelector("[routerlink*='logout'], [href*='logout']")
        };

        for (By locator : logoutLocators) {
            try {
                WebElement el = WaitUtils.waitForClickability(driver, locator);
                WaitUtils.safeClick(driver, el);
                WaitUtils.waitForUrlNotContains(driver, "dashboard");
                return;
            } catch (Exception ignored) {}
        }

        // Last resort: navigate to base URL (session will redirect to login if invalid)
        driver.get(AppConstants.BASE_URL);
    }

    public void clickNotificationBell() {
        WaitUtils.safeClick(driver, notificationBell);
    }

    public void navigateToModule(String moduleName) {
        for (WebElement tile : moduleTiles) {
            try {
                if (tile.getText().trim().equalsIgnoreCase(moduleName)) {
                    WaitUtils.scrollAndClick(driver, tile);
                    return;
                }
            } catch (StaleElementReferenceException ignored) {}
        }
        // Fallback: find by text anywhere in page
        try {
            WebElement el = driver.findElement(
                By.xpath("//*[normalize-space(text())='" + moduleName + "' and " +
                         "(self::a or self::button or ancestor::a or ancestor::button)]"));
            WaitUtils.scrollAndClick(driver, el);
        } catch (Exception e) {
            throw new NoSuchElementException("Module not found: " + moduleName);
        }
    }

    // ── Verifications ─────────────────────────────────────────────────────────

    /**
     * Checks if the dashboard has loaded.
     * Primary: URL contains "dashboard". Fallback: stat cards or welcome text present.
     */
    public boolean isDashboardLoaded() {
        // URL-based check is most reliable — works for all roles
        if (driver.getCurrentUrl().contains("dashboard")) {
            return true;
        }
        // Fallback: any dashboard widget present
        try {
            WaitUtils.waitForUrlContains(driver, "dashboard");
            return true;
        } catch (Exception ignored) {}
        // Second fallback: page has cards or navigation
        try {
            return !driver.findElements(By.cssSelector(
                ".p-card, [class*='dashboard'], [class*='stat-card'], " +
                "[class*='kpi'], nav a, .sidebar a")).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public String getWelcomeText() {
        try {
            WaitUtils.waitForVisibility(driver, welcomeText);
            return welcomeText.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public int getStatCardCount() {
        return statCards.size();
    }

    public String getStatCardValue(int index) {
        return statCards.get(index).getText().trim();
    }

    public boolean isModuleVisible(String moduleName) {
        try {
            WebElement el = driver.findElement(
                By.xpath("//*[contains(text(),'" + moduleName + "')]"));
            return el.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public int getChartCount() {
        return charts.size();
    }

    public boolean isRecentActivityTableVisible() {
        return !recentActivityRows.isEmpty();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getPageTitle() {
        return driver.getTitle();
    }

    // ── Page navigation helpers (URL-first for Angular SPA reliability) ───────

    public EmployeeDirectoryPage goToEmployeeDirectory() {
        navigateByUrlOrLink(AppConstants.EMPLOYEE_DIR_URL, "employee");
        return new EmployeeDirectoryPage(driver);
    }

    public LeaveDashboardPage goToLeave() {
        navigateByUrlOrLink(AppConstants.LEAVE_URL, "leave");
        return new LeaveDashboardPage(driver);
    }

    public AttendancePage goToAttendance() {
        navigateByUrlOrLink(AppConstants.ATTENDANCE_URL, "attendance");
        return new AttendancePage(driver);
    }

    public PayrollDashboardPage goToPayroll() {
        navigateByUrlOrLink(AppConstants.PAYROLL_URL, "payroll");
        return new PayrollDashboardPage(driver);
    }

    public RecruitmentDashboardPage goToRecruitment() {
        navigateByUrlOrLink(AppConstants.RECRUITMENT_URL, "recruitment");
        return new RecruitmentDashboardPage(driver);
    }

    public CompanyStructurePage goToCompany() {
        navigateByUrlOrLink(AppConstants.COMPANY_URL, "company");
        return new CompanyStructurePage(driver);
    }

    /**
     * Navigates to a module:
     * 1. Try clicking the sidebar/nav link by text.
     * 2. Fall back to direct URL navigation.
     */
    private void navigateByUrlOrLink(String url, String linkKeyword) {
        try {
            WebElement link = WaitUtils.waitForClickability(driver,
                By.xpath("//nav//a[contains(translate(@href,'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
                         "'abcdefghijklmnopqrstuvwxyz'),'" + linkKeyword + "')] | " +
                         "//nav//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
                         "'abcdefghijklmnopqrstuvwxyz'),'" + linkKeyword + "')]"));
            WaitUtils.safeClick(driver, link);
        } catch (Exception e) {
            driver.get(url);
        }
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.dismissPopupsIfPresent(driver);
    }
}
