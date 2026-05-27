package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.EmployeeDirectoryPage;
import com.cts.rivio.pages.EmployeeOnboardPage;
import com.cts.rivio.utils.ExcelUtils;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

/**
 * EmployeeTest — Employee Management module.
 *
 * Naming pattern: {@code emp_<scenario>}.
 *
 *   emp_directorySearch         – Real-time search filters the directory
 *   emp_validOnboarding         – Add employee + verify searchable (DP, 6 rows)
 *   emp_invalidOnboarding       – Field validators reject bad input (DP, 20 rows)
 *   emp_validBankAccount        – Bank-account happy path (DP, 1 row)
 *   emp_validPhone              – Phone happy path (DP, 1 row)
 *   emp_bug_bankValidation      – Edit Contact modal accepts non-numeric bank (DP, 4 rows)
 *   emp_bug_phoneValidation     – Edit Contact modal accepts non-numeric phone (DP, 4 rows)
 */
public class EmployeeTest extends BaseTest {

    @Override protected String getRole() { return ROLE_ADMIN; }

    private EmployeeDirectoryPage directory;

    @BeforeMethod(alwaysRun = true)
    public void openDirectory() {
        // Bucket session is already logged in as Admin via BaseTest @BeforeClass.
        driver.get(AppConstants.EMPLOYEE_DIR_URL);
        WaitUtils.waitForAngularLoad(driver);
        directory = new EmployeeDirectoryPage(driver);
    }

