package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

public class NotificationsPage {

    private WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────

    // Bell icon — broad multi-fallback covering PrimeNG, Angular Material, Bootstrap, custom
    @FindBy(css = ".notification-bell, [class*='notification-bell'], " +
                  "button[aria-label*='notification' i], button[aria-label*='bell' i], " +
                  "[class*='notif-btn'], [class*='bell-btn'], [class*='bell-icon'], " +
                  "button[class*='notification'], .header-notification, " +
                  "[class*='header'] button[class*='notif'], " +
                  "[class*='topbar'] button, [class*='navbar'] [class*='notif']")
    private WebElement notificationBell;

    @FindBy(css = ".notification-badge, .notif-count, [class*='badge-count'], " +
                  ".p-badge, [class*='badge'], [class*='unread-count']")
    private WebElement unreadBadge;

    @FindBy(css = ".notification-panel, .notif-dropdown, [class*='notifications-list'], " +
                  "[class*='notification-panel'], [class*='notif-panel'], " +
                  ".p-overlaypanel, [class*='dropdown-menu'][class*='notif']")
    private WebElement notificationPanel;

    @FindBy(css = ".notification-item, .notif-card, [class*='notif-item'], " +
                  "[class*='notification-item'], li[class*='notif']")
    private List<WebElement> notificationItems;

    @FindBy(css = ".notification-item .message, .notif-card .text, [class*='notif-text'], " +
                  "[class*='notification-message'], [class*='notif-body']")
    private List<WebElement> notificationMessages;

    @FindBy(xpath = "//button[contains(normalize-space(),'Mark') and " +
                    "contains(normalize-space(),'Read')] | " +
                    "//a[contains(normalize-space(),'Mark') and contains(normalize-space(),'Read')]")
    private WebElement markAllReadButton;

    @FindBy(css = ".notif-filter-tab, [class*='notif-type-tab'], " +
                  "[class*='notification-tab']")
    private List<WebElement> filterTabs;

    @FindBy(css = ".no-notifications, .empty-notif, [class*='notif-empty'], " +
                  "[class*='no-data'], [class*='empty-state']")
    private WebElement emptyState;

    // ── Constructor ───────────────────────────────────────────────────────────

    public NotificationsPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public void clickNotificationBell() {
        // Dismiss any overlay that might be blocking the bell
        WaitUtils.dismissPopupsIfPresent(driver);
        WaitUtils.waitForClickability(driver, notificationBell);
        WaitUtils.safeClick(driver, notificationBell);
        WaitUtils.hardWait(500);
    }

    public void clickMarkAllRead() {
        WaitUtils.waitForClickability(driver, markAllReadButton);
        WaitUtils.safeClick(driver, markAllReadButton);
    }

    public void clickFilterTab(String tabName) {
        for (WebElement tab : filterTabs) {
            try {
                if (tab.getText().trim().equalsIgnoreCase(tabName)) {
                    WaitUtils.safeClick(driver, tab);
                    return;
                }
            } catch (StaleElementReferenceException ignored) {}
        }
    }

    // ── Verifications ─────────────────────────────────────────────────────────

    public boolean isNotificationPanelOpen() {
        try {
            WaitUtils.waitForVisibility(driver, notificationPanel);
            return notificationPanel.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public int getNotificationCount() {
        return notificationItems.size();
    }

    public String getUnreadBadgeText() {
        try {
            WaitUtils.waitForVisibility(driver, unreadBadge);
            return unreadBadge.getText().trim();
        } catch (Exception e) {
            return "0";
        }
    }

    public boolean hasUnreadNotifications() {
        try {
            String text = getUnreadBadgeText();
            return !text.isEmpty() && !text.equals("0");
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isNotificationPresent(String keyword) {
        return notificationMessages.stream()
                .anyMatch(el -> {
                    try {
                        return el.getText().toLowerCase().contains(keyword.toLowerCase());
                    } catch (Exception e) {
                        return false;
                    }
                });
    }

    public String getNotificationTextAtIndex(int index) {
        if (index < notificationMessages.size()) {
            try {
                return notificationMessages.get(index).getText().trim();
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }

    public boolean isEmptyStateVisible() {
        try {
            return emptyState.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
}
