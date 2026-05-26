package com.cts.rivio.pages;

import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * AttendancePage – mirrors
 *   features/attendance/attendance-dashboard/attendance-dashboard.component.html
 *
 * Verified DOM (live Angular code):
 *
 *   Manual Attendance Punch modal (header "Manual Attendance Punch"):
 *     - p-select[formcontrolname='employeeProfileId']  ← searchable [filter]="true"
 *     - p-datepicker[formcontrolname='date']           ← defaults to today
 *     - <input type="checkbox" id="markAbsent">        ← NOT a form control
 *     - p-datepicker[formcontrolname='punchIn']        ← timeOnly hourFormat="12"
 *     - p-datepicker[formcontrolname='punchOut']       ← timeOnly hourFormat="12"
 *     - button "Save Record"                            ← disabled when invalid
 *
 *   Bulk Upload Attendance modal (header "Bulk Upload Attendance"):
 *     - "Download" button (CSV template)
 *     - <input type="file" id="csvFile" accept=".csv"> ← hidden, label trigger
 *     - button "Process CSV"                            ← disabled when no file
 *     - After upload, result panel with .text-emerald-600 success count
 *
 * Validation behavior:
 *   - Form valid ⇒ employeeProfileId + date set
 *   - If NOT marked absent ⇒ submitManualPunch() also requires punchIn / punchOut
 *     (otherwise it fires alert("Please provide at least a Punch In time..."))
 *   - On success the modal closes; there is NO success toast.
 *
 * CSV format (from /attendance/template):
 *   employeeId,date,punchIn,punchOut
 *   1,2026-04-13,2026-04-13T09:00,2026-04-13T17:00
 *
 * employeeId=1 is hardcoded in generated CSVs — that ID always exists in the
 * demo DB (verified from the user-supplied attendance_upload_template).
 */
public class AttendancePage {

