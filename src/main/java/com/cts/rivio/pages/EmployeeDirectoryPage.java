package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * EmployeeDirectoryPage – mirrors features/employees/employee-directory/employee-directory.html.
 */
public class EmployeeDirectoryPage {

    private final WebDriver driver;

    public EmployeeDirectoryPage(WebDriver driver) { this.driver = driver; }

    public boolean isPageLoaded() {
        boolean headerOk = WaitUtils.waitForH1Text(driver, "Employees", 15);
        if (!headerOk) return false;
        // p-table needs another tick to render rows or empty state
        WaitUtils.waitForPresence(driver, By.cssSelector("p-table, p-paginator"), 8);
        return true;
    }

    public String getPageHeading() {
        try { return driver.findElement(By.cssSelector("h1")).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public void searchEmployee(String text) {
        WebElement search = WaitUtils.waitForVisibility(driver,
            By.cssSelector("input[placeholder='Search employees...']"));
        search.clear();
        search.sendKeys(text);
        WaitUtils.waitForAngularLoad(driver);
    }

    public void clearSearch() {
        WebElement search = WaitUtils.waitForVisibility(driver,
            By.cssSelector("input[placeholder='Search employees...']"));
        search.sendKeys(Keys.CONTROL, "a");
        search.sendKeys(Keys.DELETE);
        WaitUtils.waitForAngularLoad(driver);
    }

    public int getRowCount() {
        return driver.findElements(By.cssSelector("p-table tbody tr")).size();
    }

    public void clickAddEmployee() {
        WebElement btn = WaitUtils.waitForClickability(driver, By.xpath(
            "//button[normalize-space()='Add Employee' or contains(.,'Add Employee')]"));
        WaitUtils.safeClick(driver, btn);
    }

    public boolean isOnboardModalOpen() {
        return WaitUtils.waitForPresence(driver,
            By.cssSelector("p-dialog .p-dialog, .p-dialog-mask .p-dialog"), 5);
    }

    public boolean isPaginationVisible() {
        return !driver.findElements(By.cssSelector(".p-paginator, p-paginator")).isEmpty();
    }

    public void openFirstEmployeeProfile() {
        WebElement viewBtn = WaitUtils.waitForClickability(driver,
            By.cssSelector("p-table tbody tr:first-child a[href*='/employees/'], "
                + "p-table tbody tr:first-child button[ng-reflect-router-link*='employees']"));
        WaitUtils.scrollAndClick(driver, viewBtn);
        WaitUtils.waitForAngularLoad(driver);
    }
}
