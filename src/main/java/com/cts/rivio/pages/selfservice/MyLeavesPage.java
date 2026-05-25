package com.cts.rivio.pages.selfservice;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MyLeavesPage – mirrors features/self-service/employee-leaves/employee-leaves.component.html.
 *
 * ════════════════════════════════════════════════════════════════════════════
 * REAL DOM (PrimeNG 17 / Angular 17)
 * ════════════════════════════════════════════════════════════════════════════
 *  Page:
 *    &lt;h1&gt;My Leaves&lt;/h1&gt;
 *    "Apply for Leave" button
 *    Balance cards  → .glass-panel.p-6.relative.overflow-hidden
 *    History table  → p-table tbody tr
 *
 *  Apply Leave modal (p-dialog):
 *    p-select[formcontrolname='leaveTypeId']       – leave type
 *    p-datepicker[formcontrolname='dateRange']     – range picker (selectionMode="range")
 *    textarea[formcontrolname='reason']            – optional reason
 *    button "Submit" / "Apply" / "Send Request"   – submit
 *
 * ════════════════════════════════════════════════════════════════════════════
 * KEY DESIGN DECISIONS
 * ════════════════════════════════════════════════════════════════════════════
 * • Date selection uses ACTUAL CALENDAR CLICKS, not jsSetValue().
 *   PrimeNG's p-datepicker intercepts the input's value via its own model;
 *   setting input.value via JS does not update the Angular FormControl.
 *   The only reliable approach is to click on the calendar day cells.
 *
 * • The datepicker has appendTo="body" — the calendar OVERLAY PANEL renders
 *   at the document <body> level, NOT inside p-dialog. Therefore:
 *     – openCalendarPicker() scopes to "p-dialog p-datepicker" for the INPUT
 *       (which IS inside the modal), but uses the class p-datepicker-DROPDOWN
 *       (not p-datepicker-trigger) for the calendar icon button.
 *     – isCalendarPanelOpen(), clickDayCell(), clickCalendarNextMonth(), etc.
 *       ALL search globally (no "p-dialog" scope) because the overlay is at body.
 *
 * • The apply-leave form has exactly two required fields: Leave Type and Date
 *   Range. There is NO reason / notes textarea in the current UI. fillReason()
 *   silently no-ops when the textarea is absent, so test data reason values
 *   are left empty to match the live DOM.
 *
 * • Month names are read from the calendar header and compared numerically
 *   to navigate to the correct month before clicking a day cell.
 *
 * • Working-day offsets (from ExcelUtils) skip Saturday and Sunday
 *   automatically, so tests always land on Mon–Fri dates.
 */
public class MyLeavesPage {

    private final WebDriver driver;

    // PrimeNG month names (index 0 = January, index 11 = December)
    private static final String[] MONTH_NAMES = {
        "January","February","March","April","May","June",
        "July","August","September","October","November","December"
    };

    public MyLeavesPage(WebDriver driver) { this.driver = driver; }

    // ════════════════════════════════════════════════════════════════════════
    // Page-level assertions
    // ════════════════════════════════════════════════════════════════════════

