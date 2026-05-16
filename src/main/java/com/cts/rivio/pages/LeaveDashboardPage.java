package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

public class LeaveDashboardPage {

    private WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(css = ".page-title, h1, h2")
    private WebElement pageTitle;

    @FindBy(css = ".leave-balance-card, [class*='balance-card'], .leave-type-card, " +
                  "[class*='leave-balance'], .p-card[class*='leave']")
    private List<WebElement> balanceCards;

    @FindBy(css = "table tbody tr, .leave-request-row, [class*='leave-row'], " +
                  "[class*='request-row'], p-table tbody tr")
    private List<WebElement> leaveRequestRows;

    // Status filter — PrimeNG p-dropdown wrapper (NOT native <select>)
    @FindBy(css = "p-dropdown[formcontrolname='status'], p-dropdown[formcontrolname='leaveStatus'], " +
                  "p-select[formcontrolname='status'], " +
                  "[class*='status-filter'] .p-dropdown, [class*='status-filter'] p-dropdown, " +
                  "select[name='status'], select.status-filter")
    private WebElement statusFilter;

    // Date from / to filter
    @FindBy(css = "input[formcontrolname='fromDate'], input[formcontrolname='startDate'], " +
                  "input[placeholder*='From' i], input[placeholder*='Start' i], " +
                  "p-calendar[formcontrolname='fromDate'] input")
    private WebElement fromDateFilter;

    @FindBy(css = "input[formcontrolname='toDate'], input[formcontrolname='endDate'], " +
                  "input[placeholder*='To' i], input[placeholder*='End' i], " +
                  "p-calendar[formcontrolname='toDate'] input")
    private WebElement toDateFilter;

    // Filter/Apply button — XPath replaces invalid :contains() CSS
    @FindBy(xpath = "//button[contains(normalize-space(),'Filter') or " +
                    "contains(normalize-space(),'Apply') or " +
                    "contains(normalize-space(),'Search')]")
    private WebElement applyFilterButton;

    @FindBy(xpath = "//button[contains(normalize-space(),'Approve') or @aria-label='Approve']")
    private List<WebElement> approveButtons;

    @FindBy(xpath = "//button[contains(normalize-space(),'Reject') or @aria-label='Reject']")
    private List<WebElement> rejectButtons;

    @FindBy(css = ".confirm-dialog, .modal, [role='dialog'], .p-dialog, .p-confirm-dialog")
    private WebElement confirmDialog;

    @FindBy(css = ".confirm-dialog .confirm-btn, .modal .btn-primary, " +
                  "[role='dialog'] button.confirm, .p-confirm-dialog-accept, " +
                  ".p-dialog .p-button-primary")
    private WebElement confirmButton;

    @FindBy(css = ".confirm-dialog .cancel-btn, .modal .btn-secondary, " +
                  ".p-confirm-dialog-reject")
    private WebElement cancelConfirmButton;

    @FindBy(css = ".toast, .alert, [class*='toast'], .p-toast-message")
    private WebElement toastMessage;

    // ── Constructor ───────────────────────────────────────────────────────────

    public LeaveDashboardPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    /**
     * Filters leave requests by status. Handles both PrimeNG dropdown and native select.
     */
    public void filterByStatus(String status) {
        // Try PrimeNG dropdown first
        try {
            WaitUtils.selectPrimeNgOption(driver, statusFilter, status);
            WaitUtils.waitForAngularLoad(driver);
            return;
        } catch (Exception ignored) {}
        // Fallback: native select
        try {
            new org.openqa.selenium.support.ui.Select(statusFilter).selectByVisibleText(status);
        } catch (Exception e) {
            System.err.println("[LeaveDashboardPage] Could not filter by status: " + e.getMessage());
        }
    }

    public void filterByDateRange(String from, String to) {
        try {
            WaitUtils.waitForVisibility(driver, fromDateFilter);
            WaitUtils.jsSetValue(driver, fromDateFilter, from);
            fromDateFilter.sendKeys(Keys.TAB);
        } catch (Exception ignored) {}
        try {
            WaitUtils.waitForVisibility(driver, toDateFilter);
            WaitUtils.jsSetValue(driver, toDateFilter, to);
            toDateFilter.sendKeys(Keys.TAB);
        } catch (Exception ignored) {}
        try {
            WaitUtils.safeClick(driver, applyFilterButton);
        } catch (Exception ignored) {}
        WaitUtils.waitForAngularLoad(driver);
    }

    public void approveLeaveRequest(int index) {
        // Re-fetch buttons to avoid stale elements
        List<WebElement> btns = driver.findElements(
            By.xpath("//button[contains(normalize-space(),'Approve') or @aria-label='Approve']"));
        WaitUtils.waitForClickability(driver, btns.get(index));
        WaitUtils.safeClick(driver, btns.get(index));
        handleConfirmDialog(true);
        WaitUtils.waitForAngularLoad(driver);
    }

    public void rejectLeaveRequest(int index) {
        List<WebElement> btns = driver.findElements(
            By.xpath("//button[contains(normalize-space(),'Reject') or @aria-label='Reject']"));
        WaitUtils.waitForClickability(driver, btns.get(index));
        WaitUtils.safeClick(driver, btns.get(index));
        handleConfirmDialog(true);
        WaitUtils.waitForAngularLoad(driver);
    }

    private void handleConfirmDialog(boolean confirm) {
        try {
            WaitUtils.waitForVisibility(driver, confirmDialog);
            if (confirm) {
                WaitUtils.waitForClickability(driver, confirmButton);
                WaitUtils.safeClick(driver, confirmButton);
            } else {
                WaitUtils.safeClick(driver, cancelConfirmButton);
            }
        } catch (Exception ignored) {}
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

    public int getLeaveRequestCount() {
        return driver.findElements(
            By.cssSelector("table tbody tr, .leave-request-row, [class*='leave-row']")).size();
    }

    public int getBalanceCardCount() {
        return balanceCards.size();
    }

    public String getToastMessage() {
        WaitUtils.waitForVisibility(driver, toastMessage);
        return toastMessage.getText().trim();
    }

    public String getLeaveStatusAtRow(int rowIndex) {
        List<WebElement> rows = driver.findElements(
            By.cssSelector("table tbody tr, .leave-request-row, [class*='leave-row']"));
        WebElement row = rows.get(rowIndex);
        return row.findElement(
            By.cssSelector(".status-badge, td:last-child, [class*='status']"))
            .getText().trim();
    }
}
