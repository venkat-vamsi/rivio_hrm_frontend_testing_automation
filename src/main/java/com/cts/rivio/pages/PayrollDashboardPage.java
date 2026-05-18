package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * PayrollDashboardPage – mirrors features/payroll/payroll-dashboard/payroll-dashboard.component.html
 * in Rivio_Angular-main.
 *
 * Real DOM (post-redesign):
 *   - <h1>Payroll Management</h1>
 *   - Two tab buttons (NOT p-tab): "Employee Salaries", "Pay Cycles & Slips"
 *     activeTab is a signal — clicking the tab triggers a synchronous re-render
 *   - Pay Cycles tab exposes a <button> "Initialize Pay Cycle" (icon + text)
 *   - Employee Salaries tab exposes a disabled "Add Component" button until
 *     an employee is selected
 */
public class PayrollDashboardPage {

    private final WebDriver driver;

    public PayrollDashboardPage(WebDriver driver) { this.driver = driver; }

    public boolean isPageLoaded() {
        return WaitUtils.waitForH1Text(driver, "Payroll Management", 15);
    }

    public void openEmployeeSalariesTab() {
        clickTabContaining("Employee Salaries");
        WaitUtils.waitForPresence(driver,
            By.xpath("//button[contains(.,'Add Component')]"), 10);
    }

    public void openPayCyclesTab() {
        clickTabContaining("Pay Cycles");
        // Wait until the tab content has switched — the Initialize Pay Cycle
        // button is rendered only when activeTab() === 'PAY_CYCLES'.
        WaitUtils.waitForPresence(driver,
            By.xpath("//button[contains(normalize-space(.),'Initialize Pay Cycle')]"), 15);
    }

    private void clickTabContaining(String partial) {
        WebElement btn = WaitUtils.waitForClickability(driver, By.xpath(
            "//button[contains(normalize-space(.),'" + partial + "')]"));
        WaitUtils.scrollAndClick(driver, btn);
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.hardWait(400);
    }

    public boolean isInitializePayCycleVisible() {
        return WaitUtils.waitForPresence(driver,
            By.xpath("//button[contains(normalize-space(.),'Initialize Pay Cycle')]"),
            15);
    }

    public boolean isAddComponentDisabledForNoEmployee() {
        try {
            WebElement btn = driver.findElement(By.xpath("//button[contains(.,'Add Component')]"));
            String dis = btn.getAttribute("disabled");
            return dis != null && !dis.isEmpty();
        } catch (Exception e) { return false; }
    }
}
