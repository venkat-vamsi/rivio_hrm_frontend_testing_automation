package com.cts.rivio.pages.selfservice;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

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

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(xpath = "//button[contains(.,'Apply for Leave')]")
    private WebElement applyForLeaveButton;

    @FindBy(css = "p-dialog .p-dialog")
    private List<WebElement> applyModalContainer;

    @FindBy(css = ".glass-panel.p-6.relative.overflow-hidden")
    private List<WebElement> balanceCards;

    @FindBy(css = "p-table tbody tr")
    private List<WebElement> historyRows;

    @FindBy(css = "p-table tbody tr:first-child")
    private List<WebElement> firstHistoryRow;

    @FindBy(css = "p-dialog textarea[formcontrolname='reason'], "
                + "p-dialog textarea[formcontrolname='notes'], p-dialog textarea")
    private WebElement reasonTextarea;

    // ── Constructor ───────────────────────────────────────────────────────────

    public MyLeavesPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

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
        WaitUtils.waitForClickability(driver, By.xpath("//button[contains(.,'Apply for Leave')]"));
        WaitUtils.safeClick(driver, applyForLeaveButton);
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

        diag("Target range: " + startDate + " → " + endDate);

        for (int attempt = 0; attempt < 2; attempt++) {
            diag("Attempt " + (attempt + 1) + "/2 — opening calendar");
            openCalendarPicker();
            if (!isCalendarPanelOpen()) {
                diag("Calendar panel did NOT open — aborting attempt");
                continue;
            }

            // ── click START date ───────────────────────────────────────────
            diag("Navigating to start month: " + startDate.getMonthValue() + "/" + startDate.getYear());
            navigateToMonth(startDate.getYear(), startDate.getMonthValue());
            diag("Clicking start day cell: " + startDate.getDayOfMonth());
            boolean startClicked = clickDayCell(startDate.getDayOfMonth());
            WaitUtils.hardWait(700);
            diag("Start click landed=" + startClicked + " | calendarStillOpen=" + isCalendarPanelOpen());

            // ── click END date (range mode keeps calendar open) ────────────
            if (!isCalendarPanelOpen()) {
                diag("Calendar closed after start click — reopening for end date");
                openCalendarPicker();
                WaitUtils.hardWait(500);
            }
            diag("Navigating to end month: " + endDate.getMonthValue() + "/" + endDate.getYear());
            navigateToMonth(endDate.getYear(), endDate.getMonthValue());
            diag("Clicking end day cell: " + endDate.getDayOfMonth());
            boolean endClicked = clickDayCell(endDate.getDayOfMonth());
            WaitUtils.hardWait(900);    // give PrimeNG time to commit + auto-close

            // ── DO NOT manually close calendar — range mode auto-closes ────
            // (Old code pressed ESC on body, which the modal caught and
            // closed itself. New code trusts PrimeNG to dismiss the panel.)

            // ── poll up to 3 seconds for daysRequested to commit ───────────
            int workingDays = pollForDaysRequested(3000);
            diag("End click landed=" + endClicked
                + " | calendarStillOpen=" + isCalendarPanelOpen()
                + " | dialogStillOpen=" + isApplyModalOpen()
                + " | workingDays=" + workingDays
                + " | dateInputText=\"" + getDateInputText() + "\"");

            if (workingDays > 0) return;                         // success

            // Days still 0: clear and retry
            diag("workingDays still 0 — retrying after clearing input");
            tryClearDateInput();
            WaitUtils.hardWait(500);
        }
    }

    /** Polls getWorkingDaysRequested() every 300ms up to {@code maxMs}. */
    private int pollForDaysRequested(long maxMs) {
        long deadline = System.currentTimeMillis() + maxMs;
        int days;
        while (System.currentTimeMillis() < deadline) {
            days = getWorkingDaysRequested();
            if (days > 0) return days;
            WaitUtils.hardWait(300);
        }
        return getWorkingDaysRequested();
    }

    /** Returns the readonly text shown in the date-range input, or "" if unreadable. */
    private String getDateInputText() {
        try {
            WebElement input = driver.findElement(By.cssSelector(
                "p-dialog p-datepicker input, p-dialog .p-datepicker-input"));
            String v = input.getAttribute("value");
            return v == null ? "" : v;
        } catch (Exception e) {
            return "";
        }
    }

    /** Writes a diagnostic line to the Extent report so we can see flow at runtime. */
    private void diag(String msg) {
        try {
            com.cts.rivio.utils.ExtentManager.getTest().info("[Leave-Calendar] " + msg);
        } catch (Exception ignored) {}
        System.out.println("[Leave-Calendar] " + msg);
    }

    /**
     * Returns the number shown in the "WORKING DAYS REQUESTED: N DAY(S)" counter
     * inside the Apply Leave modal.  Returns 0 if the counter cannot be read or
     * shows zero — meaning PrimeNG has not accepted the date selection yet.
     */
    public int getWorkingDaysRequested() {
        // Live DOM (employee-leaves.component.html lines 140-151):
        //   <label>Working Days Requested</label>
        //   <div ...>
        //     <span class="font-black text-xl ..."> 1 <span>Day(s)</span></span>
        //   </div>
        // NOTE: there is NO colon between the label and the number. The old
        // regex ":\s*(\d+)" required one, so it always returned 0 even when
        // PrimeNG had committed 1+ day. New strategy: read the dialog text
        // and grab the first integer that precedes "Day(s)".
        try {
            WebElement dialog = driver.findElement(By.cssSelector("p-dialog .p-dialog"));
            String text = dialog.getText();

            // Primary pattern: "N Day(s)" or "N DAY(S)" (case-insensitive)
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "(\\d+)\\s*Day\\(s\\)",
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
            if (m.find()) return Integer.parseInt(m.group(1));

            // Fallback: any integer following the "Working Days Requested" label
            m = java.util.regex.Pattern.compile(
                "Working Days Requested[^0-9]*(\\d+)",
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
            if (m.find()) return Integer.parseInt(m.group(1));
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
            WebElement el = WaitUtils.waitForVisibility(driver, reasonTextarea);
            el.clear();
            el.sendKeys(reason);
        } catch (Exception ignored) {}
    }

    /**
     * Clicks the "Submit Request" button in the Apply Leave modal, then
     * accepts the JS alert that the Angular component fires on success.
     *
     * Why it was broken before:
     *   • Old code waited up to EXPLICIT_WAIT for `clickability` and treated
     *     a disabled button as "click anyway via JS" — which silently
     *     no-oped the form submission because the form was still invalid
     *     (insufficientBalance / daysRequested===0 / dateRange not committed).
     *   • The Angular submitLeaveRequest() success path calls
     *     `alert('Leave request submitted successfully!')` — a NATIVE
     *     browser alert that blocks Selenium until accepted.
     *
     * New flow:
     *   1. Poll up to 5s for the button to be ENABLED (no `disabled`
     *      attribute and no `p-disabled` class). If it never enables,
     *      throw with a diagnostic snapshot of the failing form state.
     *   2. Click it.
     *   3. Accept the JS alert if one appears within 4s.
     *   4. Wait for Angular to reload data (component calls loadData()).
     */
    public void submitLeaveApplication() {
        By btnBy = By.xpath(
            "//p-dialog//button[normalize-space()='Submit Request' "
          + "or contains(normalize-space(.),'Submit Request')]");

        // ── 1. Poll up to 5s for the button's disabled-attr to clear ─────
        long deadline = System.currentTimeMillis() + 5000;
        WebElement btn = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                List<WebElement> btns = driver.findElements(btnBy);
                if (!btns.isEmpty()) {
                    btn = btns.get(0);
                    String disabled = btn.getAttribute("disabled");
                    String cls = btn.getAttribute("class");
                    boolean cssDisabled = cls != null && cls.contains("p-disabled");
                    boolean attrDisabled = disabled != null && !disabled.isEmpty()
                                        && !"false".equalsIgnoreCase(disabled);
                    if (!attrDisabled && !cssDisabled) {
                        diag("Submit Request enabled — disabled-attr=null, class=\"" + cls + "\"");
                        break;
                    }
                }
            } catch (Exception ignored) {}
            WaitUtils.hardWait(250);
            btn = null;
        }

        if (btn == null) {
            int days = getWorkingDaysRequested();
            throw new RuntimeException(
                "Submit Request never became enabled within 5s. workingDays="
              + days + ". Likely cause: dateRange didn't register in the "
              + "FormControl (calendar clicks missed), leaveTypeId blank, "
              + "or insufficient balance for the chosen range.");
        }

        // Scroll into view so neither sticky header nor calendar overlay
        // can intercept the click
        try {
            ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
            WaitUtils.hardWait(250);
        } catch (Exception ignored) {}

        // ── 2. STRATEGY A: native WebDriver click ────────────────────────
        boolean clickedNative = false;
        try {
            btn.click();
            clickedNative = true;
            diag("Strategy A (native click) executed");
        } catch (Exception e) {
            diag("Strategy A failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        // Give Angular 800ms to flip isSubmitting → start the API call → fire alert
        WaitUtils.hardWait(800);
        if (handleSubmitOutcome()) return;

        // ── 3. STRATEGY B: JS .click() on the button element ─────────────
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
            diag("Strategy B (JS click) executed");
        } catch (Exception e) {
            diag("Strategy B failed: " + e.getMessage());
        }
        WaitUtils.hardWait(800);
        if (handleSubmitOutcome()) return;

        // ── 4. STRATEGY C: form.requestSubmit() — fires ngSubmit reliably ─
        // A native `.click()` on a `type=submit` button doesn't always
        // trigger Angular's (ngSubmit) handler when zone.js misses the
        // event. requestSubmit() is the documented way to programmatically
        // submit a form WITH validation + ngSubmit firing.
        try {
            Object result = ((JavascriptExecutor) driver).executeScript(
                "var form = document.querySelector('p-dialog form');" +
                "if (!form) return 'no form found';" +
                "if (typeof form.requestSubmit === 'function') {" +
                "  form.requestSubmit();" +
                "  return 'requestSubmit';" +
                "} else {" +
                "  form.dispatchEvent(new Event('submit', {bubbles:true,cancelable:true}));" +
                "  return 'dispatchEvent';" +
                "}");
            diag("Strategy C (form.requestSubmit) result: " + result);
        } catch (Exception e) {
            diag("Strategy C failed: " + e.getMessage());
        }
        WaitUtils.hardWait(800);
        handleSubmitOutcome();
    }

    /**
     * After a submit attempt: accept the success alert if present, wait for
     * Angular to settle, and report whether the dialog closed. Returns true
     * if the submit succeeded (alert handled OR modal closed).
     */
    private boolean handleSubmitOutcome() {
        // Native browser alert "Leave request submitted successfully!" — accept
        // with a 3s visibility pause for manual validation
        boolean alertSeen = acceptAlertIfPresent(2000);
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.hardWait(500);
        boolean modalClosed = !isApplyModalOpen();
        diag("Submit outcome — alertSeen=" + alertSeen + ", modalClosed=" + modalClosed);
        return alertSeen || modalClosed;
    }

    /**
     * Polls up to {@code maxMillis} for a native JS alert. When found, pauses
     * 3 seconds so the alert is visible during demo / video runs ("Leave
     * request submitted successfully!" needs to be readable on screen for
     * manual validation), then accepts it.
     */
    private boolean acceptAlertIfPresent(long maxMillis) {
        long deadline = System.currentTimeMillis() + maxMillis;
        while (System.currentTimeMillis() < deadline) {
            try {
                org.openqa.selenium.Alert alert = driver.switchTo().alert();
                String text = alert.getText();
                try {
                    com.cts.rivio.utils.ExtentManager.getTest().info(
                        "Alert detected — pausing 3s for visibility: \""
                      + text + "\"");
                } catch (Exception ignored) {}
                WaitUtils.hardWait(3000);
                alert.accept();
                return true;
            } catch (org.openqa.selenium.NoAlertPresentException ignored) {
                WaitUtils.hardWait(200);
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
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
            if (firstHistoryRow.isEmpty()) return "";
            WebElement row = firstHistoryRow.get(0);
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
        return balanceCards.size();
    }

    public int getHistoryRowCount() {
        return historyRows.size();
    }

    public boolean isApplyModalOpen() {
        return !applyModalContainer.isEmpty();
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
    /**
     * Clicks the calendar cell for the given day number in the currently displayed month.
     * Returns true if a click was successfully executed on a visible cell, false if
     * all four strategies missed (caller can then decide to retry or log a failure).
     */
    private boolean clickDayCell(int day) {
        String dayStr = String.valueOf(day);

        By stratA = By.xpath(
            "//table[@role='grid' or contains(@class,'p-datepicker-calendar')]"
            + "//td[@data-p-other-month='false'"
            + "     and (@data-p-disabled='false' or not(@data-p-disabled))]"
            + "//span[normalize-space()='" + dayStr + "']");

        By stratB = By.xpath(
            "//table[contains(@class,'p-datepicker-calendar') or @role='grid']"
            + "//td[not(contains(@class,'p-disabled'))"
            + "     and not(contains(@class,'p-datepicker-other-month'))"
            + "     and not(contains(@class,'other-month'))]"
            + "//span[normalize-space()='" + dayStr + "'][1]");

        By stratC = By.xpath(
            "//table//td[not(contains(@class,'p-disabled'))"
            + "           and not(contains(@class,'other-month'))]"
            + "//span[normalize-space()='" + dayStr + "'][1]");

        for (By locator : new By[]{stratA, stratB, stratC}) {
            try {
                List<WebElement> cells = driver.findElements(locator);
                for (WebElement cell : cells) {
                    if (cell.isDisplayed()) {
                        WaitUtils.safeClick(driver, cell);
                        WaitUtils.hardWait(400);
                        return true;
                    }
                }
            } catch (Exception ignored) {}
        }

        // Strategy D: JavaScript click as last resort
        return clickDayCellViaJS(day);
    }

    /**
     * JavaScript fallback for clicking a calendar day cell.
     * Returns true if the script located and clicked a matching span, false otherwise.
     */
    private boolean clickDayCellViaJS(int day) {
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
            Object result = ((JavascriptExecutor) driver).executeScript(script);
            WaitUtils.hardWait(350);
            return Boolean.TRUE.equals(result);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Dismisses the PrimeNG calendar overlay if it is still open.
     *
     * IMPORTANT: do NOT press Escape. PrimeNG p-dialog has closeOnEscape=true
     * by default, so an Escape sent to <body> closes the WHOLE LEAVE APPLY
     * MODAL — which is exactly what the user reported ("form just closed after
     * date picked"). In range-mode the calendar auto-closes after the second
     * click anyway; the only remaining case is when the second click missed.
     *
     * Safe dismissal: click on the dialog HEADER (part of the modal, so the
     * modal stays open, but loses focus from the calendar overlay so PrimeNG
     * closes it). The dialog header has class `p-dialog-header`.
     */
    private void closeCalendarIfOpen() {
        if (!isCalendarPanelOpen()) return;
        try {
            List<WebElement> headers = driver.findElements(By.cssSelector(
                "p-dialog .p-dialog-header"));
            if (!headers.isEmpty()) {
                ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].click();", headers.get(0));
                WaitUtils.hardWait(300);
            }
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
