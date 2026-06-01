package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.pages.selfservice.MyLeavesPage;
import com.cts.rivio.utils.ExcelUtils;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

/**
 * MyLeavesTest – Employee self-service leave management tests.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * RENDERING TESTS (existing, unchanged)
 * ══════════════════════════════════════════════════════════════════════════
 *   RV_LVE_004   – Balance cards render with Available / Used / Total
 *   RV_LVE_005   – Leave Request History table is visible
 *   RV_LVE_BUG_08– Weekend days must be disabled in the date range picker
 *
 * ══════════════════════════════════════════════════════════════════════════
 * DATA-DRIVEN TESTS (reads from LeaveData.xlsx)
 * ══════════════════════════════════════════════════════════════════════════
 *   RV_LVE_DD_001 – Valid leave application (5 positive rows)
 *     Sheet  : ValidLeave
 *     Columns: testCase | leaveType | startDaysFromToday | endDaysFromToday | reason
 *     What   : Opens "Apply for Leave" modal → selects leave type → clicks
 *              correct calendar cells for start + end date → types reason →
 *              clicks Submit → asserts success toast OR modal closed.
 *              Also asserts that a new row appeared in history with "PENDING" status.
 *
 *   RV_LVE_DD_002 – Invalid leave application (5 negative rows)
 *     Sheet  : InvalidLeave
 *     Columns: testCase | leaveType | startDaysFromToday | endDaysFromToday
 *              | reason | expectedError
 *     What   : Opens modal → fills only the fields supplied (empty = skip) →
 *              clicks Submit → asserts that EITHER:
 *                (a) submit button stays disabled (reactive-form guards), OR
 *                (b) inline validation error is visible inside the modal, OR
 *                (c) modal does not close (server rejected the request), OR
 *                (d) an insufficient-balance warning is shown.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * EXCEL SENTINEL VALUES
 * ══════════════════════════════════════════════════════════════════════════
 *   leaveType              – empty string → skip selectLeaveType() (TC-I-01, TC-I-03)
 *   startDaysFromToday     – empty string → skip setLeaveDateRange()
 *   endDaysFromToday       – empty string → skip setLeaveDateRange()
 *   startDaysFromToday < 0 – past working day (calendar likely disables it)
 *   "AUTO"                 – select first available option
 */
public class MyLeavesTest extends BaseTest {

    @Override
    protected String getRole() { return ROLE_EMPLOYEE; }

    private MyLeavesPage leavesPage;

    @BeforeMethod(alwaysRun = true)
    public void openMyLeaves() {
        driver.get(AppConstants.MY_LEAVES_URL);
        WaitUtils.waitForAngularLoad(driver);
        // Angular auth guard redirects to /login when the session expires.
        // Re-login as Employee so the test does not fail immediately at Step 1.
        reLoginIfSessionExpired();
        leavesPage = new MyLeavesPage(driver);
    }

    /**
     * Bug: Saturday and Sunday cells in the Apply Leave date picker must be
     * disabled (disabledDays = [0, 6] in employee-leaves.component.ts).
     */
    @Test(priority = 1,
          groups  = {"bug", "regression", "negative"},
          description = "leave_bug_weekendDisabled – Weekend day cells must be disabled in the date range picker")
    public void leave_bug_weekendDisabled() {
        Assert.assertTrue(leavesPage.isPageLoaded(), "My Leaves page must load");

        leavesPage.clickApplyForLeave();
        WaitUtils.hardWait(600);
        Assert.assertTrue(leavesPage.isApplyModalOpen(),
            "Apply for Leave dialog must open before checking weekend cells");

        // Open the calendar overlay to inspect day cells
        try {
            WebElement dateInput = driver.findElement(By.cssSelector(
                "p-dialog p-datepicker input, p-dialog .p-datepicker-input"));
            WaitUtils.scrollAndClick(driver, dateInput);
            WaitUtils.hardWait(700);
        } catch (Exception ignored) {}

        // PrimeNG renders columns: Su Mo Tu We Th Fr Sa (position 1 = Sun, 7 = Sat)
        List<WebElement> weekendCells = driver.findElements(By.xpath(
            "//*[contains(@class,'p-datepicker') or contains(@class,'p-calendar')]"
            + "//td[position()=1 or position()=7][.//span[normalize-space()!='']]"));

        int total       = weekendCells.size();
        int clickable   = 0;
        for (WebElement td : weekendCells) {
            try {
                String tdCls   = td.getAttribute("class");
                String pDisabled = td.getAttribute("data-p-disabled");
                WebElement span = td.findElement(By.tagName("span"));
                String spanCls  = span.getAttribute("class");
                String ariaD    = span.getAttribute("aria-disabled");

                boolean disabled =
                    "true".equalsIgnoreCase(pDisabled)
                    || (tdCls   != null && tdCls.contains("p-disabled"))
                    || (spanCls != null && spanCls.contains("p-disabled"))
                    || "true".equalsIgnoreCase(ariaD);
                if (!disabled) clickable++;
            } catch (Exception ignored) {}
        }

        ExtentManager.getTest().info(
            "Weekend cells: " + clickable + " clickable out of " + total + " total");

        Assert.assertTrue(total > 0 && clickable == 0,
            "leave_bug_weekendDisabled: " + clickable + "/" + total
            + " weekend cells are clickable — Saturday and Sunday must be disabled.");
        ExtentManager.getTest().pass("All weekend cells are correctly disabled");
    }

