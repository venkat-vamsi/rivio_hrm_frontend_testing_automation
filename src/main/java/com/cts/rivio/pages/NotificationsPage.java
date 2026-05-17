package com.cts.rivio.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * NotificationsPage – stub.
 * The current Rivio Angular app has no notifications UI in the header.
 * Tests that depended on it should be treated as out-of-scope until the
 * feature is implemented.
 */
public class NotificationsPage {

    private final WebDriver driver;

    public NotificationsPage(WebDriver driver) { this.driver = driver; }

    public boolean isNotificationsPanelVisible() {
        return !driver.findElements(By.cssSelector("[class*='notif']")).isEmpty();
    }

    public int getUnreadCount() { return 0; }
    public void markAllAsRead() { /* no-op */ }
}
