package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * PayrollDashboardPage – mirrors features/payroll/payroll-dashboard/payroll-dashboard.component.html.
 *
 * Real DOM:
 *   - <h1>Payroll Management</h1>
 *   - Two tabs: "Employee Salaries", "Pay Cycles & Slips"
 *   - p-select dropdown to pick an employee
 *   - "+ Add Component" button when an employee is selected
 *   - "Initialize Pay Cycle" button in Pay Cycles tab
 */
public class PayrollDashboardPage {

    private final WebDriver driver;

    public PayrollDashboardPage(WebDriver driver) { this.driver = driver; }

    public boolean isPageLoaded() {
        return WaitUtils.waitForH1Text(driver, "Payroll Management", 15);
    }

    public void openEmployeeSalariesTab() { clickTab("Employee Salaries"); }
    public void openPayCyclesTab()        { clickTab("Pay Cycles & Slips"); }

    private void clickTab(String label) {
        WebElement btn = WaitUtils.waitForClickability(driver,
            By.xpath("//button[normalize-space()='" + label + "']"));
        WaitUtils.safeClick(driver, btn);
        WaitUtils.waitForAngularLoad(driver);
    }

    public boolean isInitializePayCycleVisible() {
        return !driver.findElements(By.xpath("//button[contains(.,'Initialize Pay Cycle')]")).isEmpty();
    }

    public boolean isAddComponentDisabledForNoEmployee() {
        try {
            WebElement btn = driver.findElement(By.xpath("//button[contains(.,'Add Component')]"));
            String dis = btn.getAttribute("disabled");
            return dis != null && !dis.isEmpty();
        } catch (Exception e) { return false; }
    }
}
