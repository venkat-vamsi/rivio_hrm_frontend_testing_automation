package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * AttendancePage – mirrors features/attendance/attendance-dashboard/attendance-dashboard.component.html.
 *
 * Real DOM:
 *   - <h1>Time & Attendance</h1>
 *   - Two tabs: "Daily Tracking" and "Employee History"
 *   - "CSV Upload" button and "Manual Punch" button in the header
 *   - p-table renders daily records or history records
 */
public class AttendancePage {

    private final WebDriver driver;

    public AttendancePage(WebDriver driver) { this.driver = driver; }

    public boolean isPageLoaded() {
        return WaitUtils.waitForPresence(driver,
            By.xpath("//h1[contains(.,'Time') and contains(.,'Attendance')]"), 15);
    }

    public void selectDailyTrackingTab() {
        clickTab("Daily Tracking");
    }

    public void selectEmployeeHistoryTab() {
        clickTab("Employee History");
    }

    private void clickTab(String label) {
        WebElement btn = WaitUtils.waitForClickability(driver, By.xpath(
            "//button[normalize-space()='" + label + "']"));
        WaitUtils.safeClick(driver, btn);
        WaitUtils.waitForAngularLoad(driver);
    }

    public void clickManualPunch() {
        WebElement btn = WaitUtils.waitForClickability(driver, By.xpath(
            "//button[contains(.,'Manual Punch')]"));
        WaitUtils.safeClick(driver, btn);
    }

    public void clickCsvUpload() {
        WebElement btn = WaitUtils.waitForClickability(driver, By.xpath(
            "//button[contains(.,'CSV Upload')]"));
        WaitUtils.safeClick(driver, btn);
    }

    public int getAttendanceRecordCount() {
        return driver.findElements(By.cssSelector("p-table tbody tr")).size();
    }

    public boolean isManualPunchModalOpen() {
        return WaitUtils.waitForPresence(driver,
            By.cssSelector("p-dialog .p-dialog, .p-dialog-mask .p-dialog"), 5);
    }

    public boolean isCsvUploadModalOpen() {
        return WaitUtils.waitForPresence(driver,
            By.cssSelector("p-dialog .p-dialog, .p-dialog-mask .p-dialog"), 5);
    }

    /** Tries to find an "Absent (Full Day)" checkbox in the open Manual Punch modal. */
    public WebElement findAbsentCheckbox() {
        try {
            return driver.findElement(By.cssSelector(
                "p-checkbox[formcontrolname*='absent'] input, "
                + "input[type='checkbox'][formcontrolname*='absent'], "
                + "label:has(span:contains('Absent')) input"));
        } catch (Exception e) { return null; }
    }

    /** Reads the disabled attribute on the Punch In time input in the Manual Punch modal. */
    public boolean isPunchInTimeFieldDisabled() {
        try {
            WebElement el = driver.findElement(By.cssSelector(
                "input[formcontrolname='punchIn'], input[placeholder*='Punch In']"));
            String dis = el.getAttribute("disabled");
            return dis != null && !dis.isEmpty();
        } catch (Exception e) { return false; }
    }

    // ── Legacy-compat shims so old tests still compile ───────────────────────
    public void setDateRange(String from, String to) { /* not implemented on this page */ }
    public void searchEmployee(String name) { /* covered by history tab dropdown */ }
    public void checkIn() { /* employee self-service has the personal punch button; n/a here */ }
    public void checkOut() { /* same */ }
    public boolean isCheckInButtonEnabled() { return false; }
    public boolean isCheckOutButtonEnabled() { return false; }
    public String getCurrentStatus() { return ""; }
}
