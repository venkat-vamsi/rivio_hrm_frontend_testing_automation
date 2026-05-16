package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

/**
 * PayrollDashboardPage – Page Object for the Payroll module.
 *
 * Covers:
 *   – Payroll summary (total payroll, components)
 *   – Run/process payroll action
 *   – Filter by month/year/department
 *   – View/Download payslip
 */
public class PayrollDashboardPage {

    private WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(css = ".page-title, h1, h2")
    private WebElement pageTitle;

    // Summary KPI cards
    @FindBy(css = ".payroll-summary .card, .kpi-card, [class*='payroll-stat']")
    private List<WebElement> summaryCards;

    // Month and Year selectors for payroll cycle
    @FindBy(css = "select[name='month'], select.month-select, select[formcontrolname='month']")
    private WebElement monthSelector;

    @FindBy(css = "select[name='year'], select.year-select, select[formcontrolname='year']")
    private WebElement yearSelector;

    // Department filter
    @FindBy(css = "select[name='department'], select.dept-filter")
    private WebElement departmentFilter;

    // "Run Payroll" / "Process Payroll" button
    @FindBy(css = "button.run-payroll, button[class*='process'], "
                + "button:contains('Run Payroll'), button:contains('Process')")
    private WebElement runPayrollButton;

    // Payroll records table rows
    @FindBy(css = "table tbody tr, .payroll-row")
    private List<WebElement> payrollRows;

    // Download payslip buttons
    @FindBy(css = "button.download-payslip, a.download-payslip, [class*='download']")
    private List<WebElement> downloadButtons;

    // Payroll status badge (e.g. Processed, Pending)
    @FindBy(css = ".payroll-status, [class*='payroll-status']")
    private List<WebElement> statusBadges;

    // Total payroll amount shown on dashboard
    @FindBy(css = ".total-payroll, [class*='total-amount']")
    private WebElement totalPayrollAmount;

    // Confirmation modal for running payroll
    @FindBy(css = ".modal, [role='dialog'], .confirm-dialog")
    private WebElement confirmModal;

    @FindBy(css = ".modal .confirm-btn, [role='dialog'] button.btn-primary")
    private WebElement confirmRunPayroll;

    // ── Constructor ───────────────────────────────────────────────────────────

    public PayrollDashboardPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public void selectMonth(String month) {
        WaitUtils.waitForVisibility(driver, monthSelector);
        new Select(monthSelector).selectByVisibleText(month);
    }

    public void selectYear(String year) {
        WaitUtils.waitForVisibility(driver, yearSelector);
        new Select(yearSelector).selectByVisibleText(year);
    }

    public void filterByDepartment(String dept) {
        WaitUtils.waitForVisibility(driver, departmentFilter);
        new Select(departmentFilter).selectByVisibleText(dept);
    }

    public void clickRunPayroll() {
        WaitUtils.waitForClickability(driver, runPayrollButton);
        runPayrollButton.click();
        // Handle confirm modal if it appears
        try {
            WaitUtils.waitForVisibility(driver, confirmModal);
            WaitUtils.waitForClickability(driver, confirmRunPayroll);
            confirmRunPayroll.click();
        } catch (Exception ignored) {}
    }

    public void downloadPayslip(int rowIndex) {
        WaitUtils.waitForClickability(driver, downloadButtons.get(rowIndex));
        downloadButtons.get(rowIndex).click();
    }

    // ── Verifications ─────────────────────────────────────────────────────────

    public boolean isPageLoaded() {
        WaitUtils.waitForVisibility(driver, pageTitle);
        return pageTitle.isDisplayed();
    }

    public String getTotalPayrollAmount() {
        try {
            WaitUtils.waitForVisibility(driver, totalPayrollAmount);
            return totalPayrollAmount.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public int getPayrollRowCount() {
        return payrollRows.size();
    }

    public String getPayrollStatusAtRow(int rowIndex) {
        return statusBadges.get(rowIndex).getText().trim();
    }

    public int getSummaryCardCount() {
        return summaryCards.size();
    }
}