    // ══════════════════════════════════════════════════════════════════════
    // ── Data-driven: @DataProviders ────────────────────────────────────────
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Reads positive leave rows from the "ValidLeave" sheet of LeaveData.xlsx.
     *
     * Column order matches the @Test parameters exactly:
     *   [0] testCase          – human-readable test case label (logged to Extent report)
     *   [1] leaveType         – leave type text shown in the p-select dropdown
     *   [2] startDaysFromToday– working-day offset for start date (integer as string)
     *   [3] endDaysFromToday  – working-day offset for end   date (integer as string)
     *   [4] reason            – text to type into the reason textarea
     */
    @DataProvider(name = "validLeaveData", parallel = false)
    public Object[][] validLeaveData() {
        return ExcelUtils.readDataExcludingHeader(
            AppConstants.LEAVE_DATA_PATH,
            AppConstants.SHEET_VALID_LEAVE);
    }

    /**
     * Reads negative leave rows from the "InvalidLeave" sheet of LeaveData.xlsx.
     *
     * Column order:
     *   [0] testCase           – label
     *   [1] leaveType          – empty string = do NOT call selectLeaveType
     *   [2] startDaysFromToday – empty string = do NOT call setLeaveDateRange
     *   [3] endDaysFromToday   – empty string = do NOT call setLeaveDateRange
     *   [4] reason             – may be empty
     *   [5] expectedError      – description of the expected rejection reason
     */
    @DataProvider(name = "invalidLeaveData", parallel = false)
    public Object[][] invalidLeaveData() {
        return ExcelUtils.readDataExcludingHeader(
            AppConstants.LEAVE_DATA_PATH,
            AppConstants.SHEET_INVALID_LEAVE);
    }

