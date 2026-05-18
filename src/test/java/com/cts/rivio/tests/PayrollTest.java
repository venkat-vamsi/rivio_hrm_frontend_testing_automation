package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.DashboardPage;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.pages.PayrollDashboardPage;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * PayrollTest – PAY-S-01..PAY-S-02 admin-side.
 *
 *   RV_PAY_001 – Employee Salaries tab + Add Component button
 *   RV_PAY_002 – Initialize pay cycle (creates DRAFT; RV-BUG-006 says it shows FINALIZED)
 */
public class PayrollTest extends BaseTest {

    private PayrollDashboardPage payroll;

    @BeforeMethod
    public void loginAsAdminAndOpenPayroll() {
        DashboardPage dash = new LoginPage(driver)
                .login(AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD);
        payroll = dash.goToPayroll();
    }

    @Test(priority = 1, description = "RV_PAY_001 – Payroll page loads with Employee Salaries tab")
    public void RV_PAY_001_payrollPageLoads() {
        Assert.assertTrue(payroll.isPageLoaded(),
                "Payroll Management page should be loaded");
        payroll.openEmployeeSalariesTab();
        Assert.assertTrue(payroll.isAddComponentDisabledForNoEmployee(),
                "'Add Component' button should be disabled when no employee is selected");
        ExtentManager.getTest().pass("Employee Salaries tab renders with disabled Add Component");
    }

    @Test(priority = 2, description = "RV_PAY_002 – Pay Cycles tab exposes 'Initialize Pay Cycle' action")
    public void RV_PAY_002_payCyclesTab() {
        payroll.openPayCyclesTab();
        Assert.assertTrue(payroll.isInitializePayCycleVisible(),
                "'Initialize Pay Cycle' button must be present in the Pay Cycles tab");
        ExtentManager.getTest().pass("Pay Cycles tab renders");
    }

}
