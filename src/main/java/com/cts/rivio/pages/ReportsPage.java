package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

/**
 * ReportsPage – Page Object for the Reports & Analytics module.
 *
 * Test Scenarios : Rivio_TS_16_ReportsAndAnalytics
 * Test Cases     : Rivio_TC032 – Attendance Summary Report (PDF + Excel export)
 *                  Rivio_TC033 – Performance Rating Summary report (Excel export)
 *
 * Available reports:
 *   1. Attendance Summary
 *   2. Leave Utilisation
 *   3. Monthly Payroll
 *   4. Employee Master
 *   5. Performance Rating Summary
 */
public class ReportsPage {

    private WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(css = ".page-title, h1, h2")
    private WebElement pageTitle;

    // Report type navigation cards or tabs
    @FindBy(css = ".report-card, .report-type, [class*='report-item']")
    private List<WebElement> reportTypeCards;

    // Report sidebar links
    @FindBy(css = ".report-nav a, [class*='report-link'], aside a")
    private List<WebElement> reportNavLinks;

    // Date range from/to filters
    @FindBy(css = "input[formcontrolname='startDate'], input[type='date'].from-date")
    private WebElement startDateInput;

    @FindBy(css = "input[formcontrolname='endDate'], input[type='date'].to-date")
    private WebElement endDateInput;

    // Department filter
    @FindBy(css = "select[name='department'], select.dept-filter")
    private WebElement departmentFilter;

    // Generate / Apply report button
    @FindBy(css = "button.generate-report, button[type='submit'].report, button.apply-filter")
    private WebElement generateButton;

    // Report data table
    @FindBy(css = "table.report-table, .report-data table, [class*='report-grid']")
    private WebElement reportTable;

    @FindBy(css = "table.report-table tbody tr, .report-data tbody tr")
    private List<WebElement> reportRows;

    // Export as PDF button
    @FindBy(css = "button.export-pdf, a.export-pdf, button[aria-label*='PDF' i]")
    private WebElement exportPdfButton;

    // Export as Excel button
    @FindBy(css = "button.export-excel, a.export-excel, button[aria-label*='Excel' i]")
    private WebElement exportExcelButton;

    // Report title heading (inside the report section)
    @FindBy(css = ".report-heading, .report-name, [class*='report-title']")
    private WebElement reportHeading;

    // Empty / no data message
    @FindBy(css = ".no-data, .empty-report, [class*='no-report-data']")
    private WebElement noDataMessage;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ReportsPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    /**
     * Clicks the report type card/link matching the given name.
     * e.g. "Attendance Summary", "Performance Rating Summary"
     */
    public void selectReportType(String reportName) {
        // Try nav links first
        for (WebElement link : reportNavLinks) {
            if (link.getText().trim().toLowerCase().contains(reportName.toLowerCase())) {
                WaitUtils.waitForClickability(driver, link);
                link.click();
                return;
            }
        }
        // Fallback: try report cards
        for (WebElement card : reportTypeCards) {
            if (card.getText().trim().toLowerCase().contains(reportName.toLowerCase())) {
                WaitUtils.waitForClickability(driver, card);
                card.click();
                return;
            }
        }
        throw new NoSuchElementException("Report type not found: " + reportName);
    }

    public void setStartDate(String date) {
        WaitUtils.waitForVisibility(driver, startDateInput);
        startDateInput.clear();
        startDateInput.sendKeys(date);
    }

    public void setEndDate(String date) {
        endDateInput.clear();
        endDateInput.sendKeys(date);
    }

    public void filterByDepartment(String dept) {
        WaitUtils.waitForVisibility(driver, departmentFilter);
        new Select(departmentFilter).selectByVisibleText(dept);
    }

    public void clickGenerate() {
        WaitUtils.waitForClickability(driver, generateButton);
        generateButton.click();
    }

    /**
     * Click Export as PDF – triggers browser file download.
     */
    public void clickExportPdf() {
        WaitUtils.waitForClickability(driver, exportPdfButton);
        exportPdfButton.click();
    }

    /**
     * Click Export as Excel – triggers browser file download.
     */
    public void clickExportExcel() {
        WaitUtils.waitForClickability(driver, exportExcelButton);
        exportExcelButton.click();
    }

    // ── Verifications ─────────────────────────────────────────────────────────

    public boolean isPageLoaded() {
        WaitUtils.waitForVisibility(driver, pageTitle);
        return pageTitle.isDisplayed();
    }

    public int getReportRowCount() {
        try {
            WaitUtils.waitForVisibility(driver, reportTable);
            return reportRows.size();
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean isExportPdfButtonVisible() {
        try {
            return exportPdfButton.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isExportExcelButtonVisible() {
        try {
            return exportExcelButton.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getReportHeading() {
        try {
            WaitUtils.waitForVisibility(driver, reportHeading);
            return reportHeading.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public boolean isNoDataMessageVisible() {
        try {
            return noDataMessage.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }
}