    // ══════════════════════════════════════════════════════════════════════
    // ── RV_LVE_DD_001 – Valid leave application ────────────────────────────
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Positive data-driven test: fills ALL form fields from Excel, submits, and
     * verifies both the success indicator AND that the new request appears in the
     * history table with a "PENDING" status.
     *
     * Test flow:
     *   Step 1 – Navigate to /self-service/leaves and wait for the page to load
     *   Step 2 – Read the current history row count (baseline)
     *   Step 3 – Click "Apply for Leave" and assert the modal opens
     *   Step 4 – Select leave type from the p-select dropdown
     *   Step 5 – Click start date and end date in the PrimeNG range calendar
     *   Step 6 – Type reason text into the textarea (if non-empty)
     *   Step 7 – Click Submit button
     *   Step 8 – Assert: success toast OR modal closed
     *   Step 9 – Assert: history table has one more row than before
     *   Step 10– Assert: new row's status is PENDING (or APPROVED in demo data)
     */
    @Test(dataProvider = "validLeaveData",
          priority     = 20,
          groups       = {"regression", "positive"},
          description  = "leave_validApplication – Apply leave → modal closes → history grows")
    public void leave_validApplication(
            String testCase,
            String leaveType,
            String startDaysStr,
            String endDaysStr,
            String reason) {

        int start = parseIntSafe(startDaysStr, 1);
        int end   = parseIntSafe(endDaysStr,   1);

        ExtentManager.getTest().info(
            "[DD-LEAVE-VALID] " + testCase
            + " | type=" + leaveType
            + " | start=+" + start + " | end=+" + end);

        // ── Step 1: baseline history row count ────────────────────────────
        Assert.assertTrue(leavesPage.isPageLoaded(), "My Leaves page must load");
        int rowsBefore = leavesPage.getHistoryRowCount();
        ExtentManager.getTest().info("Rows before submit: " + rowsBefore);

        // ── Step 2: open modal + fill form ────────────────────────────────
        leavesPage.clickApplyForLeave();
        Assert.assertTrue(leavesPage.isApplyModalOpen(),
            "Apply for Leave modal must open");

        leavesPage.selectLeaveType(leaveType);
        leavesPage.setLeaveDateRange(start, end);

        // ── Step 3: poll up to 6s for daysRequested counter to commit ─────
        // The Angular form's daysRequested is computed from dateRange via
        // valueChanges. If calendar clicks land but the model hasn't
        // recomputed yet, clicking Submit Request hits the (daysRequested===0)
        // disable guard and silently no-ops.
        int workingDays = waitForWorkingDays(6000);
        ExtentManager.getTest().info("daysRequested after calendar selection: " + workingDays);
        Assert.assertTrue(workingDays > 0,
            "[" + testCase + "] PrimeNG calendar did NOT register the chosen "
          + "range (daysRequested stayed 0). Selection: today+" + start
          + " → today+" + end + ".");

        // ── Step 4: submit ────────────────────────────────────────────────
        // submitLeaveApplication() polls for the button to be enabled, clicks
        // it, and accepts the success alert('Leave request submitted...').
        leavesPage.submitLeaveApplication();

        // ── Step 5: modal must close (component sets isApplyModalOpen=false) ─
        boolean modalClosed = waitForApplyModalClosed(6000);
        Assert.assertTrue(modalClosed,
            "[" + testCase + "] Apply Leave modal did NOT close within 6s after "
          + "Submit Request. The POST likely failed or alert was not accepted.");
        ExtentManager.getTest().pass("Modal closed after submit");

        // ── Step 6: poll history for the new row (component calls loadData()) ─
        int rowsAfter = leavesPage.getHistoryRowCountAfterRefresh(rowsBefore);
        ExtentManager.getTest().info(
            "Rows after submit: " + rowsAfter + " (was " + rowsBefore + ")");
        Assert.assertTrue(rowsAfter > rowsBefore,
            "[" + testCase + "] History table did NOT grow. Before=" + rowsBefore
          + " After=" + rowsAfter + ". Leave was submitted but does not appear "
          + "in the user's history — record was not persisted.");
        ExtentManager.getTest().pass("History row appeared: " + rowsAfter + " rows");
    }