    private final WebDriver driver;
    private static final DateTimeFormatter DATE_API_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    // PrimeNG with hourFormat="12" parses "hh:mm a" (e.g. "09:00 AM")
    private static final DateTimeFormatter TIME_12H_FMT = DateTimeFormatter.ofPattern("hh:mm a");

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
        WaitUtils.hardWait(300);
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
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.hardWait(500);
    }

    public boolean isManualPunchModalOpen() {
        return !driver.findElements(By.cssSelector(
            "p-dialog p-select[formcontrolname='employeeProfileId']")).isEmpty();
    }

    public boolean isCsvUploadModalOpen() {
        return !driver.findElements(By.cssSelector("input[type='file'][id='csvFile']")).isEmpty();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MANUAL PUNCH MODAL
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Selects an employee in the punch dropdown.
     * Pass "AUTO" / "" → first available employee (works regardless of demo data).
     */
    public void selectEmployeeInPunchModal(String employee) {
        By dropdown = By.cssSelector(
            "p-dialog p-select[formcontrolname='employeeProfileId']");
        try {
            if (employee == null || employee.isEmpty() || "AUTO".equalsIgnoreCase(employee)) {
                WaitUtils.selectPrimeNgOption(driver, dropdown, null);
            } else {
                WaitUtils.selectPrimeNgOption(driver, dropdown, employee);
            }
            WaitUtils.hardWait(300);
        } catch (Exception e) {
            diag("selectEmployeeInPunchModal failed: " + e.getMessage());
        }
    }

    /**
     * Date defaults to today inside the modal. For most valid tests pass
     * "today" (skipped — keeps the pre-filled default Date). For relative
     * offsets ("today+N" / "yesterday") we type the formatted date — PrimeNG
     * will re-parse it on blur.
     */
    public void setDateInPunchModal(String dateOrKeyword) {
        if (dateOrKeyword == null
                || dateOrKeyword.trim().isEmpty()
                || "today".equalsIgnoreCase(dateOrKeyword.trim())) {
            return;
        }
        String resolved = resolveDateMMDDYYYY(dateOrKeyword);
        try {
            WebElement input = WaitUtils.waitForVisibility(driver, By.cssSelector(
                "p-dialog p-datepicker[formcontrolname='date'] input"));
            WaitUtils.jsSetValue(driver, input, resolved);
            input.sendKeys(Keys.TAB);
            WaitUtils.hardWait(300);
        } catch (Exception e) {
            diag("setDateInPunchModal failed: " + e.getMessage());
        }
    }

    /**
     * Sets Punch-In time. Pass "09:00", "9:00 AM", or "09:00 AM" — we
     * normalise to the "hh:mm a" format PrimeNG expects with hourFormat="12".
     */
    public void setPunchInTime(String time) {
        setTimeOnlyField("punchIn", time);
    }

    public void setPunchOutTime(String time) {
        setTimeOnlyField("punchOut", time);
    }

    private void setTimeOnlyField(String controlName, String time) {
        if (time == null || time.isEmpty()) return;
        String formatted = normalizeTimeTo12h(time);
        try {
            WebElement input = WaitUtils.waitForVisibility(driver, By.cssSelector(
                "p-dialog p-datepicker[formcontrolname='" + controlName + "'] input"));
            // Some PrimeNG builds make the time-picker input readonly. Use JS
            // to set the value AND dispatch input+change+blur so the control
            // re-parses the string into a Date.
            ((JavascriptExecutor) driver).executeScript(
                "var el = arguments[0]; var v = arguments[1];" +
                "el.removeAttribute('readonly');" +
                "el.value = v;" +
                "el.dispatchEvent(new Event('input',  {bubbles:true}));" +
                "el.dispatchEvent(new Event('change', {bubbles:true}));" +
                "el.dispatchEvent(new Event('blur',   {bubbles:true}));",
                input, formatted);
            WaitUtils.hardWait(300);
        } catch (Exception e) {
            diag("setTimeOnlyField(" + controlName + ") failed: " + e.getMessage());
        }
    }

    /** Toggles the "Mark Employee as Absent (Full Day)" checkbox. */
    public void checkAbsentCheckbox() {
        try {
            WebElement chk = driver.findElement(By.cssSelector(
                "p-dialog input[type='checkbox'][id='markAbsent']"));
            if (chk != null && !chk.isSelected()) {
                WaitUtils.safeClick(driver, chk);
                WaitUtils.hardWait(300);
            }
        } catch (Exception ignored) {}
    }

    /** Diagnostic helper kept for backward compat (used by the bug test). */
    public WebElement findAbsentCheckbox() {
        List<WebElement> els = driver.findElements(By.cssSelector(
            "p-dialog input[type='checkbox'][id='markAbsent']"));
        return els.isEmpty() ? null : els.get(0);
    }

    /** Diagnostic helper for the Absent-disables-times bug test. */
    public boolean isPunchInTimeFieldDisabled() {
        try {
            WebElement el = driver.findElement(By.cssSelector(
                "p-dialog p-datepicker[formcontrolname='punchIn'] input"));
            String dis = el.getAttribute("disabled");
            if (dis != null && !dis.isEmpty()) return true;
            // Also true if the time-picker block is hidden by @if (!isAbsent())
            return driver.findElements(By.cssSelector(
                "p-dialog p-datepicker[formcontrolname='punchIn']")).isEmpty();
        } catch (Exception e) { return false; }
    }

    /**
     * Multi-strategy submit (native click → JS click → form.requestSubmit).
     * Returns true when modal closes within 6s, false otherwise.
     */
    public boolean submitPunchForm() {
        return submitDialogForm("Save Record",
            "p-dialog p-select[formcontrolname='employeeProfileId']",
            5000);
    }

    /** Kept for backward compatibility — same as submitPunchForm but boolean-discarding. */
    public boolean isPunchSuccessful() {
        // Modal closed = success (component sets isManualPunchModalOpen.set(false))
        return !isManualPunchModalOpen();
    }

    public boolean isPunchValidationErrorVisible() {
        return !driver.findElements(By.cssSelector(
            "p-dialog .p-error, p-dialog small.p-error, "
            + "p-dialog input.ng-invalid.ng-touched, "
            + "p-dialog p-select.ng-invalid.ng-touched")).isEmpty();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CSV UPLOAD MODAL
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Rewrites src/test/resources/testdata/attendance_upload.csv with rows
     * for employeeIds 1–5 (all guaranteed to exist in the demo DB per the
     * user-supplied template) on TODAY's date, 09:00–17:00.
     *
     * The FILENAME is fixed so the user can open it in Excel/Notepad to
     * inspect what was uploaded. The CONTENT is regenerated on every run
     * so the date is always today — no stale 2026-04-13 data on next run.
     *
     * @return absolute path of the file
     */
    public String generateAttendanceCsvForToday() throws IOException {
        return generateAttendanceCsvBatch(new int[]{1, 2, 3, 4, 5},
            LocalDate.now(), "09:00", "17:00");
    }

    /** Single-row variant retained for backward compatibility. */
    public String generateAttendanceCsv(int employeeId, LocalDate date,
                                        String punchInHHmm, String punchOutHHmm) throws IOException {
        return generateAttendanceCsvBatch(new int[]{employeeId},
            date, punchInHHmm, punchOutHHmm);
    }

    /**
     * Writes a multi-row CSV (one row per employeeId) for the given date to
     * the FIXED path src/test/resources/testdata/attendance_upload.csv —
     * overwriting any previous run's content. Format matches the bundled
     * attendance_upload_template:
     *   employeeId,date,punchIn,punchOut
     *   1,2026-05-26,2026-05-26T09:00,2026-05-26T17:00
     */
    public String generateAttendanceCsvBatch(int[] employeeIds, LocalDate date,
                                             String punchInHHmm, String punchOutHHmm) throws IOException {
        String dateStr = date.format(DATE_API_FMT);
        StringBuilder csv = new StringBuilder("employeeId,date,punchIn,punchOut\n");
        for (int id : employeeIds) {
            csv.append(id).append(',').append(dateStr).append(',')
               .append(dateStr).append('T').append(punchInHHmm).append(',')
               .append(dateStr).append('T').append(punchOutHHmm).append('\n');
        }
        File out = new File(AppConstants.ATTENDANCE_CSV_PATH);
        Files.createDirectories(out.getParentFile().toPath());
        Files.write(out.toPath(), csv.toString().getBytes());
        diag("Wrote CSV (" + employeeIds.length + " employees, date="
            + dateStr + "): " + out.getAbsolutePath());
        return out.getAbsolutePath();
    }

    /** Closes the Bulk Upload modal so the user can see the daily table. */
    public void closeCsvUploadModal() {
        try {
            // Component renders Cancel during upload AND a Close after result.
            // Click whichever button is visible.
            List<WebElement> closes = driver.findElements(By.xpath(
                "//p-dialog//button[normalize-space()='Cancel' "
              + "or normalize-space()='Close' "
              + "or contains(normalize-space(.),'Done')]"));
            if (!closes.isEmpty()) {
                WaitUtils.safeClick(driver, closes.get(0));
                WaitUtils.hardWait(400);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Returns the number of Daily Tracking rows whose visible text contains
     * the given employee identifier (e.g. "EMP", "Aisha", "Alice").
     * Scrolling not needed — PrimeNG renders all rows in DOM; out-of-viewport
     * rows are still findElements-matchable.
     */
    public int countDailyTrackingRowsContaining(String token) {
        if (token == null || token.isEmpty()) return 0;
        try {
            String xp =
                  "//p-table//tbody/tr["
                + "contains(normalize-space(.),'" + token.replace("'", "&apos;") + "')]";
            return driver.findElements(By.xpath(xp)).size();
        } catch (Exception ignored) { return 0; }
    }

    /**
     * Uploads a CSV file via the hidden <input type="file"> in the Bulk
     * Upload Attendance modal. Selenium's sendKeys() works even when the
     * element is visually hidden (display:none).
     */
    public void uploadCsvFile(String absolutePath) {
        try {
            By fileInputLocator = By.cssSelector("input[type='file'][id='csvFile']");
            WaitUtils.waitForPresence(driver, fileInputLocator, 5);
            WebElement fileInput = driver.findElement(fileInputLocator);
            // Strip the hidden class so sendKeys is accepted on all drivers
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].classList.remove('hidden');" +
                "arguments[0].style.display='block';" +
                "arguments[0].style.visibility='visible';", fileInput);
            fileInput.sendKeys(absolutePath);
            WaitUtils.hardWait(500);
            diag("Sent file to <input id=csvFile>: " + absolutePath);
        } catch (Exception e) {
            diag("uploadCsvFile failed: " + e.getMessage());
        }
    }

    /**
     * Clicks "Process CSV" and waits up to 10s for the result panel to
     * appear OR the modal to close. Returns the number of successful
     * records (read from .text-emerald-600), or 0 if not detected.
     */
    public int clickProcessCsvAndGetSuccessCount() {
        By processBtn = By.xpath(
            "//p-dialog//button[normalize-space()='Process CSV' "
          + "or contains(normalize-space(.),'Process CSV')]");

        try {
            WebElement btn = WaitUtils.waitForClickability(driver, processBtn);
            ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
            WaitUtils.hardWait(200);
            WaitUtils.safeClick(driver, btn);
        } catch (Exception e) {
            diag("Process CSV click failed: " + e.getMessage());
            return 0;
        }

        // Poll up to 10s for the green success number to appear
        long deadline = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < deadline) {
            // Component renders <span class="block text-4xl font-black text-emerald-600">N</span>
            List<WebElement> nums = driver.findElements(By.cssSelector(
                "p-dialog .text-emerald-600"));
            for (WebElement el : nums) {
                String txt = el.getText().trim();
                if (txt.matches("\\d+")) {
                    int n = Integer.parseInt(txt);
                    diag("CSV upload success count: " + n);
                    return n;
                }
            }
            WaitUtils.hardWait(400);
        }
        diag("CSV upload result panel did NOT appear within 10s");
        return 0;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Multi-strategy submit (shared with leave / recruitment)
    // ══════════════════════════════════════════════════════════════════════════

    private boolean submitDialogForm(String btnText, String sentinelCss, long buttonEnableTimeoutMs) {
        By btnBy = By.xpath(
            "//p-dialog//button[normalize-space()='" + btnText
          + "' or contains(normalize-space(.),'" + btnText + "')]");

        long deadline = System.currentTimeMillis() + buttonEnableTimeoutMs;
        WebElement btn = null;
        while (System.currentTimeMillis() < deadline) {
            List<WebElement> btns = driver.findElements(btnBy);
            if (!btns.isEmpty()) {
                btn = btns.get(0);
                String disabled = btn.getAttribute("disabled");
                String cls = btn.getAttribute("class");
                boolean cssDisabled = cls != null && cls.contains("p-disabled");
                boolean attrDisabled = disabled != null && !disabled.isEmpty()
                                    && !"false".equalsIgnoreCase(disabled);
                if (!attrDisabled && !cssDisabled) {
                    diag("[" + btnText + "] enabled");
                    break;
                }
            }
            WaitUtils.hardWait(250);
            btn = null;
        }
        if (btn == null) {
            diag("[" + btnText + "] never enabled — form invalid");
            return false;
        }

        try {
            ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
            WaitUtils.hardWait(200);
        } catch (Exception ignored) {}

        try { btn.click(); diag("[" + btnText + "] native click"); }
        catch (Exception e) { diag("[" + btnText + "] native click failed"); }
        acceptAlertIfPresent(1000);  // missing punch alert path
        if (waitForModalGone(sentinelCss, 2500)) return true;

        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
            diag("[" + btnText + "] JS click");
        } catch (Exception ignored) {}
        acceptAlertIfPresent(1000);
        if (waitForModalGone(sentinelCss, 2500)) return true;

        try {
            Object r = ((JavascriptExecutor) driver).executeScript(
                "var form = document.querySelector('p-dialog form');" +
                "if (!form) return 'no form';" +
                "if (typeof form.requestSubmit === 'function') {" +
                "  form.requestSubmit(); return 'requestSubmit';" +
                "}" +
                "form.dispatchEvent(new Event('submit', {bubbles:true,cancelable:true}));" +
                "return 'dispatchEvent';");
            diag("[" + btnText + "] form.requestSubmit result: " + r);
        } catch (Exception ignored) {}
        acceptAlertIfPresent(1500);
        return waitForModalGone(sentinelCss, 3000);
    }

    private boolean waitForModalGone(String sentinelCss, long maxMillis) {
        long deadline = System.currentTimeMillis() + maxMillis;
        while (System.currentTimeMillis() < deadline) {
            if (driver.findElements(By.cssSelector(sentinelCss)).isEmpty()) return true;
            WaitUtils.hardWait(250);
        }
        return driver.findElements(By.cssSelector(sentinelCss)).isEmpty();
    }

    /**
     * The Angular code fires alert("Please provide at least a Punch In time...")
     * when punch times are missing. If the test forgot to supply them this
     * blocks Selenium — accept-on-sight (no 3 s pause; this is the failure
     * path, not the success path).
     */
    private void acceptAlertIfPresent(long maxMillis) {
        long deadline = System.currentTimeMillis() + maxMillis;
        while (System.currentTimeMillis() < deadline) {
            try {
                org.openqa.selenium.Alert a = driver.switchTo().alert();
                String text = a.getText();
                diag("Alert dismissed: \"" + text + "\"");
                a.accept();
                return;
            } catch (org.openqa.selenium.NoAlertPresentException ignored) {
                WaitUtils.hardWait(150);
            } catch (Exception ignored) { return; }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════════

    public int getAttendanceRecordCount() {
        return driver.findElements(By.cssSelector("p-table tbody tr")).size();
    }

    // legacy compat shims (kept so callers don't break)
    public void setDateRange(String from, String to) { }
    public void searchEmployee(String name) { }
    public void checkIn()  { }
    public void checkOut() { }
    public boolean isCheckInButtonEnabled()  { return false; }
    public boolean isCheckOutButtonEnabled() { return false; }
    public String getCurrentStatus() { return ""; }

    /** Converts "today" / "yesterday" / "today+N" / "today-N" / literal → MM/dd/yyyy. */
    private String resolveDateMMDDYYYY(String keyword) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        if (keyword == null || keyword.isEmpty()) return LocalDate.now().format(f);
        String k = keyword.trim().toLowerCase();
        if (k.equals("today"))     return LocalDate.now().format(f);
        if (k.equals("yesterday")) return LocalDate.now().minusDays(1).format(f);
        if (k.startsWith("today+")) {
            try { return LocalDate.now().plusDays(Integer.parseInt(k.substring(6))).format(f); }
            catch (NumberFormatException ignored) {}
        }
        if (k.startsWith("today-")) {
            try { return LocalDate.now().minusDays(Integer.parseInt(k.substring(6))).format(f); }
            catch (NumberFormatException ignored) {}
        }
        return keyword;
    }

    /**
     * Accepts "09:00" (24h), "9:00 AM", "9 AM", "09:00 AM" — outputs the
     * "hh:mm a" form PrimeNG expects with hourFormat="12".
     */
    private String normalizeTimeTo12h(String input) {
        String s = input.trim().toUpperCase();
        try {
            if (s.matches("\\d{1,2}:\\d{2}\\s+(AM|PM)")) {
                // already "hh:mm AM"
                String[] parts = s.split("\\s+");
                String[] hm = parts[0].split(":");
                int h = Integer.parseInt(hm[0]);
                int m = Integer.parseInt(hm[1]);
                return String.format("%02d:%02d %s", h, m, parts[1]);
            }
            if (s.matches("\\d{1,2}\\s+(AM|PM)")) {
                String[] parts = s.split("\\s+");
                return String.format("%02d:00 %s", Integer.parseInt(parts[0]), parts[1]);
            }
            if (s.matches("\\d{1,2}:\\d{2}")) {
                String[] hm = s.split(":");
                int h = Integer.parseInt(hm[0]);
                int m = Integer.parseInt(hm[1]);
                String ampm = (h >= 12) ? "PM" : "AM";
                int h12 = h % 12; if (h12 == 0) h12 = 12;
                return String.format("%02d:%02d %s", h12, m, ampm);
            }
        } catch (Exception ignored) {}
        return s;
    }

    private void diag(String msg) {
        try { ExtentManager.getTest().info("[Attendance] " + msg); } catch (Exception ignored) {}
        System.out.println("[Attendance] " + msg);
    }
}
