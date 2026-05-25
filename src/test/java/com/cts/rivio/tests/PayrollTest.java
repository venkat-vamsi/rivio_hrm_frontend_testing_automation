package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.PayrollDashboardPage;
import com.cts.rivio.utils.ExcelUtils;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * PayrollTest – PAY-S-01..PAY-S-02 admin-side.
 *
 *   RV_PAY_001 – Employee Salaries tab + Add Component button
 *   RV_PAY_002 – Initialize pay cycle (creates DRAFT; RV-BUG-006 says it shows FINALIZED)
 */
public class PayrollTest extends BaseTest {

    @Override protected String getRole() { return ROLE_ADMIN; }

    private PayrollDashboardPage payroll;

    @BeforeMethod(alwaysRun = true)
    public void openPayroll() {
        // Bucket session is already logged in as Admin via BaseTest @BeforeClass.
        driver.get(AppConstants.PAYROLL_URL);
        com.cts.rivio.utils.WaitUtils.waitForAngularLoad(driver);
        payroll = new PayrollDashboardPage(driver);
    }

    @Test(priority = 1, groups = {"smoke", "regression"}, description = "RV_PAY_001 – Payroll page loads with Employee Salaries tab")
    public void RV_PAY_001_payrollPageLoads() {
        Assert.assertTrue(payroll.isPageLoaded(),
                "Payroll Management page should be loaded");
        payroll.openEmployeeSalariesTab();
        Assert.assertTrue(payroll.isAddComponentDisabledForNoEmployee(),
                "'Add Component' button should be disabled when no employee is selected");
        ExtentManager.getTest().pass("Employee Salaries tab renders with disabled Add Component");
    }

    @Test(priority = 2, groups = {"regression"}, description = "RV_PAY_002 – Pay Cycles tab exposes 'Initialize Pay Cycle' action")
    public void RV_PAY_002_payCyclesTab() {
        payroll.openPayCyclesTab();
        Assert.assertTrue(payroll.isInitializePayCycleVisible(),
                "'Initialize Pay Cycle' button must be present in the Pay Cycles tab");
        ExtentManager.getTest().pass("Pay Cycles tab renders");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DATA-DRIVEN: Salary Components (reads from PayrollData.xlsx)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Supplies positive salary component rows from the "ValidSalaryComponent" sheet.
     * Columns (in order): employee, componentName, componentType, value
     * "AUTO" employee → first available employee in the payroll dropdown.
     */
    @DataProvider(name = "validSalaryData")
    public Object[][] validSalaryData() {
        return ExcelUtils.readDataExcludingHeader(
                AppConstants.PAYROLL_DATA_PATH,
                AppConstants.SHEET_VALID_SALARY);
    }

    /**
     * Supplies negative salary component rows from the "InvalidSalaryComponent" sheet.
     * Columns (in order): testCase, employee, componentName, componentType, value, expectedError
     */
    @DataProvider(name = "invalidSalaryData")
    public Object[][] invalidSalaryData() {
        return ExcelUtils.readDataExcludingHeader(
                AppConstants.PAYROLL_DATA_PATH,
                AppConstants.SHEET_INVALID_SALARY);
    }

    /**
     * RV_PAY_DD_001 – Valid salary component is saved successfully.
     *
     * Flow: open Payroll → Employee Salaries tab → select employee
     * → click Add Component → fill componentName, type, value → submit → assert success.
     */
    @Test(dataProvider = "validSalaryData",
          priority = 20,
          groups = {"regression"},
          description = "RV_PAY_DD_001 – Valid salary component is saved successfully")
    public void RV_PAY_DD_001_validSalaryComponent(
            String employee, String componentName, String componentType, String value) {

        ExtentManager.getTest().info(
            "[DD-Payroll] employee=" + employee
            + " component=" + componentName + " type=" + componentType + " value=" + value);

        payroll.openEmployeeSalariesTab();
        payroll.selectEmployeeForPayroll(employee);

        // After selecting employee the Add Component button becomes enabled
        Assert.assertFalse(payroll.isAddComponentDisabledForNoEmployee(),
            "Add Component should be enabled after selecting an employee");

        payroll.clickAddComponentButton();
        payroll.fillComponentName(componentName);
        payroll.selectComponentType(componentType);
        payroll.fillComponentValue(value);
        payroll.submitComponentForm();

        boolean success = payroll.isComponentSavedSuccessfully();
        ExtentManager.getTest().info("Component saved: " + success);
        Assert.assertTrue(success,
            "RV_PAY_DD_001: Salary component '" + componentName
            + "' (type=" + componentType + ", value=" + value
            + ") should be saved. No success indicator appeared.");
        ExtentManager.getTest().pass("Salary component saved: " + componentName);
    }

    /**
     * RV_PAY_DD_002 – Invalid salary component data is rejected with validation error.
     *
     * Flow: open Payroll → Employee Salaries tab → select employee
     * → click Add Component → fill with bad data → submit → assert rejection.
     */
    @Test(dataProvider = "invalidSalaryData",
          priority = 21,
          groups = {"regression"},
          description = "RV_PAY_DD_002 – Invalid salary component data is rejected")
    public void RV_PAY_DD_002_invalidSalaryComponent(
            String testCase, String employee, String componentName,
            String componentType, String value, String expectedError) {

        ExtentManager.getTest().info(
            "[DD-Payroll-Invalid] " + testCase + " | Expected: " + expectedError);

        payroll.openEmployeeSalariesTab();
        payroll.selectEmployeeForPayroll(employee);

        payroll.clickAddComponentButton();
        payroll.fillComponentName(componentName);
        if (!componentType.isEmpty()) payroll.selectComponentType(componentType);
        payroll.fillComponentValue(value);
        payroll.submitComponentForm();

        boolean validationError = payroll.isComponentValidationErrorVisible();
        boolean modalStillOpen  = !driver.findElements(
            org.openqa.selenium.By.cssSelector("p-dialog .p-dialog")).isEmpty();
        boolean rejected        = validationError || modalStillOpen;

        ExtentManager.getTest().info(
            "validationError=" + validationError + " | modalStillOpen=" + modalStillOpen);
        Assert.assertTrue(rejected,
            "RV_PAY_DD_002 [" + testCase + "]: Invalid salary component should be rejected. "
            + "Expected: '" + expectedError + "'");
        ExtentManager.getTest().pass("Invalid salary component rejected [" + testCase + "]");
    }
}
