package com.cts.rivio.pages.selfservice;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

public class MyAttendancePage {

    private WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(css = ".page-title, h1, h2")
    private WebElement pageTitle;

    @FindBy(css = ".attendance-summary .stat, [class*='summary-stat'], .attendance-card, " +
                  "[class*='stat-card'], [class*='attendance-summary'] [class*='count']")
    private List<WebElement> summaryStats;

    @FindBy(css = ".calendar-day, [class*='cal-cell'], td[data-date], " +
                  "[class*='calendar-cell'], p-fullcalendar .fc-daygrid-day, " +
                  "[class*='day-cell'], [class*='fc-day']")
    private List<WebElement> calendarCells;

    // Previous month button — broad locator set
    @FindBy(css = "button.prev-month, [aria-label='Previous month'], " +
                  "button[aria-label='prev'], [class*='prev-month'], " +
                  "[class*='cal-prev'], .fc-prev-button, " +
                  "[class*='calendar-nav'] button:first-child")
    private WebElement prevMonthBtn;

    // Next month button
    @FindBy(css = "button.next-month, [aria-label='Next month'], " +
                  "button[aria-label='next'], [class*='next-month'], " +
                  "[class*='cal-next'], .fc-next-button, " +
                  "[class*='calendar-nav'] button:last-child")
    private WebElement nextMonthBtn;

    // Calendar title: month/year heading — many possible class names
    @FindBy(css = ".calendar-header .month-year, [class*='cal-title'], " +
                  "[class*='calendar-title'], [class*='month-year'], " +
                  ".fc-toolbar-title, [class*='cal-header'] h2, " +
                  "[class*='cal-header'] span, [class*='calendar-header'] span, " +
                  "[class*='calendar-header'] h3")
    private WebElement calendarTitle;

    @FindBy(css = "table tbody tr, .attendance-record-row, [class*='attendance-row']")
    private List<WebElement> attendanceRows;

    @FindBy(css = "button.calendar-view, button.list-view, [class*='view-toggle'], " +
                  "[class*='toggle-view'] button")
    private List<WebElement> viewToggleButtons;

    @FindBy(css = "input[type='month'], input[type='date'].month-picker, " +
                  "p-calendar[view='month'] input")
    private WebElement monthPicker;

    @FindBy(css = "td.checkin-time, [class*='check-in-time'], [class*='checkin']")
    private List<WebElement> checkInTimes;

    @FindBy(css = "td.checkout-time, [class*='check-out-time'], [class*='checkout']")
    private List<WebElement> checkOutTimes;

    // ── Constructor ───────────────────────────────────────────────────────────

    public MyAttendancePage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public void clickPreviousMonth() {
        WaitUtils.waitForClickability(driver, prevMonthBtn);
        WaitUtils.safeClick(driver, prevMonthBtn);
        WaitUtils.waitForAngularLoad(driver);
    }

    public void clickNextMonth() {
        WaitUtils.waitForClickability(driver, nextMonthBtn);
        WaitUtils.safeClick(driver, nextMonthBtn);
        WaitUtils.waitForAngularLoad(driver);
    }

    public void selectMonth(String monthYear) {
        try {
            WaitUtils.waitForVisibility(driver, monthPicker);
            WaitUtils.jsSetValue(driver, monthPicker, monthYear);
        } catch (Exception e) {
            System.err.println("[MyAttendancePage] Could not set month: " + e.getMessage());
        }
    }

    public void toggleToListView() {
        for (WebElement btn : viewToggleButtons) {
            try {
                String text = btn.getText();
                String label = btn.getAttribute("aria-label");
                if ("list".equalsIgnoreCase(text) || "list view".equalsIgnoreCase(label)) {
                    WaitUtils.safeClick(driver, btn);
                    return;
                }
            } catch (Exception ignored) {}
        }
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

    public int getSummaryStatCount() {
        return summaryStats.size();
    }

    public String getSummaryStatText(int index) {
        return summaryStats.get(index).getText().trim();
    }

    public int getCalendarCellCount() {
        return calendarCells.size();
    }

    public String getCalendarTitle() {
        try {
            WaitUtils.waitForVisibility(driver, calendarTitle);
            return calendarTitle.getText().trim();
        } catch (Exception e) {
            // Fallback: any heading in the calendar container
            try {
                WebElement el = driver.findElement(By.xpath(
                    "//*[contains(@class,'calendar') or contains(@class,'fc-toolbar')]" +
                    "//*[self::h1 or self::h2 or self::h3 or self::h4 or self::span]" +
                    "[string-length(normalize-space())>0][1]"));
                return el.getText().trim();
            } catch (Exception ex) {
                return "";
            }
        }
    }

    public int getAttendanceRecordCount() {
        return attendanceRows.size();
    }

    public String getCheckInTimeAtRow(int rowIndex) {
        return checkInTimes.get(rowIndex).getText().trim();
    }

    public String getCheckOutTimeAtRow(int rowIndex) {
        return checkOutTimes.get(rowIndex).getText().trim();
    }
}