    @Test(priority = 1, groups = {"smoke", "regression", "positive"},
          description = "emp_directorySearch – Real-time search filters the directory")
    public void emp_directorySearch() {
        WaitUtils.waitForUrlContains(driver, "/employees");
        WaitUtils.waitForAngularLoad(driver);

        int before = directory.getRowCount();
        String urlBefore = driver.getCurrentUrl();

        directory.searchEmployee("zzzzz_no_match_xyz");

        Assert.assertEquals(driver.getCurrentUrl(), urlBefore,
                "URL should NOT change during real-time search");
        int after = directory.getRowCount();
        ExtentManager.getTest().info("Rows before: " + before + ", after no-match search: " + after);
        Assert.assertTrue(after <= before,
                "Row count must not grow when search has no matches");

        directory.clearSearch();
        ExtentManager.getTest().pass("Search filters the list in real time");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bugs — Edit Contact & Info modal on /employees/:id
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * emp_bug_bankValidation: Bank Account input accepts any random string. Per
     * Rivio_Angular-main employee-profile.component.html lines 186-189, the
     * "bankAccount" field has no validator beyond presence — alphabetic /
     * symbol input is saved as-is.
     *
     * Data-driven across 12 invalid bank account values from
     * EmployeeData.xlsx → InvalidBankAccount sheet (alphabetic, special chars,
     * too short, too long, decimal, scientific notation, etc).
     */
    @DataProvider(name = "invalidBankAccountData")
    public Object[][] invalidBankAccountData() {
        return ExcelUtils.readDataExcludingHeader(
                AppConstants.EMPLOYEE_DATA_PATH,
                AppConstants.SHEET_INVALID_BANK);
    }

    @Test(dataProvider = "invalidBankAccountData",
          priority = 10,
          groups = {"bug", "regression", "negative"},
          description = "emp_bug_bankValidation – Bank Account field must reject non-numeric input")
    public void emp_bug_bankValidation(
            String testCase, String bankValue, String expectedError) {

        ExtentManager.getTest().info(
            "[" + testCase + "] bankValue='" + bankValue + "' | expected=" + expectedError);

        openEditContactModalFromFirstProfile();

        WebElement bank = findInputByFormControlName("bankAccount");
        Assert.assertNotNull(bank,
                "Bank Account input not found in Edit Contact & Info modal — selector drift");

        // Type the invalid value
        WaitUtils.jsSetValue(driver, bank, bankValue);
        bank.sendKeys(Keys.TAB);
        WaitUtils.hardWait(300);

        // Capture state BEFORE clicking save (form-guard signals)
        boolean markedInvalidBefore = isMarkedInvalid(bank);
        boolean saveDisabledBefore  = isSaveDisabled();
        boolean errorBefore         = hasNearbyError("bank", "account");

        // ACTUALLY try to save
        clickSaveChanges();
        WaitUtils.hardWait(1500);

        // After save attempt — look for an EXPLICIT validation indicator only.
        // We intentionally do NOT treat "modal still open" as rejection: the
        // modal can also stay open due to API latency or a non-validation
        // error, which would falsely report this bug as fixed.
        WebElement bankAfter = findInputByFormControlName("bankAccount");
        boolean markedInvalidAfter = bankAfter != null && isMarkedInvalid(bankAfter);
        boolean errorAfter = hasNearbyError("bank", "account");
        boolean modalClosed = !isEditContactModalOpen();

        boolean rejected = markedInvalidBefore || saveDisabledBefore || errorBefore
                       || markedInvalidAfter || errorAfter;

        ExtentManager.getTest().info(
            "Indicators — before: markedInvalid=" + markedInvalidBefore
            + " saveDisabled=" + saveDisabledBefore + " error=" + errorBefore
            + " | after-save: markedInvalid=" + markedInvalidAfter
            + " error=" + errorAfter + " modalClosed=" + modalClosed);

        // Restore clean state for the next iteration BEFORE the assertion
        closeEditContactModalIfOpen();

        Assert.assertTrue(rejected,
                "emp_bug_bankValidation [" + testCase + "]: bankAccount='" + bankValue
              + "' was accepted by the form (no visible validation error). "
              + "Expected: " + expectedError + ". "
              + "The field must validate the account-number format (numeric, "
              + "length, or pattern) — otherwise any random string is persisted. "
              + "modalClosed=" + modalClosed);
        ExtentManager.getTest().pass("Bank Account rejected as expected [" + testCase + "]");
    }

    /**
     * emp_bug_phoneValidation: Phone Number input accepts arbitrary character strings.
     * Per Rivio_Angular-main employee-profile.component.html lines 182-185,
     * "phoneNo" has no validator — alphabetic input is saved as-is.
     *
     * Data-driven across 12 invalid phone formats from EmployeeData.xlsx →
     * InvalidPhone sheet (alphabetic, too short, too long, special chars,
     * leading zero, country code, decimals, etc).
     */
    @DataProvider(name = "invalidPhoneData")
    public Object[][] invalidPhoneData() {
        return ExcelUtils.readDataExcludingHeader(
                AppConstants.EMPLOYEE_DATA_PATH,
                AppConstants.SHEET_INVALID_PHONE);
    }

    @Test(dataProvider = "invalidPhoneData",
          priority = 11,
          groups = {"bug", "regression", "negative"},
          description = "emp_bug_phoneValidation – Phone field must reject non-numeric input")
    public void emp_bug_phoneValidation(
            String testCase, String phoneValue, String expectedError) {

        ExtentManager.getTest().info(
            "[" + testCase + "] phoneValue='" + phoneValue + "' | expected=" + expectedError);

        openEditContactModalFromFirstProfile();

        WebElement phone = findInputByFormControlName("phoneNo");
        Assert.assertNotNull(phone,
                "Phone Number input not found in Edit Contact & Info modal — selector drift");

        WaitUtils.jsSetValue(driver, phone, phoneValue);
        phone.sendKeys(Keys.TAB);
        WaitUtils.hardWait(300);

        boolean markedInvalidBefore = isMarkedInvalid(phone);
        boolean saveDisabledBefore  = isSaveDisabled();
        boolean errorBefore         = hasNearbyError("phone", "mobile");

        clickSaveChanges();
        WaitUtils.hardWait(1500);

        WebElement phoneAfter = findInputByFormControlName("phoneNo");
        boolean markedInvalidAfter = phoneAfter != null && isMarkedInvalid(phoneAfter);
        boolean errorAfter = hasNearbyError("phone", "mobile");
        boolean modalClosed = !isEditContactModalOpen();

        // Strict: explicit validation indicator only (no "modal stayed open" fallback)
        boolean rejected = markedInvalidBefore || saveDisabledBefore || errorBefore
                       || markedInvalidAfter || errorAfter;

        ExtentManager.getTest().info(
            "Indicators — before: markedInvalid=" + markedInvalidBefore
            + " saveDisabled=" + saveDisabledBefore + " error=" + errorBefore
            + " | after-save: markedInvalid=" + markedInvalidAfter
            + " error=" + errorAfter + " modalClosed=" + modalClosed);

        closeEditContactModalIfOpen();

        Assert.assertTrue(rejected,
                "emp_bug_phoneValidation [" + testCase + "]: phoneNo='" + phoneValue
              + "' was accepted by the form (no visible validation error). "
              + "Expected: " + expectedError + ". "
              + "The field must validate numeric phone-format input. "
              + "modalClosed=" + modalClosed);
        ExtentManager.getTest().pass("Phone Number rejected as expected [" + testCase + "]");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // VALID DATA-DRIVEN: positive cases for bank + phone
    // ══════════════════════════════════════════════════════════════════════════

    @DataProvider(name = "validBankAccountData")
    public Object[][] validBankAccountData() {
        return ExcelUtils.readDataExcludingHeader(
                AppConstants.EMPLOYEE_DATA_PATH,
                AppConstants.SHEET_VALID_BANK);
    }

    /**
     * emp_validBankAccount — typing a valid bank number and clicking Save
     * MUST close the modal (success) and persist the value. This is the happy
     * path counterpart to emp_bug_bankValidation and protects against over-aggressive
     * validation regressions once the bug is fixed.
     */
    @Test(dataProvider = "validBankAccountData",
          priority = 12,
          groups = {"regression", "positive"},
          description = "emp_validBankAccount – Valid numeric bank account is accepted")
    public void emp_validBankAccount(
            String testCase, String bankValue) {

        ExtentManager.getTest().info("[" + testCase + "] bankValue='" + bankValue + "'");
        openEditContactModalFromFirstProfile();

        WebElement bank = findInputByFormControlName("bankAccount");
        Assert.assertNotNull(bank, "Bank Account input not found — selector drift");

        WaitUtils.jsSetValue(driver, bank, bankValue);
        bank.sendKeys(Keys.TAB);
        WaitUtils.hardWait(300);

        clickSaveChanges();

        // Poll up to 5s for modal to close OR success toast to appear
        boolean accepted = waitForSaveResult(true);

        closeEditContactModalIfOpen();

        Assert.assertTrue(accepted,
                "[" + testCase + "] Valid bankAccount='" + bankValue
              + "' was NOT accepted within 5s after Save click. "
              + "Either the modal failed to close or no success indicator appeared.");
        ExtentManager.getTest().pass("Bank Account accepted [" + testCase + "]");
    }

    @DataProvider(name = "validPhoneData")
    public Object[][] validPhoneData() {
        return ExcelUtils.readDataExcludingHeader(
                AppConstants.EMPLOYEE_DATA_PATH,
                AppConstants.SHEET_VALID_PHONE);
    }

    @Test(dataProvider = "validPhoneData",
          priority = 13,
          groups = {"regression", "positive"},
          description = "emp_validPhone – Valid 10-digit phone is accepted")
    public void emp_validPhone(
            String testCase, String phoneValue) {

        ExtentManager.getTest().info("[" + testCase + "] phoneValue='" + phoneValue + "'");
        openEditContactModalFromFirstProfile();

        WebElement phone = findInputByFormControlName("phoneNo");
        Assert.assertNotNull(phone, "Phone input not found — selector drift");

        WaitUtils.jsSetValue(driver, phone, phoneValue);
        phone.sendKeys(Keys.TAB);
        WaitUtils.hardWait(300);

        clickSaveChanges();

        boolean accepted = waitForSaveResult(true);

        closeEditContactModalIfOpen();

        Assert.assertTrue(accepted,
                "[" + testCase + "] Valid phoneNo='" + phoneValue
              + "' was NOT accepted within 5s after Save click.");
        ExtentManager.getTest().pass("Phone Number accepted [" + testCase + "]");
    }

    /**
     * Polls up to 5s for the Edit Contact dialog to either close (success) or
     * for a success toast to appear. Returns true if either happens.
     */
    private boolean waitForSaveResult(boolean expectSuccess) {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            boolean modalClosed = !isEditContactModalOpen();
            boolean toast = !driver.findElements(By.cssSelector(
                ".p-toast-message-success, [class*='toast-message-success']")).isEmpty();
            if (modalClosed || toast) return true;
            WaitUtils.hardWait(300);
        }
        return false;
    }

    // ── helpers for bank/phone testing ───────────────────────────────────────

    /** Clicks Save Changes inside the Edit Contact & Info dialog (if enabled). */
    private void clickSaveChanges() {
        try {
            WebElement save = driver.findElement(By.xpath(
                "//p-dialog//button[contains(normalize-space(.),'Save Changes') "
              + "or normalize-space()='Save' "
              + "or normalize-space()='Update']"));
            if (save.isEnabled() && (save.getAttribute("disabled") == null
                                  || save.getAttribute("disabled").isEmpty())) {
                WaitUtils.scrollAndClick(driver, save);
            }
        } catch (Exception ignored) {}
    }

    private boolean isEditContactModalOpen() {
        return !driver.findElements(By.cssSelector(
            "p-dialog .p-dialog:not(.p-dialog-leave)")).isEmpty()
            && !driver.findElements(By.cssSelector(
            "p-dialog input[formcontrolname='bankAccount'], "
          + "p-dialog input[formcontrolname='phoneNo']")).isEmpty();
    }

    /** Robust modal close — try Cancel/Close button, then header X, then Escape. */
    private void closeEditContactModalIfOpen() {
        if (!isEditContactModalOpen()) return;
        try {
            // 1. Cancel / Close button
            List<WebElement> cancels = driver.findElements(By.xpath(
                "//p-dialog//button[normalize-space()='Cancel' "
              + "or normalize-space()='Close']"));
            if (!cancels.isEmpty()) {
                WaitUtils.scrollAndClick(driver, cancels.get(0));
                WaitUtils.hardWait(400);
                if (!isEditContactModalOpen()) return;
            }
            // 2. Header close X
            List<WebElement> headerX = driver.findElements(By.cssSelector(
                "p-dialog .p-dialog-header button[aria-label='Close'], "
              + "p-dialog .p-dialog-close-button, "
              + "p-dialog button.p-dialog-header-icon"));
            if (!headerX.isEmpty()) {
                WaitUtils.scrollAndClick(driver, headerX.get(0));
                WaitUtils.hardWait(400);
                if (!isEditContactModalOpen()) return;
            }
            // 3. Escape key
            new org.openqa.selenium.interactions.Actions(driver)
                .sendKeys(Keys.ESCAPE).perform();
            WaitUtils.hardWait(400);
        } catch (Exception ignored) {}
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DATA-DRIVEN: Employee Onboarding (reads from EmployeeData.xlsx)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Supplies positive onboarding rows from the "ValidOnboard" sheet.
     * Columns (in order): firstName, lastName, email, tempPassword, employeeCode,
     *   systemRole, department, designation, location, reportsTo,
     *   employmentType, joiningDate
     */
    @DataProvider(name = "validOnboardData")
    public Object[][] validOnboardData() {
        return ExcelUtils.readDataExcludingHeader(
                AppConstants.EMPLOYEE_DATA_PATH,
                AppConstants.SHEET_VALID_ONBOARD);
    }

    /**
     * Supplies negative onboarding rows from the "InvalidOnboard" sheet.
     * Columns (in order): testCase, firstName, lastName, email, tempPassword,
     *   employeeCode, systemRole, department, designation, location, reportsTo,
     *   employmentType, joiningDate, expectedError
     */
    @DataProvider(name = "invalidOnboardData")
    public Object[][] invalidOnboardData() {
        return ExcelUtils.readDataExcludingHeader(
                AppConstants.EMPLOYEE_DATA_PATH,
                AppConstants.SHEET_INVALID_ONBOARD);
    }

    /**
     * emp_validOnboarding – Valid employee onboarding submits successfully.
     *
     * Success criterion: after clicking Complete Onboarding, the newly created
     * employee MUST appear in the directory when searched by their full name
     * (the FRD path: HR can search the directory and find the new hire).
     * The frontend's success toast is unreliable on this build, so we verify
     * persistence the way a real user would.
     *
     * Flow:
     *   1. Navigate to /employees
     *   2. Click Add Employee → fill all fields → Complete Onboarding
     *   3. Wait for the modal to close (server accepted the record)
     *   4. Navigate back to /employees
     *   5. Search for "<firstName> <lastName>"
     *   6. Assert at least one matching row appears
     */
    @Test(dataProvider = "validOnboardData",
          priority = 20,
          groups = {"regression", "positive"},
          description = "emp_validOnboarding – New employee created via form is searchable in the directory")
    public void emp_validOnboarding(
            String firstName, String lastName, String email,
            String tempPassword, String employeeCode,
            String systemRole, String department, String designation,
            String location, String reportsTo, String employmentType,
            String joiningDate) {

        // Replace the {RUN} placeholder in email + employeeCode with a unique
        // nanosecond suffix per test invocation. Same xlsx → fresh values
        // every run, so re-running this test never collides with previously
        // created employees.
        String runId = uniqueRunId();
        email        = applyRun(email, runId);
        employeeCode = applyRun(employeeCode, runId);

        ExtentManager.getTest().info(
            "[DD-Onboard] " + firstName + " " + lastName + " (" + email
            + ", code=" + employeeCode + ")");

        // ── Step 1: open modal ───────────────────────────────────────────
        driver.get(AppConstants.EMPLOYEE_DIR_URL);
        WaitUtils.waitForAngularLoad(driver);
        directory = new EmployeeDirectoryPage(driver);
        directory.clickAddEmployee();

        Assert.assertTrue(directory.isOnboardModalOpen(),
            "Onboard modal must open before filling data");

        // ── Step 2: fill all fields ──────────────────────────────────────
        EmployeeOnboardPage onboard = new EmployeeOnboardPage(driver);
        onboard.onboardEmployee(
            firstName, lastName, email, tempPassword, employeeCode,
            systemRole, department, designation, location,
            reportsTo, employmentType, joiningDate);

        // ── Step 3: submit ───────────────────────────────────────────────
        onboard.clickCompleteOnboarding();

        // ── Step 4: wait up to 15s for the modal to close ────────────────
        // The onboard handler fires THREE sequential API calls (createUser →
        // createEmployee → optional hireCandidate). On a slow demo server,
        // and especially for the 2nd/3rd row of the data provider (when the
        // server is still committing the previous insert), 8s was not enough.
        // 15s comfortably covers the worst case observed.
        boolean modalClosed = waitForOnboardModalClosed(15000);
        ExtentManager.getTest().info("Onboard modal closed after submit: " + modalClosed);

        // If it failed, print whatever diagnostic the dialog is showing right
        // now so the next failure points at the real cause instead of a bare
        // timeout. This includes: any inline error text, the isSubmitting
        // spinner state, and the form's invalid-controls list.
        if (!modalClosed) {
            String diagnostic = collectOnboardFailureDiagnostic();
            Assert.fail(
                "emp_validOnboarding [" + firstName + " " + lastName + "]: Onboard modal "
              + "did not close within 15s after Complete Onboarding. Email=" + email
              + "\nDiagnostic snapshot:\n" + diagnostic);
        }

        // ── Step 5: verify the new employee appears in the directory ─────
        driver.get(AppConstants.EMPLOYEE_DIR_URL);
        WaitUtils.waitForAngularLoad(driver);
        directory = new EmployeeDirectoryPage(driver);
        String fullName = firstName + " " + lastName;

        // Poll up to 10s for the search to surface at least one matching row.
        // Hits BOTH the name search and the email search before giving up so
        // the assertion doesn't fire before the backend re-list returns.
        int rows = pollForSearchHit(fullName, 10000);
        ExtentManager.getTest().info("Rows matching '" + fullName + "': " + rows);

        if (rows == 0) {
            directory.clearSearch();
            rows = pollForSearchHit(email, 10000);
            ExtentManager.getTest().info("Fallback rows matching email '" + email + "': " + rows);
        }

        if (rows == 0) {
            // Final fallback — search by employeeCode (the most uniquely-shaped key)
            directory.clearSearch();
            rows = pollForSearchHit(employeeCode, 10000);
            ExtentManager.getTest().info("Fallback rows matching code '" + employeeCode + "': " + rows);
        }

        Assert.assertTrue(rows >= 1,
            "emp_validOnboarding [" + firstName + " " + lastName + "]: Onboarding modal "
          + "closed but the new employee was NOT found in the directory after "
          + "polling 10s by name, 10s by email '" + email + "', and 10s by code "
          + "'" + employeeCode + "'. The record did not persist.");
        ExtentManager.getTest().pass("Onboarded + searchable: " + firstName + " " + lastName);
    }

    /** Polls until the Onboard New Employee dialog disappears, or timeout. */
    private boolean waitForOnboardModalClosed(long maxMillis) {
        long deadline = System.currentTimeMillis() + maxMillis;
        while (System.currentTimeMillis() < deadline) {
            boolean visible = !driver.findElements(By.cssSelector(
                "p-dialog .p-dialog input[formcontrolname='firstName']")).isEmpty();
            if (!visible) return true;
            WaitUtils.hardWait(300);
        }
        return false;
    }

    /**
     * Builds a diagnostic snapshot when the onboard modal doesn't close. Used
     * only on failure path to give a real reason instead of a bare timeout.
     */
    private String collectOnboardFailureDiagnostic() {
        StringBuilder sb = new StringBuilder();
        // Submit-button state
        try {
            WebElement btn = driver.findElement(By.xpath(
                "//p-dialog//button[contains(normalize-space(.),'Complete Onboarding') "
              + "or contains(normalize-space(.),'Saving')]"));
            sb.append("  Submit btn text=\"").append(btn.getText().trim())
              .append("\"  disabled=").append(btn.getAttribute("disabled")).append('\n');
        } catch (Exception ignored) { sb.append("  Submit btn: not found\n"); }

        // ng-invalid controls
        try {
            List<WebElement> invalids = driver.findElements(By.cssSelector(
                "p-dialog [formcontrolname].ng-invalid, p-dialog p-select.ng-invalid"));
            if (!invalids.isEmpty()) {
                sb.append("  Invalid controls: ");
                for (WebElement el : invalids) {
                    String name = el.getAttribute("formcontrolname");
                    if (name != null && !name.isEmpty()) sb.append(name).append(", ");
                }
                sb.append('\n');
            }
        } catch (Exception ignored) {}

        // Any visible error / toast text
        try {
            List<WebElement> errors = driver.findElements(By.xpath(
                "//p-dialog//*[contains(@class,'text-red') or contains(@class,'text-rose') "
              + "or contains(@class,'error') or contains(@class,'p-error')]"));
            for (WebElement e : errors) {
                String txt = e.getText().trim();
                if (!txt.isEmpty()) sb.append("  Error text: ").append(txt).append('\n');
            }
        } catch (Exception ignored) {}

        // Any toast (success or error) currently visible
        try {
            List<WebElement> toasts = driver.findElements(By.cssSelector(
                ".p-toast-message, [class*='toast']"));
            for (WebElement t : toasts) {
                String txt = t.getText().trim();
                if (!txt.isEmpty()) sb.append("  Toast: ").append(txt).append('\n');
            }
        } catch (Exception ignored) {}

        if (sb.length() == 0) sb.append("  (no visible error indicators)\n");
        return sb.toString();
    }

    /**
     * Types {@code query} into the directory search box, then polls the
     * results table up to {@code maxMillis} ms for at least one row to appear.
     * Returns the final row count (≥1 on hit, 0 on timeout).
     *
     * Why poll instead of a fixed wait: the directory re-list after an
     * onboard POST can be 2–5s on a cold cache. A flat 1.2s wait was
     * declaring failure before the row showed up.
     */
    private int pollForSearchHit(String query, long maxMillis) {
        directory.searchEmployee(query);
        long deadline = System.currentTimeMillis() + maxMillis;
        int rows = 0;
        while (System.currentTimeMillis() < deadline) {
            rows = directory.getRowCount();
            if (rows >= 1) return rows;
            WaitUtils.hardWait(400);
        }
        return rows;
    }

    /**
     * Generates a per-test-invocation unique token used to substitute the
     * {RUN} placeholder in Excel email + employeeCode cells. Combines nanoTime
     * (high-resolution) with a random 4-digit suffix so even rapid-fire data
     * provider iterations collide with negligible probability.
     */
    private static String uniqueRunId() {
        return Long.toString(System.nanoTime(), 36)
             + "-" + (int)(Math.random() * 9000 + 1000);
    }

    /**
     * Replaces the literal "{RUN}" token in the given cell value with the
     * supplied unique runId. Returns the input unchanged if no placeholder.
     */
    private static String applyRun(String cellValue, String runId) {
        if (cellValue == null) return null;
        return cellValue.replace("{RUN}", runId);
    }

    /**
     * emp_invalidOnboarding – Invalid onboarding data shows appropriate validation errors.
     *
     * Flow: navigate to /employees → click Add Employee → fill with bad data
     * from the Excel row → click Complete Onboarding → assert validation error
     * OR assert modal did NOT close (form blocked submission).
     */
    @Test(dataProvider = "invalidOnboardData",
          priority = 21,
          groups = {"regression", "negative"},
          description = "emp_invalidOnboarding – Onboarding form rejects bad field values")
    public void emp_invalidOnboarding(
            String testCase,
            String firstName, String lastName, String email,
            String tempPassword, String employeeCode,
            String systemRole, String department, String designation,
            String location, String reportsTo, String employmentType,
            String joiningDate, String expectedError) {

        // Substitute {RUN} placeholder so re-runs of this test don't collide
        // on the server-side unique constraint (email / employeeCode). Even
        // negative tests need fresh values — otherwise the rejection might
        // be "duplicate email" instead of the intended validation rule.
        String runId = uniqueRunId();
        email        = applyRun(email, runId);
        employeeCode = applyRun(employeeCode, runId);

        ExtentManager.getTest().info(
            "[DD-Onboard-Invalid] " + testCase + " | Expected: " + expectedError
            + " | email=" + email + " | code=" + employeeCode);

        // Open modal
        driver.get(AppConstants.EMPLOYEE_DIR_URL);
        WaitUtils.waitForAngularLoad(driver);
        directory = new EmployeeDirectoryPage(driver);
        directory.clickAddEmployee();

        Assert.assertTrue(directory.isOnboardModalOpen(),
            "Onboard modal must open before filling invalid data");

        // Fill form with bad data
        EmployeeOnboardPage onboard = new EmployeeOnboardPage(driver);
        onboard.onboardEmployee(
            firstName, lastName, email, tempPassword, employeeCode,
            systemRole, department, designation, location,
            reportsTo, employmentType, joiningDate);

        // Try to submit
        onboard.clickCompleteOnboarding();
        WaitUtils.hardWait(800);

        // Either a validation error must be visible OR the form must not have closed
        boolean validationError = onboard.isValidationErrorVisible();
        boolean modalStillOpen  = onboard.isModalOpen();
        boolean rejected        = validationError || modalStillOpen;

        ExtentManager.getTest().info(
            "validationError=" + validationError + " | modalStillOpen=" + modalStillOpen);
        Assert.assertTrue(rejected,
            "emp_invalidOnboarding [" + testCase + "]: Invalid data should be rejected. "
            + "Expected error: '" + expectedError + "' — but form appeared to accept the data.");
        ExtentManager.getTest().pass("Invalid onboarding rejected [" + testCase + "]");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void openEditContactModalFromFirstProfile() {
        // Open the first employee's profile via the eye icon, then click the
        // "Edit Contact Info" pencil to open the Edit Contact & Info modal.
        // The eye icon per Rivio_Angular-main employee-directory.html line 101 is
        // a <button> with a pi-eye <i> inside it.
        List<WebElement> eyes = driver.findElements(By.xpath(
            "//p-table//tbody/tr[1]//button[.//i[contains(@class,'pi-eye')]]"));
        if (eyes.isEmpty()) {
            eyes = driver.findElements(By.xpath(
                "//p-table//button[.//i[contains(@class,'pi-eye')]]"));
        }
        if (!eyes.isEmpty()) {
            WaitUtils.scrollAndClick(driver, eyes.get(0));
        } else {
            // Last resort: jump to an arbitrary profile id
            driver.get(AppConstants.EMPLOYEE_DIR_URL + "/1");
        }
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.waitForUrlToBeStable(driver);

        // Click the pencil button titled "Edit Contact Info"
        try {
            WebElement pencil = WaitUtils.waitForClickability(driver, By.xpath(
                "//button[@title='Edit Contact Info' or contains(@title,'Edit Contact')]"));
            WaitUtils.scrollAndClick(driver, pencil);
        } catch (Exception e) {
            // Fallback: first pencil icon on the page
            try {
                WebElement anyPencil = driver.findElement(By.cssSelector(
                    "button .pi-pencil, button[class*='pencil']"));
                WaitUtils.scrollAndClick(driver, anyPencil);
            } catch (Exception ignored) {}
        }

        // Wait for the Edit Contact & Info dialog
        WaitUtils.waitForPresence(driver, By.cssSelector(
            "p-dialog input[formcontrolname='bankAccount'], "
          + "p-dialog input[formcontrolname='phoneNo']"), 10);
    }

    private WebElement findInputByFormControlName(String name) {
        List<WebElement> els = driver.findElements(By.cssSelector(
            "p-dialog input[formcontrolname='" + name + "']"));
        return els.isEmpty() ? null : els.get(0);
    }

    private boolean isMarkedInvalid(WebElement input) {
        try {
            String cls  = input.getAttribute("class");
            String aria = input.getAttribute("aria-invalid");
            if (cls != null && (cls.contains("ng-invalid") || cls.contains("p-invalid")
                             || cls.contains("is-invalid") || cls.contains("border-rose")
                             || cls.contains("!border-rose"))) return true;
            if ("true".equalsIgnoreCase(aria)) return true;
        } catch (Exception ignored) {}
        return false;
    }

    private boolean isSaveDisabled() {
        try {
            WebElement save = driver.findElement(By.xpath(
                "//p-dialog//button[contains(.,'Save Changes') or contains(.,'Save')]"));
            String dis = save.getAttribute("disabled");
            return dis != null && !dis.isEmpty();
        } catch (Exception e) { return false; }
    }

    private boolean hasNearbyError(String... keywords) {
        for (String kw : keywords) {
            if (!driver.findElements(By.xpath(
                "//p-dialog//*[contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ',"
              + "'abcdefghijklmnopqrstuvwxyz'),'" + kw.toLowerCase() + "') "
              + "and (contains(.,'invalid') or contains(.,'numeric') "
              + "or contains(.,'digits') or contains(.,'must') "
              + "or contains(.,'valid'))]")).isEmpty()) return true;
        }
        return false;
    }
}
