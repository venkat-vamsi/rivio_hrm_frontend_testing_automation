package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

/**
 * AuditLogPage – Page Object for the Security & Audit Log section (HR/Admin only).
 *
 * Test Scenarios : Rivio_TS_17_SecurityAndAudit
 * Test Cases     : Rivio_TC034 – Audit log records bank detail change with user, timestamp, action
 *                  Rivio_TC035 – Mobile responsiveness (viewport resize check)
 *
 * The audit log captures:
 *   – User who made the change
 *   – Timestamp of the action
 *   – Action type (e.g., BANK_DETAIL_UPDATE)
 *   – Old value / New value
 *   – Approver (for changes that go through approval)
 */
public class AuditLogPage {

    private WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(css = ".page-title, h1, h2")
    private WebElement pageTitle;

    // Audit log table rows
    @FindBy(css = "table.audit-table tbody tr, .audit-log-row, [class*='audit-entry']")
    private List<WebElement> auditLogRows;

    // Search / filter by event type
    @FindBy(css = "input.audit-search, input[placeholder*='Search' i], input.search-audit")
    private WebElement searchInput;

    // Filter by action / event type dropdown
    @FindBy(css = "select[name='eventType'], select.event-filter")
    private WebElement eventTypeFilter;

    // Date from/to filter
    @FindBy(css = "input[type='date'].audit-from, input[formcontrolname='auditFrom']")
    private WebElement dateFromInput;

    @FindBy(css = "input[type='date'].audit-to, input[formcontrolname='auditTo']")
    private WebElement dateToInput;

    // User column in the log table
    @FindBy(css = "table.audit-table tbody tr td:nth-child(1), .audit-entry .user-col")
    private List<WebElement> userColumns;

    // Action/Event column
    @FindBy(css = "table.audit-table tbody tr td:nth-child(2), .audit-entry .action-col")
    private List<WebElement> actionColumns;

    // Timestamp column
    @FindBy(css = "table.audit-table tbody tr td:nth-child(3), .audit-entry .timestamp-col")
    private List<WebElement> timestampColumns;

    // Old value / New value columns
    @FindBy(css = "table.audit-table tbody tr td.old-value, .audit-entry .old-val")
    private List<WebElement> oldValueColumns;

    @FindBy(css = "table.audit-table tbody tr td.new-value, .audit-entry .new-val")
    private List<WebElement> newValueColumns;

    // ── Constructor ───────────────────────────────────────────────────────────

    public AuditLogPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public void searchByKeyword(String keyword) {
        WaitUtils.waitForVisibility(driver, searchInput);
        searchInput.clear();
        searchInput.sendKeys(keyword);
    }

    public void filterByEventType(String eventType) {
        WaitUtils.waitForVisibility(driver, eventTypeFilter);
        new org.openqa.selenium.support.ui.Select(eventTypeFilter).selectByVisibleText(eventType);
    }

    public void setDateRange(String from, String to) {
        dateFromInput.clear();
        dateFromInput.sendKeys(from);
        dateToInput.clear();
        dateToInput.sendKeys(to);
    }

    // ── Verifications ─────────────────────────────────────────────────────────

    public boolean isPageLoaded() {
        WaitUtils.waitForVisibility(driver, pageTitle);
        return pageTitle.isDisplayed();
    }

    public int getAuditLogRowCount() {
        return auditLogRows.size();
    }

    /**
     * Checks if any audit log entry contains the given action keyword.
     * e.g. "BANK_DETAIL" or "bank" to find bank detail change entries.
     */
    public boolean isEventPresentInLog(String actionKeyword) {
        return actionColumns.stream()
                .anyMatch(el -> el.getText().toLowerCase().contains(actionKeyword.toLowerCase()));
    }

    /**
     * Gets all text from the audit log in one flat string for assertion.
     * Useful for verifying user, timestamp, old/new values all appear.
     */
    public String getAuditLogRowText(int rowIndex) {
        if (rowIndex < auditLogRows.size()) {
            return auditLogRows.get(rowIndex).getText().trim();
        }
        return "";
    }

    public String getActionAtRow(int rowIndex) {
        if (rowIndex < actionColumns.size())
            return actionColumns.get(rowIndex).getText().trim();
        return "";
    }

    public String getUserAtRow(int rowIndex) {
        if (rowIndex < userColumns.size())
            return userColumns.get(rowIndex).getText().trim();
        return "";
    }

    public String getTimestampAtRow(int rowIndex) {
        if (rowIndex < timestampColumns.size())
            return timestampColumns.get(rowIndex).getText().trim();
        return "";
    }
}
