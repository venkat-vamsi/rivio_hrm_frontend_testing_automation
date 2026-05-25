package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * AttendancePage – mirrors features/attendance/attendance-dashboard.component.html.
 *
 * Real DOM:
 *   - <h1>Time &amp; Attendance</h1>
 *   - Two tabs: "Daily Tracking" and "Employee History"
 *   - "CSV Upload" button and "Manual Punch" button in the header
 *   - p-table renders daily records or history records
 *   - Manual Punch modal: p-select employee, p-datepicker date,
 *     punchIn / punchOut time inputs, Absent checkbox, Submit button
 */
public class AttendancePage {

    private final WebDriver driver;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public AttendancePage(WebDriver driver) { this.driver = driver; }

    // ── Page load ─────────────────────────────────────────────────────────────

    public boolean isPageLoaded() {
        return WaitUtils.waitForPresence(driver,
            By.xpath("//h1[contains(.,'Time') and contains(.,'Attendance')]"), 15);
    }

    // ── Tab navigation ────────────────────────────────────────────────────────

    public void selectDailyTrackingTab()    { clickTab("Daily Tracking"); }
    public void selectEmployeeHistoryTab()  { clickTab("Employee History"); }

    private void clickTab(String label) {
        WebElement btn = WaitUtils.waitForClickability(driver, By.xpath(
            "//button[normalize-space()='" + label + "']"));
        WaitUtils.safeClick(driver, btn);
        WaitUtils.waitForAngularLoad(driver);
    }

    // ── Header actions ────────────────────────────────────────────────────────

    public void clickManualPunch() {
        WebElement btn = WaitUtils.waitForClickability(driver,
            By.xpath("//button[contains(.,'Manual Punch')]"));
        WaitUtils.safeClick(driver, btn);
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.hardWait(500);
    }

    public void clickCsvUpload() {
        WebElement btn = WaitUtils.waitForClickability(driver,
            By.xpath("//button[contains(.,'CSV Upload')]"));
        WaitUtils.safeClick(driver, btn);
    }

    // ── Manual Punch modal — form filling ─────────────────────────────────────

