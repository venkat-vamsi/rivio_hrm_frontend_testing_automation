package com.cts.rivio.pages.selfservice;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * MyLeavesPage – mirrors features/self-service/employee-leaves/employee-leaves.component.html.
 *
 * Real DOM:
 *   - <h1>My Leaves</h1>
 *   - "Apply for Leave" button
 *   - Balance cards each show Available / Used / Total
 *   - Leave Request History p-table below
 */
public class MyLeavesPage {

    private final WebDriver driver;

    public MyLeavesPage(WebDriver driver) { this.driver = driver; }

    public boolean isPageLoaded() {
        return com.cts.rivio.utils.WaitUtils.waitForH1Text(driver, "My Leaves", 15);
    }

    public void clickApplyForLeave() {
        WebElement btn = WaitUtils.waitForClickability(driver,
            By.xpath("//button[contains(.,'Apply for Leave')]"));
        WaitUtils.safeClick(driver, btn);
    }

    public int getBalanceCardCount() {
        // Cards under "Current Balances" header
        return driver.findElements(By.cssSelector(
            ".glass-panel.p-6.relative.overflow-hidden")).size();
    }

    public int getHistoryRowCount() {
        return driver.findElements(By.cssSelector("p-table tbody tr")).size();
    }

    public boolean isApplyModalOpen() {
        return !driver.findElements(By.cssSelector("p-dialog .p-dialog")).isEmpty();
    }
}
