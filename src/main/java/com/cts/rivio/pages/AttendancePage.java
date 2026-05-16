package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

public class AttendancePage {

    private WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(css = ".page-title, h1, h2")
    private WebElement pageTitle;

    @FindBy(css = "button.check-in, button[aria-label='Check In'], [class*='checkin-btn'], " +
                  "button[class*='check-in'], [class*='checkIn-btn']")
    private WebElement checkInButton;

    @FindBy(css = "button.check-out, button[aria-label='Check Out'], [class*='checkout-btn'], " +
                  "button[class*='check-out'], [class*='checkOut-btn']")
    private WebElement checkOutButton;

    @FindBy(css = ".attendance-status, [class*='current-status'], [class*='attendance-state'], " +
                  "[class*='status-label'], [class*='check-status']")
    private WebElement currentStatusLabel;

    // Date range — covers p-calendar inputs and native date inputs
    @FindBy(css = "input[formcontrolname='startDate'], input[formcontrolname='fromDate'], " +
                  "input[placeholder*='Start' i], input[placeholder*='From' i], " +
                  "p-calendar[formcontrolname='startDate'] input, " +
                  "p-calendar[formcontrolname='fromDate'] input, " +
                  "input[type='date']:first-of-type")
    private WebElement startDateInput;

    @FindBy(css = "input[formcontrolname='endDate'], input[formcontrolname='toDate'], " +
                  "input[placeholder*='End' i], input[placeholder*='To' i], " +
                  "p-calendar[formcontrolname='endDate'] input, " +
                  "p-calendar[formcontrolname='toDate'] input")
    private WebElement endDateInput;

    @FindBy(css = "table tbody tr, .attendance-row, [class*='attendance-record'], " +
                  "p-table tbody tr")
    private List<WebElement> attendanceRows;

    @FindBy(css = ".stat-card .count, .attendance-summary .value, " +
                  "[class*='summary-stat'] [class*='value'], [class*='stat-value']")
    private List<WebElement> summaryStatValues;

    // Employee search — broad selectors covering various input patterns
    @FindBy(css = "input[placeholder*='employee' i], input[placeholder*='Employee' i], " +
                  "input[placeholder*='Search' i], input[formcontrolname*='employee' i], " +
                  "input[formcontrolname*='search' i], input[type='search']")
    private WebElement employeeSearchInput;

    @FindBy(css = ".calendar-day, [class*='cal-day'], td.day-cell, " +
                  "[class*='calendar-cell'], p-fullcalendar .fc-daygrid-day")
    private List<WebElement> calendarDays;

    // Apply / Search / Filter button
    @FindBy(xpath = "//button[contains(normalize-space(),'Search') or " +
                    "contains(normalize-space(),'Filter') or " +
                    "contains(normalize-space(),'Apply')]")
    private WebElement searchFilterButton;

    // ── Constructor ───────────────────────────────────────────────────────────

    public AttendancePage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public void checkIn() {
        WaitUtils.waitForClickability(driver, checkInButton);
        WaitUtils.safeClick(driver, checkInButton);
        handleAlertIfPresent(true);
        WaitUtils.waitForAngularLoad(driver);
    }

    public void checkOut() {
        WaitUtils.waitForClickability(driver, checkOutButton);
        WaitUtils.safeClick(driver, checkOutButton);
        handleAlertIfPresent(true);
        WaitUtils.waitForAngularLoad(driver);
    }

    private void handleAlertIfPresent(boolean accept) {
        try {
            Alert alert = WaitUtils.waitForAlert(driver);
            System.out.println("[Alert] " + alert.getText());
            if (accept) alert.accept();
            else alert.dismiss();
        } catch (Exception ignored) {}
    }

    /**
     * Sets a date range filter. Uses JavaScript to set Angular date inputs reliably.
     */
    public void setDateRange(String from, String to) {
        try {
            WaitUtils.waitForVisibility(driver, startDateInput);
            WaitUtils.jsSetValue(driver, startDateInput, from);
            startDateInput.sendKeys(Keys.TAB);
        } catch (Exception e) {
            System.err.println("[AttendancePage] Could not set start date: " + e.getMessage());
        }
        try {
            WaitUtils.waitForVisibility(driver, endDateInput);
            WaitUtils.jsSetValue(driver, endDateInput, to);
            endDateInput.sendKeys(Keys.TAB);
        } catch (Exception e) {
            System.err.println("[AttendancePage] Could not set end date: " + e.getMessage());
        }
        // Click search/filter button if present
        try {
            WaitUtils.waitForClickability(driver, searchFilterButton);
            WaitUtils.safeClick(driver, searchFilterButton);
        } catch (Exception ignored) {}
        WaitUtils.waitForAngularLoad(driver);
    }

    public void searchEmployee(String name) {
        try {
            WaitUtils.waitForVisibility(driver, employeeSearchInput);
            employeeSearchInput.clear();
            employeeSearchInput.sendKeys(name);
            WaitUtils.waitForAngularLoad(driver);
        } catch (Exception e) {
            System.err.println("[AttendancePage] Employee search failed: " + e.getMessage());
        }
    }

    public void clickCalendarDay(String dayNumber) {
        for (WebElement day : calendarDays) {
            try {
                if (day.getText().trim().equals(dayNumber)) {
                    WaitUtils.safeClick(driver, day);
                    return;
                }
            } catch (StaleElementReferenceException ignored) {}
        }
        throw new NoSuchElementException("Calendar day not found: " + dayNumber);
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

    public String getCurrentStatus() {
        try {
            WaitUtils.waitForVisibility(driver, currentStatusLabel);
            return currentStatusLabel.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public boolean isCheckInButtonEnabled() {
        try { return checkInButton.isEnabled(); } catch (Exception e) { return false; }
    }

    public boolean isCheckOutButtonEnabled() {
        try { return checkOutButton.isEnabled(); } catch (Exception e) { return false; }
    }

    public int getAttendanceRecordCount() {
        return driver.findElements(
            By.cssSelector("table tbody tr, .attendance-row")).size();
    }

    public String getSummaryStatValue(int index) {
        return summaryStatValues.get(index).getText().trim();
    }
}
