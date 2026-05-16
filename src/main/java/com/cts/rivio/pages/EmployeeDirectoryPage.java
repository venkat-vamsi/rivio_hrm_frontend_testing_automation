package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;
import java.util.stream.Collectors;

public class EmployeeDirectoryPage {

    private WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(css = ".page-title, h1, h2")
    private WebElement pageTitle;

    @FindBy(css = "input[placeholder*='Search' i], input[placeholder*='search' i], " +
                  ".search-input, input[formcontrolname*='search' i], input[type='search']")
    private WebElement searchInput;

    @FindBy(css = "select[name*='department' i], select.department-filter, " +
                  "p-dropdown[formcontrolname*='department' i]")
    private WebElement departmentFilter;

    @FindBy(css = "table tbody tr, .employee-card, [class*='employee-row'], " +
                  "[class*='emp-row'], p-table tbody tr")
    private List<WebElement> employeeRows;

    @FindBy(css = "table tbody tr td:first-child, .employee-name, [class*='emp-name']")
    private List<WebElement> employeeNameCells;

    // "Add Employee" / "Onboard" button — XPath replaces invalid :contains() CSS
    @FindBy(xpath = "//button[contains(normalize-space(),'Add Employee') or " +
                    "contains(normalize-space(),'Onboard') or " +
                    "contains(normalize-space(),'Add New') or " +
                    "contains(@routerlink,'onboard')]")
    private WebElement addEmployeeButton;

    @FindBy(css = ".pagination, nav[aria-label='pagination'], [class*='paginator'], " +
                  "p-paginator")
    private WebElement paginationBar;

    @FindBy(css = ".pagination .next, button[aria-label='next'], [class*='next-page'], " +
                  "p-paginator [aria-label='Next Page']")
    private WebElement nextPageButton;

    @FindBy(css = ".pagination .prev, button[aria-label='previous'], [class*='prev-page'], " +
                  "p-paginator [aria-label='Previous Page']")
    private WebElement prevPageButton;

    @FindBy(css = ".total-count, .record-count, [class*='total'], [class*='count-label']")
    private WebElement totalCountLabel;

    // ── Constructor ───────────────────────────────────────────────────────────

    public EmployeeDirectoryPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public void searchEmployee(String keyword) {
        WaitUtils.waitForVisibility(driver, searchInput);
        searchInput.clear();
        searchInput.sendKeys(keyword);
        WaitUtils.waitForAngularLoad(driver);
    }

    public void clearSearch() {
        searchInput.clear();
        searchInput.sendKeys(Keys.ENTER);
        WaitUtils.waitForAngularLoad(driver);
    }

    public void filterByDepartment(String departmentName) {
        // Try PrimeNG dropdown
        try {
            WaitUtils.selectPrimeNgOption(driver, departmentFilter, departmentName);
            return;
        } catch (Exception ignored) {}
        // Fallback: native select
        try {
            new org.openqa.selenium.support.ui.Select(departmentFilter)
                .selectByVisibleText(departmentName);
        } catch (Exception e) {
            System.err.println("[EmployeeDirectoryPage] Could not filter by department: " + e.getMessage());
        }
    }

    /**
     * Clicks View/Details for an employee row. Re-fetches the row from DOM each time
     * to prevent StaleElementReferenceException caused by Angular re-rendering.
     */
    public EmployeeProfilePage clickViewEmployee(int rowIndex) {
        // Re-fetch rows fresh from DOM
        List<WebElement> rows = driver.findElements(
            By.cssSelector("table tbody tr, .employee-card, [class*='employee-row']"));

        WebElement row = rows.get(rowIndex);
        WaitUtils.scrollAndClick(driver, row);  // click row itself to open profile

        // If clicking the row doesn't navigate, look for a View/Details button
        try {
            WebElement viewBtn = row.findElement(By.xpath(
                ".//button[contains(normalize-space(),'View') or " +
                "contains(normalize-space(),'Details') or " +
                "contains(@class,'view') or contains(@class,'detail')] | " +
                ".//a[contains(@href,'profile') or contains(@href,'employee')]"));
            WaitUtils.safeClick(driver, viewBtn);
        } catch (Exception ignored) {}

        WaitUtils.waitForAngularLoad(driver);
        return new EmployeeProfilePage(driver);
    }

    public void clickEditEmployee(String employeeName) {
        // Re-fetch using XPath to avoid stale reference
        WebElement editBtn = WaitUtils.fluentWait(driver, d -> {
            try {
                return d.findElement(By.xpath(
                    "//tr[.//td[normalize-space()='" + employeeName + "']]" +
                    "//button[contains(normalize-space(),'Edit') or contains(@class,'edit')] | " +
                    "//*[contains(@class,'employee-card') and .//*[normalize-space()='" + employeeName + "']]" +
                    "//*[contains(normalize-space(),'Edit')]"));
            } catch (Exception e) {
                return null;
            }
        }, 15, 500);
        WaitUtils.safeClick(driver, editBtn);
    }

    public EmployeeOnboardPage clickAddEmployee() {
        WaitUtils.dismissPopupsIfPresent(driver);
        WaitUtils.waitForClickability(driver, addEmployeeButton);
        WaitUtils.safeClick(driver, addEmployeeButton);
        WaitUtils.waitForAngularLoad(driver);
        return new EmployeeOnboardPage(driver);
    }

    public void clickNextPage() {
        WaitUtils.waitForClickability(driver, nextPageButton);
        WaitUtils.safeClick(driver, nextPageButton);
        WaitUtils.waitForAngularLoad(driver);
    }

    // ── Verifications ─────────────────────────────────────────────────────────

    public boolean isPageLoaded() {
        try {
            WaitUtils.waitForVisibility(driver, pageTitle);
            return pageTitle.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public int getEmployeeCount() {
        // Re-fetch to get current count after any search/filter
        return driver.findElements(
            By.cssSelector("table tbody tr, .employee-card, [class*='employee-row']")).size();
    }

    public List<String> getAllEmployeeNames() {
        return driver.findElements(
            By.cssSelector("table tbody tr td:first-child, .employee-name"))
            .stream()
            .map(el -> {
                try { return el.getText().trim(); } catch (Exception e) { return ""; }
            })
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }

    public boolean isEmployeePresent(String name) {
        return getAllEmployeeNames().stream()
                .anyMatch(n -> n.equalsIgnoreCase(name));
    }

    public String getTotalCountText() {
        try {
            return totalCountLabel.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public boolean isPaginationDisplayed() {
        try {
            return paginationBar.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
}