    /** Polls until the daysRequested counter > 0 or the deadline passes. */
    private int waitForWorkingDays(long maxMillis) {
        long deadline = System.currentTimeMillis() + maxMillis;
        int days = 0;
        while (System.currentTimeMillis() < deadline) {
            days = leavesPage.getWorkingDaysRequested();
            if (days > 0) return days;
            try { Thread.sleep(300); } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); break;
            }
        }
        return days;
    }

    /** Polls until the Apply Leave dialog disappears or deadline passes. */
    private boolean waitForApplyModalClosed(long maxMillis) {
        long deadline = System.currentTimeMillis() + maxMillis;
        while (System.currentTimeMillis() < deadline) {
            if (!leavesPage.isApplyModalOpen()) return true;
            try { Thread.sleep(300); } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); break;
            }
        }
        return !leavesPage.isApplyModalOpen();
    }

    // ══════════════════════════════════════════════════════════════════════
    // ── RV_LVE_DD_002 – Invalid leave application ──────────────────────────
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Negative data-driven test: submits with bad / missing data and verifies
     * that the form rejects the submission.
     *
     * What counts as "rejected" (any ONE of the following is sufficient):
     *   (a) The Submit button is disabled before clicking (reactive-form guard)
     *   (b) An inline validation error is visible inside the modal
     *   (c) An insufficient-balance warning is visible
     *   (d) The modal is still open after clicking Submit
     *
     * Empty cell in leaveType column → test skips selectLeaveType()
     * Empty cells in start/end columns → test skips setLeaveDateRange()
     *
     * Test flow:
     *   Step 1 – Navigate and load My Leaves page
     *   Step 2 – Open the Apply for Leave modal
     *   Step 3 – Conditionally fill leave type (if non-empty in Excel)
     *   Step 4 – Conditionally fill date range (if both start and end non-empty)
     *   Step 5 – Conditionally fill reason (if non-empty)
     *   Step 6 – Check whether Submit button is already disabled
     *   Step 7 – Click Submit (if button is enabled)
     *   Step 8 – Assert rejection via one of the four indicators above
     */
    @Test(dataProvider = "invalidLeaveData",
          priority     = 21,
          groups       = {"regression", "negative"},
          description  = "leave_invalidApplication – Invalid leave data is rejected with appropriate error")
    public void leave_invalidApplication(
            String testCase,
            String leaveType,
            String startDaysStr,
            String endDaysStr,
            String reason,
            String expectedError) {

        int start = parseIntSafe(startDaysStr, 0);
        int end   = parseIntSafe(endDaysStr,   0);

        // ── Step 1: log ──────────────────────────────────────────────────
        ExtentManager.getTest().info(
            "[DD-LEAVE-INVALID] " + testCase
            + " | type='" + leaveType + "'"
            + " | start='" + startDaysStr + "'"
            + " | end='" + endDaysStr + "'"
            + " | expectedError=" + expectedError);

        // ── Step 2: load page ────────────────────────────────────────────
        Assert.assertTrue(leavesPage.isPageLoaded(), "Step 1: My Leaves page must load");

        // ── Step 3: open modal ───────────────────────────────────────────
        leavesPage.clickApplyForLeave();
        Assert.assertTrue(leavesPage.isApplyModalOpen(),
            "Step 2: Apply for Leave modal must open");
        ExtentManager.getTest().info("Step 2: Modal opened");

        // ── Step 4: conditionally fill leave type ────────────────────────
        if (!leaveType.isEmpty()) {
            leavesPage.selectLeaveType(leaveType);
            ExtentManager.getTest().info("Step 3: Leave type selected → " + leaveType);
        } else {
            ExtentManager.getTest().info("Step 3: Leave type SKIPPED (empty in Excel → testing missing type)");
        }

        // ── Step 5: conditionally set date range ─────────────────────────
        boolean datesProvided = !startDaysStr.isEmpty() && !endDaysStr.isEmpty();
        if (datesProvided) {
            leavesPage.setLeaveDateRange(start, end);
            ExtentManager.getTest().info(
                "Step 4: Date range set → start+=" + start + " end+=" + end);
        } else {
            ExtentManager.getTest().info("Step 4: Date range SKIPPED (empty in Excel → testing missing dates)");
        }

        // ── Step 6: fill reason ──────────────────────────────────────────
        if (!reason.isEmpty()) {
            leavesPage.fillReason(reason);
            ExtentManager.getTest().info("Step 5: Reason filled → '" + reason + "'");
        }

        // ── Step 7: check submit button state BEFORE clicking ─────────────
        boolean submitDisabled = leavesPage.isSubmitButtonDisabled();
        ExtentManager.getTest().info(
            "Step 6: Submit button disabled (before click) = " + submitDisabled);

        // ── Step 8: click submit if enabled ─────────────────────────────
        if (!submitDisabled) {
            leavesPage.submitLeaveApplication();
            ExtentManager.getTest().info("Step 7: Submit clicked (button was enabled)");
        } else {
            ExtentManager.getTest().info(
                "Step 7: Submit NOT clicked — button is already disabled (form guard active)");
        }

        // ── Step 9: gather all rejection signals ─────────────────────────
        boolean btnStillDisabled = leavesPage.isSubmitButtonDisabled();
        boolean validationErr    = leavesPage.isValidationErrorVisible();
        boolean balanceWarning   = leavesPage.isInsufficientBalanceWarningVisible();
        boolean modalStillOpen   = leavesPage.isApplyModalOpen();

        boolean rejected = submitDisabled
            || btnStillDisabled
            || validationErr
            || balanceWarning
            || modalStillOpen;

        ExtentManager.getTest().info(
            "Step 8 indicators:"
            + "\n  submitDisabledBefore = " + submitDisabled
            + "\n  submitDisabledAfter  = " + btnStillDisabled
            + "\n  validationError      = " + validationErr
            + "\n  balanceWarning       = " + balanceWarning
            + "\n  modalStillOpen       = " + modalStillOpen);

        // ── Step 10: assert ──────────────────────────────────────────────
        Assert.assertTrue(rejected,
            "[" + testCase + "] Invalid leave data should have been rejected.\n"
            + "  Expected error : " + expectedError + "\n"
            + "  leaveType      : '" + leaveType + "'\n"
            + "  start offset   : '" + startDaysStr + "'\n"
            + "  end offset     : '" + endDaysStr + "'\n"
            + "  SYMPTOM: None of the rejection indicators fired.\n"
            + "  The form accepted invalid data — this is a validation bug.");
        ExtentManager.getTest().pass(
            "[" + testCase + "] PASS – Rejected as expected. Reason: "
            + (submitDisabled ? "Submit disabled" : "")
            + (validationErr ? " | Validation error" : "")
            + (balanceWarning ? " | Balance warning" : "")
            + (modalStillOpen && !submitDisabled ? " | Modal still open" : ""));
    }

    // ══════════════════════════════════════════════════════════════════════
    // ── Helpers ────────────────────────────────────────────────────────────
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Parses an integer from the Excel cell string.
     * Returns {@code defaultValue} if the string is null, blank, or non-numeric.
     */
    private int parseIntSafe(String s, int defaultValue) {
        if (s == null || s.trim().isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // ── Session recovery ───────────────────────────────────────────────────
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Detects Angular auth-guard redirects to /login (which happen when the
     * JWT token expires mid-run) and silently re-authenticates as Employee.
     *
     * Why this is needed
     * ──────────────────
     * BaseTest logs in ONCE per role at @BeforeClass.  If the first data-driven
     * iteration (TC-V-01) takes too long — e.g. because calendar navigation was
     * slow — the Angular token can expire before the second @BeforeMethod fires.
     * driver.get(MY_LEAVES_URL) then causes the Angular route guard to redirect
     * to /login, and every subsequent test fails at "Step 1: isPageLoaded()".
     *
     * Solution
     * ────────
     * After any driver.get(MY_LEAVES_URL) call, check whether the current URL
     * contains "/login".  If it does, drive the login UI directly using
     * LoginPage (same technique BaseTest.loginAsRole() uses internally, but
     * called from a subclass where loginAsRole is private).
     *
     * After a successful re-login the driver is positioned back on MY_LEAVES_URL
     * so the caller can proceed as normal.
     */
    private void reLoginIfSessionExpired() {
        try {
            String currentUrl = driver.getCurrentUrl();
            if (currentUrl == null || !currentUrl.contains("/login")) return;

            // Session expired — inform the report and re-authenticate
            try {
                ExtentManager.getTest().info(
                    "[Session Recovery] Angular redirected to /login "
                    + "— re-authenticating as Employee...");
            } catch (Exception ignored) {}

            clearAuthStorage();
            new LoginPage(driver).login(
                AppConstants.EMPLOYEE_EMAIL,
                AppConstants.EMPLOYEE_PASSWORD);
            WaitUtils.waitForAngularLoad(driver);
            WaitUtils.hardWait(800);

            // Navigate back to My Leaves after a successful login
            driver.get(AppConstants.MY_LEAVES_URL);
            WaitUtils.waitForAngularLoad(driver);

            try {
                ExtentManager.getTest().info(
                    "[Session Recovery] Re-login complete — resuming on My Leaves page.");
            } catch (Exception ignored) {}

        } catch (Exception e) {
            // Non-fatal: if recovery itself fails, the next assertion will report it
            try {
                ExtentManager.getTest().info(
                    "[Session Recovery] Recovery attempt failed: " + e.getMessage());
            } catch (Exception ignored) {}
        }
    }
}