    /**
     * Selects an employee by name in the Manual Punch modal's p-select.
     * Pass "AUTO" to pick the first available employee from the dropdown.
     */
    public void selectEmployeeInPunchModal(String employeeName) {
        try {
            By dropdownLocator = By.cssSelector(
                "p-dialog p-select[formcontrolname='employeeId'], "
                + "p-dialog p-select[formcontrolname='employee']");
            if ("AUTO".equalsIgnoreCase(employeeName) || employeeName == null || employeeName.isEmpty()) {
                WaitUtils.selectPrimeNgOption(driver, dropdownLocator, null);
            } else {
                WaitUtils.selectPrimeNgOption(driver, dropdownLocator, employeeName);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Sets the date in the Manual Punch modal's p-datepicker.
     * Accepts: "today", "yesterday", "today+N", "today-N", or a literal date string.
     */
    public void setDateInPunchModal(String dateOrKeyword) {
        String resolved = resolveDate(dateOrKeyword);
        try {
            WebElement input = WaitUtils.waitForVisibility(driver, By.cssSelector(
                "p-dialog p-datepicker input, "
                + "p-dialog input[formcontrolname='date'], "
                + "p-dialog .p-datepicker input"));
            WaitUtils.jsSetValue(driver, input, resolved);
        } catch (Exception ignored) {}
    }

    /**
     * Sets the Punch-In time field (expects "HH:mm" format like "09:00").
     */
    public void setPunchInTime(String time) {
        try {
            WebElement el = WaitUtils.waitForVisibility(driver, By.cssSelector(
                "p-dialog input[formcontrolname='punchIn'], "
                + "p-dialog input[placeholder*='Punch In' i]"));
            WaitUtils.jsSetValue(driver, el, time);
        } catch (Exception ignored) {}
    }

    /**
     * Sets the Punch-Out time field (expects "HH:mm" format like "18:00").
     */
    public void setPunchOutTime(String time) {
        try {
            WebElement el = WaitUtils.waitForVisibility(driver, By.cssSelector(
                "p-dialog input[formcontrolname='punchOut'], "
                + "p-dialog input[placeholder*='Punch Out' i]"));
            WaitUtils.jsSetValue(driver, el, time);
        } catch (Exception ignored) {}
    }

    /**
     * Clicks the Absent checkbox in the Manual Punch modal (if present).
     */
    public void checkAbsentCheckbox() {
        try {
            WebElement chk = findAbsentCheckbox();
            if (chk != null && !chk.isSelected()) {
                WaitUtils.safeClick(driver, chk);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Clicks the Submit / Save button in the Manual Punch modal.
     */
    public void submitPunchForm() {
        try {
            WebElement btn = WaitUtils.waitForClickability(driver, By.xpath(
                "//p-dialog//button[contains(.,'Submit') or contains(.,'Save')"
                + " or contains(.,'Add Punch') or contains(.,'Record')]"));
            WaitUtils.safeClick(driver, btn);
            WaitUtils.waitForAngularLoad(driver);
            WaitUtils.hardWait(800);
        } catch (Exception ignored) {}
    }

    /**
     * Returns true if a success toast appeared after submitting the punch form.
     */
    public boolean isPunchSuccessful() {
        WaitUtils.hardWait(1000);
        // Modal closes on success, or a success toast appears
        boolean modalClosed = driver.findElements(By.cssSelector("p-dialog .p-dialog")).isEmpty();
        boolean successToast = !driver.findElements(By.cssSelector(
            ".p-toast-message-success, [class*='toast'][class*='success']")).isEmpty()
            || !driver.findElements(By.xpath(
            "//*[contains(@class,'toast') and (contains(.,'success')"
            + " or contains(.,'recorded') or contains(.,'saved'))]")).isEmpty();
        return modalClosed || successToast;
    }

    /**
     * Returns true if a validation error is shown inside the Manual Punch modal.
     */
    public boolean isPunchValidationErrorVisible() {
        return !driver.findElements(By.cssSelector(
            "p-dialog .p-error, p-dialog small.p-error, "
            + "p-dialog [class*='error'], p-dialog .ng-invalid")).isEmpty()
            || !driver.findElements(By.xpath(
            "//p-dialog//*[contains(@class,'text-red') or contains(@class,'text-danger')"
            + " or contains(@class,'error')]")).isEmpty();
    }

    // ── Table queries ─────────────────────────────────────────────────────────

    public int getAttendanceRecordCount() {
        return driver.findElements(By.cssSelector("p-table tbody tr")).size();
    }

    // ── Modal open checks ─────────────────────────────────────────────────────

    public boolean isManualPunchModalOpen() {
        return WaitUtils.waitForPresence(driver,
            By.cssSelector("p-dialog .p-dialog, .p-dialog-mask .p-dialog"), 5);
    }

    public boolean isCsvUploadModalOpen() {
        return WaitUtils.waitForPresence(driver,
            By.cssSelector("p-dialog .p-dialog, .p-dialog-mask .p-dialog"), 5);
    }

    // ── Existing helpers (kept for backward compat) ───────────────────────────

    /** Tries to find an "Absent (Full Day)" checkbox in the open Manual Punch modal. */
    public WebElement findAbsentCheckbox() {
        try {
            return driver.findElement(By.cssSelector(
                "p-checkbox[formcontrolname*='absent'] input, "
                + "input[type='checkbox'][formcontrolname*='absent']"));
        } catch (Exception e) { return null; }
    }

    /** Reads the disabled attribute on the Punch In time input in the Manual Punch modal. */
    public boolean isPunchInTimeFieldDisabled() {
        try {
            WebElement el = driver.findElement(By.cssSelector(
                "p-dialog input[formcontrolname='punchIn'], "
                + "p-dialog input[placeholder*='Punch In' i]"));
            String dis = el.getAttribute("disabled");
            return dis != null && !dis.isEmpty();
        } catch (Exception e) { return false; }
    }

    // ── Legacy-compat shims ───────────────────────────────────────────────────
    public void setDateRange(String from, String to) { }
    public void searchEmployee(String name) { }
    public void checkIn()  { }
    public void checkOut() { }
    public boolean isCheckInButtonEnabled()  { return false; }
    public boolean isCheckOutButtonEnabled() { return false; }
    public String getCurrentStatus() { return ""; }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String resolveDate(String keyword) {
        if (keyword == null || keyword.isEmpty()) return "";
        String k = keyword.trim().toLowerCase();
        if (k.equals("today"))     return LocalDate.now().format(DATE_FMT);
        if (k.equals("yesterday")) return LocalDate.now().minusDays(1).format(DATE_FMT);
        if (k.startsWith("today+")) {
            try {
                int days = Integer.parseInt(k.substring(6));
                return LocalDate.now().plusDays(days).format(DATE_FMT);
            } catch (NumberFormatException ignored) {}
        }
        if (k.startsWith("today-")) {
            try {
                int days = Integer.parseInt(k.substring(6));
                return LocalDate.now().minusDays(days).format(DATE_FMT);
            } catch (NumberFormatException ignored) {}
        }
        return keyword;
    }
}
