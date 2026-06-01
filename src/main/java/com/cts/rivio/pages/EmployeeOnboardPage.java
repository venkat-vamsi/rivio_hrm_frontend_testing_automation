package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * EmployeeOnboardPage – wraps the "Onboard New Employee" p-dialog.
 *
 * Angular form structure (employee-onboard.component.html):
 *   Section 1 – Account Credentials : email, tempPassword, systemRole (p-select)
 *   Section 2 – Personal Identity   : firstName, lastName, employeeCode
 *   Section 3 – Org Role            : departmentId (p-select), designationId (p-select),
 *                                     locationId (p-select), managerId (p-select),
 *                                     employmentType (p-select), joiningDate (p-datepicker)
 *
 * Sentinel values for dropdown/date parameters (passed via Excel):
 *   "AUTO"      → selectOrFirst() picks the first non-placeholder option
 *   "today"     → resolves to today's date in dd/MM/yyyy format
 *   "yesterday" → resolves to yesterday
 */
public class EmployeeOnboardPage {

    private final WebDriver driver;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public EmployeeOnboardPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── State queries ─────────────────────────────────────────────────────────

    public boolean isModalOpen() {
        return !driver.findElements(By.cssSelector("p-dialog .p-dialog")).isEmpty();
    }

    public boolean hasThreeSections() {
        long sections = driver.findElements(By.xpath(
            "//p-dialog//h3 | //p-dialog//div[contains(@class,'section-title')]")).size();
        return sections >= 3;
    }

    // ── Section 1: Account Credentials ───────────────────────────────────────

    public void fillEmail(String email) {
        // Real Angular control name is "email" (placeholder is "name@rivio.com",
        // NOT containing "Email" — the old typeByPlaceholder("Email", …) was a no-op).
        typeByFormControl("email", email);
    }

    public void fillPassword(String password) {
        // Real Angular control name is "password" (not "tempPassword"). The
        // input lives inside <p-password> so an extra descendant selector is
        // needed for the JS-set fallback.
        try {
            WebElement el = WaitUtils.waitForVisibility(driver, By.cssSelector(
                "p-dialog p-password input[type='password'], "
                + "p-dialog p-password input, "
                + "p-dialog input[formcontrolname='password'], "
                + "p-dialog input[type='password']"));
            WaitUtils.jsSetValue(driver, el, password);
        } catch (Exception e) {
            typeByFormControl("password", password);
        }
    }

    public void selectSystemRole(String role) {
        // Real Angular control name is "roleId" (NOT "systemRole" — the old
        // selector matched nothing, so the role dropdown was never touched
        // and roleId stayed null → Complete Onboarding button stayed disabled).
        selectPrimeNgInDialog("roleId", role);
    }

    // ── Section 2: Personal Identity ─────────────────────────────────────────

    public void fillFirstName(String n)   { typeByFormControl("firstName", n); }
    public void fillLastName(String n)    { typeByFormControl("lastName", n); }
    public void fillEmployeeCode(String c){ typeByFormControl("employeeCode", c); }

    // ── Section 3: Org Role ───────────────────────────────────────────────────

    public void selectDepartment(String dept) {
        selectPrimeNgInDialog("departmentId", dept);
    }

    public void selectDesignation(String desig) {
        selectPrimeNgInDialog("designationId", desig);
    }

    public void selectLocation(String loc) {
        selectPrimeNgInDialog("locationId", loc);
    }

    public void selectReportsTo(String manager) {
        // Real Angular control name is "reportsToProfileId" (the legacy
        // "managerId"/"reportsTo" guesses both missed). reportsTo is OPTIONAL
        // anyway and is only called when the Excel cell is non-blank.
        trySelectPrimeNgInDialog("reportsToProfileId", manager);
    }

    public void selectEmploymentType(String type) {
        selectPrimeNgInDialog("employmentType", type);
    }

    public void setJoiningDate(String dateOrKeyword) {
        // The Angular form already initialises joiningDate to `new Date()`
        // (today). For "today" / "" we simply leave the pre-filled value
        // alone — pushing a string into PrimeNG's p-datepicker via input
        // event leaves the control with an unparseable Date → form invalid
        // → Complete Onboarding stays disabled (silent failure).
        if (dateOrKeyword == null
                || dateOrKeyword.trim().isEmpty()
                || "today".equalsIgnoreCase(dateOrKeyword.trim())) {
            return;
        }
        // For any other literal/relative date, still attempt to set it.
        String resolved = resolveDate(dateOrKeyword);
        try {
            WebElement input = WaitUtils.waitForVisibility(driver, By.cssSelector(
                "p-dialog p-datepicker input, "
                + "p-dialog input[formcontrolname='joiningDate'], "
                + "p-dialog .p-datepicker input"));
            WaitUtils.jsSetValue(driver, input, resolved);
        } catch (Exception ignored) {}
    }

