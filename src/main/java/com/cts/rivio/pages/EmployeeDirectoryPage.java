package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

/**
 * EmployeeDirectoryPage – mirrors features/employees/employee-directory/employee-directory.html.
 */
public class EmployeeDirectoryPage {

    private final WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(css = "h1")
    private WebElement pageHeading;

    @FindBy(css = "input[placeholder='Search employees...']")
    private WebElement searchInput;

    @FindBy(css = "p-table tbody tr")
    private List<WebElement> tableRows;

    @FindBy(xpath = "//button[normalize-space()='Add Employee' or contains(.,'Add Employee')]")
    private WebElement addEmployeeButton;

    @FindBy(css = ".p-paginator, p-paginator")
    private List<WebElement> paginator;

    // ── Constructor ───────────────────────────────────────────────────────────

    public EmployeeDirectoryPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public boolean isPageLoaded() {
        boolean headerOk = WaitUtils.waitForH1Text(driver, "Employees", 15);
        if (!headerOk) return false;
        // p-table needs another tick to render rows or empty state
        WaitUtils.waitForPresence(driver, By.cssSelector("p-table, p-paginator"), 8);
        return true;
    }

    public String getPageHeading() {
        try { return pageHeading.getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public void searchEmployee(String text) {
        WaitUtils.waitForVisibility(driver, searchInput);
        searchInput.clear();
        searchInput.sendKeys(text);
        WaitUtils.waitForAngularLoad(driver);
    }

    public void clearSearch() {
        WaitUtils.waitForVisibility(driver, searchInput);
        searchInput.sendKeys(Keys.CONTROL, "a");
        searchInput.sendKeys(Keys.DELETE);
        WaitUtils.waitForAngularLoad(driver);
    }

    public int getRowCount() {
        return tableRows.size();
    }

    public void clickAddEmployee() {
        WaitUtils.waitForClickability(driver, By.xpath(
            "//button[normalize-space()='Add Employee' or contains(.,'Add Employee')]"));
        WaitUtils.safeClick(driver, addEmployeeButton);
    }

    public boolean isOnboardModalOpen() {
        return WaitUtils.waitForPresence(driver,
            By.cssSelector("p-dialog .p-dialog, .p-dialog-mask .p-dialog"), 5);
    }

    public boolean isPaginationVisible() {
        return !paginator.isEmpty();
    }

    public void openFirstEmployeeProfile() {
        // Dynamic — first-row varies, kept as inline By
        WebElement viewBtn = WaitUtils.waitForClickability(driver,
            By.cssSelector("p-table tbody tr:first-child a[href*='/employees/'], "
                + "p-table tbody tr:first-child button[ng-reflect-router-link*='employees']"));
        WaitUtils.scrollAndClick(driver, viewBtn);
        WaitUtils.waitForAngularLoad(driver);
    }
}
