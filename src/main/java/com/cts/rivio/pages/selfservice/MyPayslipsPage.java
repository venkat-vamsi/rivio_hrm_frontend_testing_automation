package com.cts.rivio.pages.selfservice;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * MyPayslipsPage – mirrors features/self-service/employee-payslips/employee-payslips.component.html.
 *
 * Real DOM:
 *   - <h1>My Payslips</h1>
 *   - p-table with columns: Pay Cycle, Gross Earnings, Deductions & Tax, Net Take Home, View Slip
 *   - "View Slip" button per row → opens p-dialog
 */
public class MyPayslipsPage {

    private final WebDriver driver;

    public MyPayslipsPage(WebDriver driver) { this.driver = driver; }

    public boolean isPageLoaded() {
        return com.cts.rivio.utils.WaitUtils.waitForH1Text(driver, "My Payslips", 15);
    }

    public int getPayslipRowCount() {
        return driver.findElements(By.cssSelector("p-table tbody tr")).size();
    }

    public boolean hasViewSlipButtons() {
        return !driver.findElements(By.xpath("//button[normalize-space()='View Slip']")).isEmpty();
    }

    public void clickFirstViewSlip() {
        WebElement btn = WaitUtils.waitForClickability(driver,
            By.xpath("//button[normalize-space()='View Slip'][1]"));
        WaitUtils.safeClick(driver, btn);
    }

    public boolean isSlipModalOpen() {
        return !driver.findElements(By.cssSelector("p-dialog .p-dialog")).isEmpty();
    }
}