    // ── Composite action ──────────────────────────────────────────────────────

    /**
     * Fills every field in the onboard form from the values supplied.
     * Pass "AUTO" for any dropdown to auto-select the first available option.
     * Pass "today" / "yesterday" for joiningDate.
     *
     * Notes:
     * - reportsTo is OPTIONAL in the Angular form (per FRD §2.3.2); pass an empty
     *   string from Excel to skip it. This avoids the dropdown opening problem
     *   when the Manager list has not yet loaded.
     * - department → designation is a cascade: selecting a department refreshes
     *   the designation list. A short wait between the two lets Angular update
     *   `filteredDesignations` before we open the designation panel.
     */
    public void onboardEmployee(
            String firstName, String lastName, String email,
            String tempPassword, String employeeCode,
            String systemRole, String department, String designation,
            String location, String reportsTo, String employmentType,
            String joiningDate) {

        // Section 1
        fillEmail(email);
        fillPassword(tempPassword);
        if (notEmpty(systemRole)) selectSystemRole(systemRole);

        // Section 2
        fillFirstName(firstName);
        fillLastName(lastName);
        fillEmployeeCode(employeeCode);

        // Section 3 – cascade: department first, then wait for the designation
        // dropdown to become enabled, then pick designation.
        if (notEmpty(department)) {
            selectDepartment(department);
            waitForDesignationDropdownEnabled();      // wait for cascade to populate
        }
        if (notEmpty(designation))    selectDesignation(designation);
        if (notEmpty(location))       selectLocation(location);

        // reportsTo is OPTIONAL — skip silently when blank
        if (notEmpty(reportsTo))      selectReportsTo(reportsTo);

        if (notEmpty(employmentType)) selectEmploymentType(employmentType);
        if (notEmpty(joiningDate))    setJoiningDate(joiningDate);
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    /**
     * Clicks Complete Onboarding ONLY after the button is enabled. If the
     * form is still invalid after 5s of polling, throws with a diagnostic
     * snapshot of which controls are still in error state — that turns
     * "submit silently failed" into a real test failure with context.
     */
    public void clickCompleteOnboarding() {
        By btnLocator = By.xpath(
            "//p-dialog//button[contains(normalize-space(.),'Complete Onboarding')"
          + " or normalize-space()='Submit' or normalize-space()='Save']");

        long deadline = System.currentTimeMillis() + 5000;
        WebElement btn = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                java.util.List<WebElement> btns = driver.findElements(btnLocator);
                if (!btns.isEmpty()) {
                    btn = btns.get(0);
                    String disabled = btn.getAttribute("disabled");
                    if (disabled == null || disabled.isEmpty() || "false".equalsIgnoreCase(disabled)) {
                        break;
                    }
                }
            } catch (Exception ignored) {}
            WaitUtils.hardWait(250);
            btn = null;
        }

