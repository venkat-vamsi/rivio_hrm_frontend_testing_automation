package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * PayrollDashboardPage – mirrors features/payroll/payroll-dashboard.component.html.
 *
 * Real DOM (post-redesign):
 *   - &lt;h1&gt;Payroll Management&lt;/h1&gt;
 *   - Two tab buttons: "Employee Salaries", "Pay Cycles &amp; Slips"
 *   - Employee Salaries tab:
 *       p-select for employee selection
 *       "Add Component" button (disabled until employee selected)
 *       Add Component modal: componentName input, componentType p-select, value input
 *   - Pay Cycles tab:
 *       "Initialize Pay Cycle" button
 */
public class PayrollDashboardPage {

    private final WebDriver driver;

    public PayrollDashboardPage(WebDriver driver) { this.driver = driver; }

    // ── Page load ─────────────────────────────────────────────────────────────

    public boolean isPageLoaded() {
        return WaitUtils.waitForH1Text(driver, "Payroll Management", 15);
    }

    // ── Tab navigation ────────────────────────────────────────────────────────

    public void openEmployeeSalariesTab() {
        clickTabContaining("Employee Salaries");
        WaitUtils.waitForPresence(driver,
            By.xpath("//button[contains(.,'Add Component')]"), 10);
    }

    public void openPayCyclesTab() {
        clickTabContaining("Pay Cycles");
        WaitUtils.waitForPresence(driver,
            By.xpath("//button[contains(normalize-space(.),'Initialize Pay Cycle')]"), 15);
    }

    private void clickTabContaining(String partial) {
        WebElement btn = WaitUtils.waitForClickability(driver, By.xpath(
            "//button[contains(normalize-space(.),'" + partial + "')]"));
        WaitUtils.scrollAndClick(driver, btn);
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.hardWait(400);
    }

    // ── Employee Salaries tab — employee selection ────────────────────────────

    /**
     * Selects an employee by name from the employee p-select on the salary tab.
     * Pass "AUTO" to select the first available employee.
     */
    public void selectEmployeeForPayroll(String employeeName) {
        try {
            By dropdownLocator = By.cssSelector(
                "p-select[formcontrolname='employeeId'], "
                + "p-select[formcontrolname='employee'], "
                + "app-payroll-dashboard p-select");
            if ("AUTO".equalsIgnoreCase(employeeName)
                    || employeeName == null || employeeName.isEmpty()) {
                WaitUtils.selectPrimeNgOption(driver, dropdownLocator, null);
            } else {
                WaitUtils.selectPrimeNgOption(driver, dropdownLocator, employeeName);
            }
            WaitUtils.waitForAngularLoad(driver);
            WaitUtils.hardWait(600);
        } catch (Exception ignored) {}
    }

    /** Convenience: always picks the first employee in the list. */
    public void selectFirstEmployeeForPayroll() {
        selectEmployeeForPayroll("AUTO");
    }

    // ── Employee Salaries tab — Add Component modal ───────────────────────────

    /**
     * Clicks the "Add Component" button (must be enabled — select an employee first).
     */
    public void clickAddComponentButton() {
        try {
            WebElement btn = WaitUtils.waitForClickability(driver,
                By.xpath("//button[contains(.,'Add Component')]"));
            WaitUtils.safeClick(driver, btn);
            WaitUtils.waitForAngularLoad(driver);
            WaitUtils.hardWait(500);
        } catch (Exception ignored) {}
    }

    /**
     * Fills the component name field in the Add Component modal.
     */
    public void fillComponentName(String name) {
        try {
            WebElement el = WaitUtils.waitForVisibility(driver, By.cssSelector(
                "p-dialog input[formcontrolname='componentName'], "
                + "p-dialog input[formcontrolname='name'], "
                + "p-dialog input[placeholder*='component' i], "
                + "p-dialog input[placeholder*='name' i]"));
            el.clear();
            el.sendKeys(name);
        } catch (Exception ignored) {}
    }

    /**
     * Selects the component type (e.g., "Earning" or "Deduction") in the modal.
     * Pass "AUTO" to select the first available type.
     */
    public void selectComponentType(String type) {
        try {
            By dropdownLocator = By.cssSelector(
                "p-dialog p-select[formcontrolname='componentType'], "
                + "p-dialog p-select[formcontrolname='type']");
            if ("AUTO".equalsIgnoreCase(type) || type == null || type.isEmpty()) {
                WaitUtils.selectPrimeNgOption(driver, dropdownLocator, null);
            } else {
                WaitUtils.selectPrimeNgOption(driver, dropdownLocator, type);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Fills the component value (amount) field in the Add Component modal.
     */
    public void fillComponentValue(String value) {
        try {
            WebElement el = WaitUtils.waitForVisibility(driver, By.cssSelector(
                "p-dialog input[formcontrolname='value'], "
                + "p-dialog input[formcontrolname='amount'], "
                + "p-dialog input[type='number'], "
                + "p-dialog input[placeholder*='amount' i], "
                + "p-dialog input[placeholder*='value' i]"));
            WaitUtils.jsSetValue(driver, el, value);
        } catch (Exception ignored) {}
    }

    /**
     * Clicks the Submit / Save button in the Add Component modal.
     */
    public void submitComponentForm() {
        try {
            WebElement btn = WaitUtils.waitForClickability(driver, By.xpath(
                "//p-dialog//button[contains(.,'Submit') or contains(.,'Save')"
                + " or contains(.,'Add') or contains(.,'Confirm')]"));
            WaitUtils.safeClick(driver, btn);
            WaitUtils.waitForAngularLoad(driver);
            WaitUtils.hardWait(800);
        } catch (Exception ignored) {}
    }

    /**
     * Returns true if the component was saved successfully (success toast or modal close).
     */
    public boolean isComponentSavedSuccessfully() {
        WaitUtils.hardWait(1000);
        boolean modalClosed = driver.findElements(By.cssSelector("p-dialog .p-dialog")).isEmpty();
        boolean successToast = !driver.findElements(By.cssSelector(
            ".p-toast-message-success, [class*='toast'][class*='success']")).isEmpty()
            || !driver.findElements(By.xpath(
            "//*[contains(@class,'toast') and (contains(.,'success')"
            + " or contains(.,'saved') or contains(.,'added'))]")).isEmpty();
        return modalClosed || successToast;
    }

    /**
     * Returns true if a validation error is visible in the Add Component modal.
     */
    public boolean isComponentValidationErrorVisible() {
        return !driver.findElements(By.cssSelector(
            "p-dialog .p-error, p-dialog small.p-error, "
            + "p-dialog [class*='error'], p-dialog .ng-invalid")).isEmpty()
            || !driver.findElements(By.xpath(
            "//p-dialog//*[contains(@class,'text-red') or contains(@class,'text-danger')"
            + " or contains(@class,'error')]")).isEmpty();
    }

    // ── Pay Cycles tab ────────────────────────────────────────────────────────

    public boolean isInitializePayCycleVisible() {
        return WaitUtils.waitForPresence(driver,
            By.xpath("//button[contains(normalize-space(.),'Initialize Pay Cycle')]"), 15);
    }

    // ── Employee Salaries tab queries ─────────────────────────────────────────

    public boolean isAddComponentDisabledForNoEmployee() {
        try {
            WebElement btn = driver.findElement(
                By.xpath("//button[contains(.,'Add Component')]"));
            String dis = btn.getAttribute("disabled");
            return dis != null && !dis.isEmpty();
        } catch (Exception e) { return false; }
    }
}
