package com.cts.rivio.pages.selfservice;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

public class MyLeavesPage {

    private WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(css = ".page-title, h1, h2")
    private WebElement pageTitle;

    @FindBy(css = ".leave-balance-card, [class*='balance'], .leave-type-summary, " +
                  "[class*='leave-card'], .p-card[class*='leave']")
    private List<WebElement> balanceCards;

    // Apply leave button — XPath replaces invalid :contains() CSS
    @FindBy(xpath = "//button[contains(normalize-space(),'Apply') or " +
                    "contains(normalize-space(),'New Leave') or " +
                    "contains(normalize-space(),'Request Leave')]")
    private WebElement applyLeaveButton;

    // Leave type — PrimeNG p-dropdown wrapper (not a native <select>)
    @FindBy(css = "p-dropdown[formcontrolname='leaveType'], p-dropdown[formcontrolname='leave_type'], " +
                  "p-select[formcontrolname='leaveType'], " +
                  "[class*='leave-type'] p-dropdown, [class*='leave-type'] .p-dropdown")
    private WebElement leaveTypeDropdown;

    // Fallback native select for leave type (some builds use it)
    @FindBy(css = "select[formcontrolname='leaveType'], select[name='leaveType']")
    private WebElement leaveTypeSelect;

    @FindBy(css = "input[formcontrolname='startDate'], input[placeholder*='Start' i], " +
                  "input[placeholder*='From' i], p-calendar[formcontrolname='startDate'] input")
    private WebElement startDateInput;

    @FindBy(css = "input[formcontrolname='endDate'], input[placeholder*='End' i], " +
                  "input[placeholder*='To' i], p-calendar[formcontrolname='endDate'] input")
    private WebElement endDateInput;

    @FindBy(css = "textarea[formcontrolname='reason'], textarea[formcontrolname='description'], " +
                  "textarea[placeholder*='Reason' i], textarea[placeholder*='Comment' i]")
    private WebElement reasonTextarea;

    @FindBy(css = "button[type='submit'], .modal button.btn-primary, " +
                  "[class*='submit-btn'], [class*='apply-btn']")
    private WebElement submitLeaveButton;

    @FindBy(css = ".modal button.cancel, .modal .btn-secondary, " +
                  "button[class*='cancel'], [class*='close-btn']")
    private WebElement cancelLeaveButton;

    @FindBy(css = "table tbody tr, .leave-request-item, [class*='leave-row'], " +
                  "[class*='request-row']")
    private List<WebElement> leaveRequestRows;

    @FindBy(xpath = "//button[contains(normalize-space(),'Cancel') and " +
                    "ancestor::*[contains(@class,'leave-request') or contains(@class,'leave-row')]]")
    private List<WebElement> cancelRequestButtons;

    @FindBy(css = ".toast-success, .alert-success, [class*='success'], " +
                  ".p-toast-message-success, [class*='success-toast']")
    private WebElement successToast;

    @FindBy(css = ".toast-error, .alert-danger, [class*='error-toast'], " +
                  ".p-toast-message-error")
    private WebElement errorToast;

    // ── Constructor ───────────────────────────────────────────────────────────

    public MyLeavesPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public void clickApplyLeave() {
        WaitUtils.dismissPopupsIfPresent(driver);
        WaitUtils.waitForClickability(driver, applyLeaveButton);
        WaitUtils.safeClick(driver, applyLeaveButton);
        WaitUtils.waitForAngularLoad(driver);
    }

    /**
     * Selects leave type from PrimeNG dropdown.
     * Falls back to native Select if PrimeNG wrapper is not present.
     */
    public void selectLeaveType(String type) {
        // Try PrimeNG dropdown first
        try {
            WaitUtils.selectPrimeNgOption(driver, leaveTypeDropdown, type);
            return;
        } catch (Exception ignored) {}

        // Fallback: native <select>
        try {
            WaitUtils.waitForVisibility(driver, leaveTypeSelect);
            new org.openqa.selenium.support.ui.Select(leaveTypeSelect).selectByVisibleText(type);
            return;
        } catch (Exception ignored) {}

        // Last resort: find any dropdown on screen containing the option
        WaitUtils.selectPrimeNgOption(driver,
            By.cssSelector(".p-dropdown, p-dropdown, p-select, select"),
            type);
    }

    public void enterStartDate(String date) {
        WaitUtils.waitForVisibility(driver, startDateInput);
        WaitUtils.jsSetValue(driver, startDateInput, date);
        startDateInput.sendKeys(Keys.TAB);
    }

    public void enterEndDate(String date) {
        WaitUtils.waitForVisibility(driver, endDateInput);
        WaitUtils.jsSetValue(driver, endDateInput, date);
        endDateInput.sendKeys(Keys.TAB);
    }

    public void enterReason(String reason) {
        WaitUtils.waitForVisibility(driver, reasonTextarea);
        reasonTextarea.clear();
        reasonTextarea.sendKeys(reason);
    }

    public void submitLeave() {
        WaitUtils.waitForClickability(driver, submitLeaveButton);
        WaitUtils.safeClick(driver, submitLeaveButton);
        WaitUtils.waitForAngularLoad(driver);
    }

    public void applyForLeave(String leaveType, String startDate, String endDate, String reason) {
        clickApplyLeave();
        selectLeaveType(leaveType);
        enterStartDate(startDate);
        enterEndDate(endDate);
        enterReason(reason);
        submitLeave();
    }

    public void cancelLeaveRequest(int rowIndex) {
        // Re-fetch to avoid stale element
        List<WebElement> btns = driver.findElements(By.xpath(
            "//button[contains(normalize-space(),'Cancel') and " +
            "ancestor::*[contains(@class,'leave-request') or contains(@class,'leave-row') " +
            "or ancestor::tr]]"));
        WaitUtils.waitForClickability(driver, btns.get(rowIndex));
        WaitUtils.safeClick(driver, btns.get(rowIndex));
        // Handle confirm dialog if present
        WaitUtils.dismissPopupsIfPresent(driver);
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
        return leaveRequestRows.size();
    }

    public int getBalanceCardCount() {
        return balanceCards.size();
    }

    public String getSuccessToastMessage() {
        WaitUtils.waitForVisibility(driver, successToast);
        return successToast.getText().trim();
    }

    public String getErrorToastMessage() {
        WaitUtils.waitForVisibility(driver, errorToast);
        return errorToast.getText().trim();
    }

    public String getLeaveStatusAtRow(int rowIndex) {
        // Re-fetch row to avoid stale reference
        List<WebElement> rows = driver.findElements(
            By.cssSelector("table tbody tr, .leave-request-item, [class*='leave-row']"));
        WebElement row = rows.get(rowIndex);
        return row.findElement(By.cssSelector(".status, [class*='status'], td:last-child"))
                  .getText().trim();
    }
}