        if (btn == null) {
            throw new RuntimeException(
                "Complete Onboarding button never became enabled within 5s. "
              + "Form is still invalid. Invalid controls: " + listInvalidControls());
        }
        WaitUtils.scrollAndClick(driver, btn);
    }

    /** Returns a comma-separated list of formcontrolname values whose
     *  input/p-select is currently in ng-invalid state. Diagnostic only. */
    private String listInvalidControls() {
        StringBuilder sb = new StringBuilder();
        try {
            java.util.List<WebElement> invalids = driver.findElements(By.cssSelector(
                "p-dialog [formcontrolname].ng-invalid, "
              + "p-dialog .ng-invalid[formcontrolname], "
              + "p-dialog p-select.ng-invalid"));
            for (WebElement el : invalids) {
                String name = el.getAttribute("formcontrolname");
                if (name != null && !name.isEmpty()) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(name);
                }
            }
        } catch (Exception ignored) {}
        return sb.length() == 0 ? "<none detected>" : sb.toString();
    }

    // ── Result verification ───────────────────────────────────────────────────

    /**
     * Returns true if a success toast / confirmation is shown after submit.
     * PrimeNG p-toast uses role="alert" or class "p-toast-message".
     */
    public boolean isSuccessToastVisible() {
        WaitUtils.hardWait(1200);
        return !driver.findElements(By.cssSelector(
            ".p-toast-message-success, "
            + "[class*='toast'][class*='success'], "
            + ".p-toast .p-toast-icon-success")).isEmpty()
            || !driver.findElements(By.xpath(
            "//*[contains(@class,'toast') and ("
            + "contains(.,'success') or contains(.,'onboard') "
            + "or contains(.,'created') or contains(.,'added'))]")).isEmpty();
    }

    /**
     * Returns true if an inline validation error message is visible inside the dialog.
     */
    public boolean isValidationErrorVisible() {
        return !driver.findElements(By.cssSelector(
            "p-dialog .p-error, p-dialog small.p-error, "
            + "p-dialog [class*='error'], p-dialog .ng-invalid")).isEmpty()
            || !driver.findElements(By.xpath(
            "//p-dialog//*[contains(@class,'text-red') or contains(@class,'text-danger')"
            + " or contains(@class,'error') or contains(@class,'invalid')]")).isEmpty();
    }

    // Legacy compat
    public boolean isFormValid() { return !isValidationErrorVisible(); }

    // ── Private helpers ───────────────────────────────────────────────────────

    // Both helpers use jsSetValue so the entire string lands in one event
    // (10x faster than per-character sendKeys) AND PrimeNG / Angular still
    // see the change because we dispatch input+change events. Per-character
    // sendKeys was making the test crawl, and clear() can race with Angular's
    // form state-management on slow renders.
    private void typeByPlaceholder(String placeholder, String text) {
        try {
            WebElement el = WaitUtils.waitForVisibility(driver,
                By.cssSelector("p-dialog input[placeholder*='" + placeholder + "' i]"));
            WaitUtils.jsSetValue(driver, el, text);
        } catch (Exception ignored) {}
    }

    private void typeByFormControl(String fc, String text) {
        try {
            WebElement el = WaitUtils.waitForVisibility(driver,
                By.cssSelector("p-dialog input[formcontrolname='" + fc + "']"));
            WaitUtils.jsSetValue(driver, el, text);
        } catch (Exception ignored) {}
    }

    /**
     * Clicks the p-select whose [formcontrolname] matches {@code controlName} inside
     * the p-dialog, then picks the option matching {@code value}.
     * If value == "AUTO", the first non-placeholder item is selected.
     */
    private void selectPrimeNgInDialog(String controlName, String value) {
        trySelectPrimeNgInDialog(controlName, value);
    }

    private boolean trySelectPrimeNgInDialog(String controlName, String value) {
        try {
            if ("AUTO".equalsIgnoreCase(value) || value == null || value.isEmpty()) {
                WaitUtils.selectPrimeNgOption(driver,
                    By.cssSelector("p-dialog p-select[formcontrolname='" + controlName + "']"),
                    null);
            } else {
                WaitUtils.selectPrimeNgOption(driver,
                    By.cssSelector("p-dialog p-select[formcontrolname='" + controlName + "']"),
                    value);
            }
            return true;
        } catch (Exception e) { return false; }
    }

    /**
     * Converts "today", "yesterday", "today+N", "today-N" keywords to a date string.
     * Any other value is returned as-is (assume it's already a formatted date).
     */
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
        return keyword; // already a literal date
    }

    private boolean notEmpty(String s) { return s != null && !s.isEmpty(); }

    /**
     * Waits up to 6s for the designation p-select inside the onboard dialog
     * to become enabled (i.e. the cascade has refreshed filteredDesignations
     * after a department was chosen). Required before opening the designation
     * panel — otherwise the click hits a disabled control and nothing happens.
     */
    private void waitForDesignationDropdownEnabled() {
        long deadline = System.currentTimeMillis() + 6000;
        while (System.currentTimeMillis() < deadline) {
            try {
                java.util.List<WebElement> sels = driver.findElements(
                    By.cssSelector("p-dialog p-select[formcontrolname='designationId']"));
                if (!sels.isEmpty()) {
                    WebElement sel = sels.get(0);
                    String aria = sel.getAttribute("aria-disabled");
                    String cls  = sel.getAttribute("class");
                    boolean disabled =
                        "true".equalsIgnoreCase(aria)
                        || (cls != null && cls.contains("p-disabled"));
                    if (!disabled) return;
                }
            } catch (Exception ignored) {}
            WaitUtils.hardWait(250);
        }
    }
}
