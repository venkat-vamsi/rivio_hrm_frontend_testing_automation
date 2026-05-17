package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * LeaveDashboardPage – mirrors features/leave/leave-dashboard/leave-dashboard.component.html.
 *
 * Real DOM:
 *   - <h1>Leave Approvals</h1>
 *   - "Action Required" badge with pending count
 *   - Pending requests p-table
 *   - Each row has "Review" button → opens approval modal (p-dialog)
 *   - Reject in modal requires a reason
 */
public class LeaveDashboardPage {

    private final WebDriver driver;

    public LeaveDashboardPage(WebDriver driver) { this.driver = driver; }

    public boolean isPageLoaded() {
        return WaitUtils.waitForH1Text(driver, "Leave Approvals", 15);
    }

    public boolean isActionRequiredBadgeVisible() {
        return !driver.findElements(By.xpath("//*[contains(translate(.,'ACTIONREQUIRED','actionrequired'),'action required')]")).isEmpty();
    }

    public int getLeaveRequestCount() {
        return driver.findElements(By.cssSelector("p-table tbody tr")).size();
    }

    public boolean hasReviewButton() {
        return !driver.findElements(By.xpath("//p-table//button[normalize-space()='Review']")).isEmpty();
    }

    public void clickFirstReview() {
        WebElement btn = WaitUtils.waitForClickability(driver,
            By.xpath("//p-table//button[normalize-space()='Review'][1]"));
        WaitUtils.safeClick(driver, btn);
        WaitUtils.waitForAngularLoad(driver);
    }

    public boolean isReviewModalOpen() {
        return !driver.findElements(By.cssSelector("p-dialog .p-dialog")).isEmpty();
    }
}