    public boolean isPageLoaded() {
        return WaitUtils.waitForH1Text(driver, "My Leaves", 15);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Open modal
    // ════════════════════════════════════════════════════════════════════════

    public void clickApplyForLeave() {
        WebElement btn = WaitUtils.waitForClickability(driver,
            By.xpath("//button[contains(.,'Apply for Leave')]"));
        WaitUtils.safeClick(driver, btn);
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.hardWait(600);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Form filling — inside the Apply Leave modal
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Selects the leave type from the p-select inside the modal.
     *
     * Supported values: "Casual Leave", "Sick Leave", "Earned Leave", "Maternity Leave", etc.
     * Pass empty string or "AUTO" to select the first available option from the dropdown.
     *
     * NOTE: This does NOT fall back to jsSetValue — PrimeNG p-select requires a click
     * on the panel list item to commit the selection to the FormControl.
     */
    public void selectLeaveType(String leaveType) {
        if (leaveType == null || leaveType.isEmpty() || "AUTO".equalsIgnoreCase(leaveType)) {
            // Select first available option
            selectFirstPrimeNgOption(By.cssSelector(
                "p-dialog p-select[formcontrolname='leaveTypeId'], "
                + "p-dialog p-select[formcontrolname='leaveType']"));
        } else {
            try {
                WaitUtils.selectPrimeNgOption(driver, By.cssSelector(
                    "p-dialog p-select[formcontrolname='leaveTypeId'], "
                    + "p-dialog p-select[formcontrolname='leaveType']"), leaveType);
            } catch (Exception e) {
                // If exact match fails, open panel and pick first item containing the text
                selectPrimeNgOptionContaining(By.cssSelector(
                    "p-dialog p-select[formcontrolname='leaveTypeId'], "
                    + "p-dialog p-select[formcontrolname='leaveType']"), leaveType);
            }
        }
        WaitUtils.hardWait(300);
    }

    /**
     * Sets the date range in the Apply Leave modal's PrimeNG range datepicker.
     *
     * APPROACH: clicks the calendar input to open the overlay panel, then navigates
     * month-by-month via the header arrows and clicks the correct day cells.
     * The first click sets the start date; the second click (while the calendar
     * remains open in range mode) sets the end date.
     *
     * After both clicks the method checks the "WORKING DAYS REQUESTED" counter.
     * If it still shows 0 the whole sequence is retried once automatically.
     *
     * @param startDaysFromToday working-day offset for start (1 = next weekday, -1 = yesterday)
     * @param endDaysFromToday   working-day offset for end   (must be &ge; start)
     */
    public void setLeaveDateRange(int startDaysFromToday, int endDaysFromToday) {
        LocalDate startDate = resolveWorkingDay(LocalDate.now(), startDaysFromToday);
        LocalDate endDate   = resolveWorkingDay(LocalDate.now(), endDaysFromToday);

        for (int attempt = 0; attempt < 2; attempt++) {          // retry once if days stay 0

            // ── open the calendar ──────────────────────────────────────────
            openCalendarPicker();
            if (!isCalendarPanelOpen()) return;                  // calendar could not be opened

            // ── click START date ───────────────────────────────────────────
            navigateToMonth(startDate.getYear(), startDate.getMonthValue());
            clickDayCell(startDate.getDayOfMonth());
            WaitUtils.hardWait(500);

            // ── click END date (range mode keeps calendar open) ────────────
            if (!isCalendarPanelOpen()) {
                openCalendarPicker();
                WaitUtils.hardWait(400);
            }
            navigateToMonth(endDate.getYear(), endDate.getMonthValue());
            clickDayCell(endDate.getDayOfMonth());
            WaitUtils.hardWait(500);

            // ── close panel ────────────────────────────────────────────────
            closeCalendarIfOpen();
            WaitUtils.hardWait(400);

            // ── verify dates registered in the Angular form ────────────────
            int workingDays = getWorkingDaysRequested();
            if (workingDays > 0) return;                         // success — dates accepted

            // Days still 0: clear the input and retry
            tryClearDateInput();
            WaitUtils.hardWait(400);
        }
    }

    /**
     * Returns the number shown in the "WORKING DAYS REQUESTED: N DAY(S)" counter
     * inside the Apply Leave modal.  Returns 0 if the counter cannot be read or
     * shows zero — meaning PrimeNG has not accepted the date selection yet.
     */
    public int getWorkingDaysRequested() {
        try {
            // The text is typically "WORKING DAYS REQUESTED: 2 DAY(S)"
            List<WebElement> els = driver.findElements(By.xpath(
                "//p-dialog//*[contains("
                + "translate(.,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),"
                + "'WORKING DAYS')]"));
            for (WebElement el : els) {
                String text = el.getText();
                java.util.regex.Matcher m =
                    java.util.regex.Pattern.compile(":\\s*(\\d+)").matcher(text);
                if (m.find()) return Integer.parseInt(m.group(1));
            }
        } catch (Exception ignored) {}
        return 0;
    }

    /** Clears the date-picker input so a retry starts from a clean state. */
    private void tryClearDateInput() {
        try {
            WebElement input = driver.findElement(By.cssSelector(
                "p-dialog p-datepicker input, "
                + "p-dialog [formcontrolname='dateRange'] input, "
                + "p-dialog .p-datepicker-input"));
            input.clear();
        } catch (Exception ignored) {}
    }

    /**
     * Types a reason into the optional textarea inside the Apply Leave modal.
     * Silently ignored if the textarea is not present.
     */
    public void fillReason(String reason) {
        if (reason == null || reason.isEmpty()) return;
        try {
            WebElement el = WaitUtils.waitForVisibility(driver, By.cssSelector(
                "p-dialog textarea[formcontrolname='reason'], "
                + "p-dialog textarea[formcontrolname='notes'], "
                + "p-dialog textarea"));
            el.clear();
            el.sendKeys(reason);
        } catch (Exception ignored) {}
    }

    /**
     * Clicks the Submit / Apply / Send Request button in the Apply Leave modal.
     *
     * Scrolls the button into view before clicking so it is never obscured by
     * a sticky header or the calendar overlay.  Waits for Angular to finish
     * processing the form submission before returning.
     */
    public void submitLeaveApplication() {
        By btnBy = By.xpath(
            "//p-dialog//button["
            + "contains(normalize-space(.),'Submit') "
            + "or contains(normalize-space(.),'Apply') "
            + "or contains(normalize-space(.),'Send Request') "
            + "or contains(normalize-space(.),'Request Leave') "
            + "or contains(normalize-space(.),'Save')]");
        try {
            WebElement btn = WaitUtils.waitForClickability(driver, btnBy);
            // Scroll the button into the visible viewport before clicking
            ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
            WaitUtils.hardWait(300);
            WaitUtils.safeClick(driver, btn);
            WaitUtils.waitForAngularLoad(driver);
            WaitUtils.hardWait(1500);          // extra buffer for the POST + toast animation
        } catch (Exception e) {
            // Button may be disabled (form invalid) — try a JS click as last resort
            try {
                WebElement btn = driver.findElement(btnBy);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                WaitUtils.waitForAngularLoad(driver);
                WaitUtils.hardWait(1500);
            } catch (Exception ignored) {}
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Result verification
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Returns true if the leave was submitted successfully.
     *
     * Two indicators of success:
     *   (a) The Apply Leave modal closed (most common)
     *   (b) A success toast appeared (p-toast with success class)
     */
    public boolean isLeaveSubmittedSuccessfully() {
        WaitUtils.hardWait(1500);
        boolean modalClosed  = !isApplyModalOpen();
        boolean successToast = isSuccessToastVisible();
        return modalClosed || successToast;
    }

    /**
     * Returns the visible text (status) of the FIRST row in the leave history table.
     * Useful to confirm a newly submitted request shows "Pending" status.
     */
    public String getFirstHistoryRowStatus() {
        try {
            WebElement row = WaitUtils.waitForVisibility(driver,
                By.cssSelector("p-table tbody tr:first-child"));
            // Status badge is usually the last cell, or a cell with a badge/chip
            List<WebElement> cells = row.findElements(By.tagName("td"));
            for (int i = cells.size() - 1; i >= 0; i--) {
                String txt = cells.get(i).getText().trim();
                if (!txt.isEmpty()) return txt.toUpperCase(Locale.ROOT);
            }
        } catch (Exception ignored) {}
        return "";
    }

    /**
     * Returns the row count AFTER waiting up to 5 s for the table to refresh.
     * Use this after a successful submit to detect the new row.
     */
    public int getHistoryRowCountAfterRefresh(int previousCount) {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            int current = getHistoryRowCount();
            if (current > previousCount) return current;
            WaitUtils.hardWait(400);
        }
        return getHistoryRowCount();
    }

    /**
     * Returns the Available balance shown in the first balance card matching the
     * given leave type label. Returns -1 if the card cannot be found.
     */
    public int getAvailableBalance(String leaveTypeLabel) {
        try {
            List<WebElement> cards = driver.findElements(By.cssSelector(
                ".glass-panel.p-6.relative.overflow-hidden"));
            for (WebElement card : cards) {
                if (card.getText().toLowerCase(Locale.ROOT)
                        .contains(leaveTypeLabel.toLowerCase(Locale.ROOT))) {
                    // Extract the first number from the card text (= Available days)
                    String text = card.getText().replaceAll("[^0-9]", " ").trim();
                    String[] parts = text.split("\\s+");
                    if (parts.length > 0) return Integer.parseInt(parts[0]);
                }
            }
        } catch (Exception ignored) {}
        return -1;
    }

    /**
     * Returns true if an inline validation error is visible inside the Apply Leave modal.
     * Covers: .p-error, small.p-error, [class*='error'], .ng-invalid, text-red elements.
     */
    public boolean isValidationErrorVisible() {
        return !driver.findElements(By.cssSelector(
            "p-dialog .p-error, "
            + "p-dialog small.p-error, "
            + "p-dialog [class*='error-message'], "
            + "p-dialog .ng-invalid.ng-touched")).isEmpty()
            || !driver.findElements(By.xpath(
            "//p-dialog//*["
            + "contains(@class,'text-red') or contains(@class,'text-danger') "
            + "or contains(@class,'error')]"
            + "[normalize-space(.)!='']")).isEmpty();
    }

    /**
     * Returns true if the Submit button inside the Apply Leave modal is disabled.
     * A disabled button means the form's reactive-form validation blocked submission.
     */
    public boolean isSubmitButtonDisabled() {
        try {
            WebElement btn = driver.findElement(By.xpath(
                "//p-dialog//button["
                + "contains(normalize-space(.),'Submit') "
                + "or contains(normalize-space(.),'Apply') "
                + "or contains(normalize-space(.),'Send Request')]"));
            String dis = btn.getAttribute("disabled");
            boolean hasDisabledClass = btn.getAttribute("class") != null
                && btn.getAttribute("class").contains("p-disabled");
            return (dis != null && !dis.isEmpty()) || hasDisabledClass;
        } catch (Exception e) { return false; }
    }

    /**
     * Returns true if an insufficient-balance / exceeds-balance warning is visible.
     */
    public boolean isInsufficientBalanceWarningVisible() {
        return !driver.findElements(By.xpath(
            "//p-dialog//*[("
            + "contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'insufficient') "
            + "or contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'exceed') "
            + "or contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'balance') "
            + "or contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'not enough')"
            + ") and normalize-space(.)!='']")).isEmpty();
    }

    /**
     * Returns true if a PrimeNG success toast is currently showing on the page.
     */
    public boolean isSuccessToastVisible() {
        return !driver.findElements(By.cssSelector(
            ".p-toast-message-success, "
            + "[class*='toast'][class*='success']")).isEmpty()
            || !driver.findElements(By.xpath(
            "//*[contains(@class,'toast') and ("
            + "contains(.,'success') "
            + "or contains(.,'submitted') "
            + "or contains(.,'applied') "
            + "or contains(.,'approved'))]")).isEmpty();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Table / card queries
    // ════════════════════════════════════════════════════════════════════════

    public int getBalanceCardCount() {
        return driver.findElements(By.cssSelector(
            ".glass-panel.p-6.relative.overflow-hidden")).size();
    }

    public int getHistoryRowCount() {
        return driver.findElements(By.cssSelector("p-table tbody tr")).size();
    }

    public boolean isApplyModalOpen() {
        return !driver.findElements(By.cssSelector("p-dialog .p-dialog")).isEmpty();
    }

    // ════════════════════════════════════════════════════════════════════════
    // PrimeNG dropdown helpers (private)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Opens a p-select dropdown and clicks the FIRST available list item.
     * Used for "AUTO" selection when the exact option text is unknown.
     */
    private void selectFirstPrimeNgOption(By dropdownLocator) {
        try {
            WebElement dropdown = WaitUtils.waitForClickability(driver, dropdownLocator);
            WaitUtils.safeClick(driver, dropdown);
            WaitUtils.hardWait(400);
            // First real option (not a placeholder)
            WebElement first = WaitUtils.waitForClickability(driver, By.cssSelector(
                ".p-select-overlay li:first-child, "
                + ".p-dropdown-panel li:first-child, "
                + ".p-overlay li:first-child"));
            WaitUtils.safeClick(driver, first);
        } catch (Exception ignored) {}
    }

    /**
     * Opens a p-select dropdown and clicks the first item whose visible text
     * CONTAINS the given value (case-insensitive partial match).
     */
    private void selectPrimeNgOptionContaining(By dropdownLocator, String partialText) {
        try {
            WebElement dropdown = WaitUtils.waitForClickability(driver, dropdownLocator);
            WaitUtils.safeClick(driver, dropdown);
            WaitUtils.hardWait(400);
            String lower = partialText.toLowerCase(Locale.ROOT);
            WebElement match = WaitUtils.waitForClickability(driver, By.xpath(
                "//li[contains(@class,'p-select-option') or contains(@class,'p-dropdown-item')]"
                + "[contains(translate(normalize-space(.),"
                + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'"
                + lower + "')]"));
            WaitUtils.safeClick(driver, match);
        } catch (Exception ignored) {}
    }

    // ════════════════════════════════════════════════════════════════════════
    // PrimeNG calendar helpers (private)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Clicks the date-picker INPUT (or its calendar-icon button) to open the
     * overlay panel.
     *
     * DOM facts (confirmed from live Rivio HTML):
     *   • The datepicker has appendTo="body", so the calendar PANEL appears at
     *     document body level — not inside p-dialog.
     *   • The trigger/icon button has class "p-datepicker-dropdown" (NOT
     *     "p-datepicker-trigger" which is an older PrimeNG class name).
     *   • The input is readonly="" — clicks open the panel; typing is blocked.
     *
     * Three fallback attempts, each followed by isCalendarPanelOpen() guard:
     *   1. Click the readonly text input   (most reliable)
     *   2. Click the calendar-icon button  (class p-datepicker-dropdown)
     *   3. JS-click the p-datepicker host  (last resort)
     */
    private void openCalendarPicker() {
        // Attempt 1: click the readonly text input inside the dialog
        try {
            WebElement input = WaitUtils.waitForVisibility(driver, By.cssSelector(
                "p-dialog p-datepicker input, "
                + "p-dialog [formcontrolname='dateRange'] input, "
                + "p-dialog .p-datepicker-input"));
            WaitUtils.safeClick(driver, input);
            WaitUtils.hardWait(700);
        } catch (Exception ignored) {}

        if (isCalendarPanelOpen()) return;

        // Attempt 2: click the calendar-icon (dropdown) button
        // DOM: <button class="p-datepicker-dropdown" aria-label="Choose Date">
        // NOTE: PrimeNG 17+ uses "p-datepicker-dropdown"; older versions used
        //       "p-datepicker-trigger" — we try both to stay version-agnostic.
        try {
            WebElement trigger = driver.findElement(By.cssSelector(
                "p-dialog p-datepicker button.p-datepicker-dropdown, "
                + "p-dialog p-datepicker button.p-datepicker-trigger, "
                + "p-dialog p-datepicker button[aria-label='Choose Date'], "
                + "p-dialog p-datepicker button[aria-haspopup='dialog']"));
            WaitUtils.safeClick(driver, trigger);
            WaitUtils.hardWait(700);
        } catch (Exception ignored) {}

        if (isCalendarPanelOpen()) return;

        // Attempt 3: JS-click the p-datepicker host element
        try {
            WebElement picker = driver.findElement(By.cssSelector("p-dialog p-datepicker"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", picker);
            WaitUtils.hardWait(700);
        } catch (Exception ignored) {}
    }

    /**
     * Returns true if the PrimeNG datepicker calendar table is currently visible.
     *
     * NOTE: In this version of Rivio the calendar renders INSIDE the modal dialog
     * (not as a body-level portal), so we detect the actual calendar TABLE rather
     * than any particular panel wrapper class, which varies across PrimeNG versions.
     */
    private boolean isCalendarPanelOpen() {
        // Primary: look for the actual calendar table regardless of wrapper class
        try {
            List<WebElement> tables = driver.findElements(By.cssSelector(
                "table.p-datepicker-calendar, table[role='grid']"));
            for (WebElement t : tables) {
                if (t.isDisplayed()) return true;
            }
        } catch (Exception ignored) {}

        // Secondary: any datepicker container that is visible
        try {
            List<WebElement> containers = driver.findElements(By.cssSelector(
                ".p-datepicker-calendar-container, "
                + ".p-datepicker-panel, "
                + ".p-datepicker:not(.p-datepicker-inline)"));
            for (WebElement c : containers) {
                if (c.isDisplayed()) return true;
            }
        } catch (Exception ignored) {}

        return false;
    }

    /**
     * Navigates the visible calendar to the requested year + month.
     *
     * ROOT-CAUSE FIX (was: "going back years")
     * ─────────────────────────────────────────
     * The OLD loop re-read month/year on every iteration.  If
     * clickCalendarNextMonth() accidentally clicked the YEAR TITLE button
     * (opening a decade picker), getCalendarDisplayedYear() would read a
     * decade-grid year (e.g. 2020) instead of the real displayed month,
     * causing the loop to reverse direction and bounce for 24 iterations.
     *
     * NEW APPROACH:
     *  1. Read current month/year ONCE before the loop.
     *  2. Calculate the exact number of forward/backward steps needed.
     *  3. Click exactly that many times — no re-reading mid-loop.
     *  4. After the loop, do a single verification step (≤ 3 correction
     *     clicks) in case one click was missed.
     *
     * @param targetYear  4-digit year  (e.g. 2026)
     * @param targetMonth 1-based month (1 = January … 12 = December)
     */
    private void navigateToMonth(int targetYear, int targetMonth) {
        int currentMonth = getCalendarDisplayedMonth();
        int currentYear  = getCalendarDisplayedYear();

        // If reading failed, fall back to today so we at least navigate from a
        // known baseline rather than a garbage value.
        if (currentMonth < 1 || currentMonth > 12 || currentYear < 2020) {
            currentMonth = LocalDate.now().getMonthValue();
            currentYear  = LocalDate.now().getYear();
        }

        if (currentYear == targetYear && currentMonth == targetMonth) return;

        // Calculate exact number of month-steps needed (positive = forward)
        int monthsDiff = (targetYear - currentYear) * 12 + (targetMonth - currentMonth);
        boolean goForward = monthsDiff > 0;
        int steps = Math.min(Math.abs(monthsDiff), 18); // safety cap: never > 18 months

        for (int i = 0; i < steps; i++) {
            if (goForward) clickCalendarNextMonth();
            else           clickCalendarPrevMonth();
            WaitUtils.hardWait(300);
        }

        // ── Single verification pass (handles missed clicks) ──────────
        WaitUtils.hardWait(200);
        int nowMonth = getCalendarDisplayedMonth();
        int nowYear  = getCalendarDisplayedYear();
        if (nowMonth >= 1 && nowYear >= 2020 && (nowYear != targetYear || nowMonth != targetMonth)) {
            int remaining = (targetYear - nowYear) * 12 + (targetMonth - nowMonth);
            int corrSteps = Math.min(Math.abs(remaining), 3);
            boolean corrForward = remaining > 0;
            for (int i = 0; i < corrSteps; i++) {
                if (corrForward) clickCalendarNextMonth();
                else             clickCalendarPrevMonth();
                WaitUtils.hardWait(300);
            }
        }
    }

    /**
     * Reads the month name shown in the PrimeNG calendar header and converts it
     * to a 1-based month number (1 = January, …, 12 = December).
     *
     * Covers PrimeNG class names across versions 14-17:
     *   p-datepicker-select-month  (PrimeNG 17.x latest)
     *   p-datepicker-month-title   (PrimeNG 17.x earlier)
     *   p-datepicker-month         (PrimeNG 14-16)
     */
    private int getCalendarDisplayedMonth() {
        // Ordered from most-specific to most-general
        String[] selectors = {
            ".p-datepicker-select-month",               // PrimeNG 17.x (new)
            ".p-datepicker-month-title",                // PrimeNG 17.x (older builds)
            ".p-datepicker-month.p-datepicker-link",    // PrimeNG 16
            ".p-datepicker-month",                      // PrimeNG 14-16
            ".p-datepicker-title > button:first-of-type", // direct first button in title
            ".p-datepicker-view-port > button:first-of-type"
        };
        for (String sel : selectors) {
            try {
                List<WebElement> els = driver.findElements(By.cssSelector(sel));
                for (WebElement el : els) {
                    if (!el.isDisplayed()) continue;
                    String text = el.getText().trim();
                    int m = parseMonthName(text);
                    if (m >= 1) return m;
                }
            } catch (Exception ignored) {}
        }

        // Last resort: scan all visible text inside the calendar header area
        try {
            List<WebElement> headers = driver.findElements(By.cssSelector(
                ".p-datepicker-header, .p-datepicker-group-header, "
                + ".p-datepicker-calendar-header, .p-datepicker-title"));
            for (WebElement hdr : headers) {
                if (!hdr.isDisplayed()) continue;
                String text = hdr.getText().trim();
                for (int i = 0; i < MONTH_NAMES.length; i++) {
                    if (text.contains(MONTH_NAMES[i])) return i + 1;
                }
            }
        } catch (Exception ignored) {}

        return LocalDate.now().getMonthValue(); // safe fallback
    }

    /**
     * Reads the 4-digit year shown in the PrimeNG calendar header.
     *
     * Covers PrimeNG class names across versions 14-17:
     *   p-datepicker-select-year   (PrimeNG 17.x latest)
     *   p-datepicker-year-title    (PrimeNG 17.x earlier builds)
     *   p-datepicker-year          (PrimeNG 14-16)
     */
    private int getCalendarDisplayedYear() {
        String[] selectors = {
            ".p-datepicker-select-year",                // PrimeNG 17.x (new)
            ".p-datepicker-year-title",                 // PrimeNG 17.x (older builds)
            ".p-datepicker-year.p-datepicker-link",     // PrimeNG 16
            ".p-datepicker-year",                       // PrimeNG 14-16
            ".p-datepicker-title > button:last-of-type",// direct last button in title
            ".p-datepicker-view-port > button:last-of-type"
        };
        for (String sel : selectors) {
            try {
                List<WebElement> els = driver.findElements(By.cssSelector(sel));
                for (WebElement el : els) {
                    if (!el.isDisplayed()) continue;
                    String text = el.getText().replaceAll("[^0-9]", "").trim();
                    if (text.length() == 4) return Integer.parseInt(text);
                }
            } catch (Exception ignored) {}
        }

        // Last resort: extract 4-digit year from any visible header text
        try {
            List<WebElement> headers = driver.findElements(By.cssSelector(
                ".p-datepicker-header, .p-datepicker-group-header, "
                + ".p-datepicker-calendar-header, .p-datepicker-title"));
            Pattern yearPattern = Pattern.compile("\\b(20\\d{2})\\b");
            for (WebElement hdr : headers) {
                if (!hdr.isDisplayed()) continue;
                Matcher m = yearPattern.matcher(hdr.getText());
                if (m.find()) return Integer.parseInt(m.group(1));
            }
        } catch (Exception ignored) {}

        return LocalDate.now().getYear(); // safe fallback
    }

    /** Converts a month name (full or 3-letter) to a 1-based month number. */
    private int parseMonthName(String name) {
        if (name == null || name.isEmpty()) return -1;
        String lower = name.toLowerCase(Locale.ROOT).trim();
        for (int i = 0; i < MONTH_NAMES.length; i++) {
            if (MONTH_NAMES[i].toLowerCase(Locale.ROOT).startsWith(lower)
                    || lower.startsWith(MONTH_NAMES[i].toLowerCase(Locale.ROOT).substring(0, 3))) {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * Clicks the PREVIOUS MONTH navigation button in the calendar header.
     *
     * ROOT-CAUSE NOTE
     * ───────────────
     * The old selector ".p-datepicker-header button:last-of-type" used the
     * DESCENDANT combinator, which matched the YEAR TITLE button (last <button>
     * inside the nested title <div>) instead of the actual prev/next arrow.
     * This opened the decade picker, causing the "going back years" loop.
     *
     * Fix: use ">" (DIRECT CHILD) selectors so title buttons — which live
     * inside a nested <div class="p-datepicker-title"> — are never matched.
     * A JavaScript fallback also uses .children (direct children only).
     */
    private void clickCalendarPrevMonth() {
        // Try named classes first (most reliable across PrimeNG versions)
        String[] cssTries = {
            "button.p-datepicker-prev-button",          // PrimeNG 17.x
            "button.p-datepicker-prev",                 // PrimeNG 15-16
            "button[aria-label='Previous Month']",      // ARIA label
            "button[aria-label*='revious']",            // partial ARIA
            // DIRECT child of any header variant — the ">" is the critical fix
            ".p-datepicker-header > button:first-of-type",
            ".p-datepicker-group-header > button:first-of-type",
            ".p-datepicker-calendar-header > button:first-of-type"
        };
        for (String css : cssTries) {
            try {
                List<WebElement> btns = driver.findElements(By.cssSelector(css));
                for (WebElement btn : btns) {
                    if (btn.isDisplayed()) {
                        WaitUtils.safeClick(driver, btn);
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }
        // JS fallback — uses .children (direct children only, never title buttons)
        jsClickNavButton(false);
    }

    /**
     * Clicks the NEXT MONTH navigation button in the calendar header.
     * Same fix as clickCalendarPrevMonth() — uses ">" direct-child selectors.
     */
    private void clickCalendarNextMonth() {
        String[] cssTries = {
            "button.p-datepicker-next-button",          // PrimeNG 17.x
            "button.p-datepicker-next",                 // PrimeNG 15-16
            "button[aria-label='Next Month']",          // ARIA label
            "button[aria-label*='ext Month']",          // partial ARIA
            // DIRECT child — the ">" excludes title buttons inside nested divs
            ".p-datepicker-header > button:last-of-type",
            ".p-datepicker-group-header > button:last-of-type",
            ".p-datepicker-calendar-header > button:last-of-type"
        };
        for (String css : cssTries) {
            try {
                List<WebElement> btns = driver.findElements(By.cssSelector(css));
                for (WebElement btn : btns) {
                    if (btn.isDisplayed()) {
                        WaitUtils.safeClick(driver, btn);
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }
        jsClickNavButton(true);
    }

    /**
     * JavaScript fallback for prev/next month navigation.
     *
     * Uses element.children (DIRECT children of the header) so that buttons
     * nested inside the title <div> are never reached.  The first BUTTON
     * direct-child = prev; the last BUTTON direct-child = next.
     *
     * @param forward true = click next-month button, false = click prev-month button
     */
    private void jsClickNavButton(boolean forward) {
        String script =
            "var headerClasses = ['.p-datepicker-header','.p-datepicker-group-header'," +
            "  '.p-datepicker-calendar-header','[class*=\"datepicker-header\"]'];" +
            "for (var i = 0; i < headerClasses.length; i++) {" +
            "  var headers = document.querySelectorAll(headerClasses[i]);" +
            "  for (var j = 0; j < headers.length; j++) {" +
            "    var h = headers[j];" +
            // .children = direct children only; filter to BUTTON elements
            "    var navBtns = Array.from(h.children)" +
            "      .filter(function(c){ return c.tagName === 'BUTTON'; });" +
            "    if (navBtns.length >= 2) {" +
            (forward ? "      navBtns[navBtns.length - 1].click();" : "      navBtns[0].click();") +
            "      return true;" +
            "    }" +
            "  }" +
            "}" +
            "return false;";
        try {
            ((JavascriptExecutor) driver).executeScript(script);
        } catch (Exception ignored) {}
    }

    /**
     * Clicks the calendar cell for the given day number in the currently displayed month.
     * Only clicks cells that are:
     *   - NOT from another month  (data-p-other-month="false" or no such attribute)
     *   - NOT disabled            (no data-p-disabled="true", no p-disabled class)
     *
     * IMPORTANT: Does NOT anchor to any panel-wrapper class because the calendar in
     * this version of Rivio renders INSIDE the modal dialog — there is no separate
     * body-level portal container whose class we could rely on.
     *
     * Four strategies are tried in order; the JS fallback fires last.
     */
    private void clickDayCell(int day) {
        String dayStr = String.valueOf(day);

        // Strategy A: PrimeNG 17 data attributes on the <td>
        // Anchored to the calendar TABLE so it won't pick up non-calendar tables.
        By stratA = By.xpath(
            "//table[@role='grid' or contains(@class,'p-datepicker-calendar')]"
            + "//td[@data-p-other-month='false'"
            + "     and (@data-p-disabled='false' or not(@data-p-disabled))]"
            + "//span[normalize-space()='" + dayStr + "']");

        // Strategy B: class-based PrimeNG 14-16 / fallback
        By stratB = By.xpath(
            "//table[contains(@class,'p-datepicker-calendar') or @role='grid']"
            + "//td[not(contains(@class,'p-disabled'))"
            + "     and not(contains(@class,'p-datepicker-other-month'))"
            + "     and not(contains(@class,'other-month'))]"
            + "//span[normalize-space()='" + dayStr + "'][1]");

        // Strategy C: permissive — any visible calendar td, no other-month/disabled filter
        // (accepts first match; safe because navigateToMonth already points to correct month)
        By stratC = By.xpath(
            "//table//td[not(contains(@class,'p-disabled'))"
            + "           and not(contains(@class,'other-month'))]"
            + "//span[normalize-space()='" + dayStr + "'][1]");

        // ── IMPORTANT: use driver.findElements() (IMMEDIATE — no timeout) ──────────
        // The old code used WaitUtils.waitForClickability() which waits the full
        // EXPLICIT_WAIT (20 s) before throwing when nothing is found.
        // With 3 strategies that is 3 × 20 s = 60 s per date click.
        // findElements() returns an empty list in < 5 ms when no element matches,
        // so all three strategies together take < 50 ms on a miss.
        for (By locator : new By[]{stratA, stratB, stratC}) {
            try {
                List<WebElement> cells = driver.findElements(locator); // immediate!
                for (WebElement cell : cells) {
                    if (cell.isDisplayed()) {
                        WaitUtils.safeClick(driver, cell);
                        WaitUtils.hardWait(400);
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }

        // Strategy D: JavaScript click as last resort (also immediate)
        clickDayCellViaJS(day);
    }

    /**
     * JavaScript fallback for clicking a calendar day cell.
     * Scans all calendar tables for a <span> whose trimmed text equals the day number,
     * skipping cells whose parent td has other-month or disabled markers.
     */
    private void clickDayCellViaJS(int day) {
        try {
            String script =
                "var day = '" + day + "';" +
                "var tables = document.querySelectorAll(" +
                "    'table.p-datepicker-calendar, table[role=\"grid\"]');" +
                "for (var i = 0; i < tables.length; i++) {" +
                "  var spans = tables[i].querySelectorAll('td span');" +
                "  for (var j = 0; j < spans.length; j++) {" +
                "    var span = spans[j];" +
                "    if (span.textContent.trim() !== day) continue;" +
                "    var td = span.parentElement;" +
                "    if (!td) continue;" +
                "    var cls = td.className || '';" +
                "    if (cls.indexOf('other-month') >= 0) continue;" +
                "    if (cls.indexOf('p-disabled') >= 0) continue;" +
                "    if (td.getAttribute('data-p-other-month') === 'true') continue;" +
                "    if (td.getAttribute('data-p-disabled') === 'true') continue;" +
                "    span.click(); return true;" +
                "  }" +
                "}" +
                "return false;";
            ((JavascriptExecutor) driver).executeScript(script);
            WaitUtils.hardWait(350);
        } catch (Exception ignored) {}
    }

    /**
     * Presses Escape to dismiss the calendar overlay if it is still open after
     * both date selections are complete.
     */
    private void closeCalendarIfOpen() {
        if (!isCalendarPanelOpen()) return;
        try {
            driver.findElement(By.cssSelector("body")).sendKeys(Keys.ESCAPE);
            WaitUtils.hardWait(300);
        } catch (Exception ignored) {}
    }

    // ════════════════════════════════════════════════════════════════════════
    // Working-day date resolver
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Returns the date that is {@code offsetDays} WORKING DAYS from {@code base}.
     *
     * Positive offset → future working days (skips Sat/Sun going forward).
     * Zero           → base itself, rolled forward to Monday if it falls on a weekend.
     * Negative offset → past working days (skips Sat/Sun going backward).
     */
    private LocalDate resolveWorkingDay(LocalDate base, int offsetDays) {
        LocalDate result = base;
        int step = offsetDays >= 0 ? 1 : -1;
        int remaining = Math.abs(offsetDays);

        while (remaining > 0) {
            result = result.plusDays(step);
            if (result.getDayOfWeek() != DayOfWeek.SATURDAY
                    && result.getDayOfWeek() != DayOfWeek.SUNDAY) {
                remaining--;
            }
        }

        // Zero offset: if base is a weekend, roll to next Monday
        if (offsetDays == 0) {
            while (result.getDayOfWeek() == DayOfWeek.SATURDAY
                    || result.getDayOfWeek() == DayOfWeek.SUNDAY) {
                result = result.plusDays(1);
            }
        }
        return result;
    }
}
