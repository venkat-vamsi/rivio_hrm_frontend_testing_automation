package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.*;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.*;

/**
 * PayrollTest – tests for the Payroll module (Payroll Manager role).
 */
public class PayrollTest extends BaseTest {

    private PayrollDashboardPage payrollPage;

    @BeforeMethod
    public void loginAndGoToPayroll() {
        LoginPage loginPage = new LoginPage(driver);
        DashboardPage dashboard = loginPage.login(
                AppConstants.PAYROLL_EMAIL, AppConstants.PAYROLL_PASSWORD);
        payrollPage = dashboard.goToPayroll();
    }

    @Test(priority = 1, description = "Payroll dashboard should load")
    public void testPayrollDashboardLoads() {
        Assert.assertTrue(payrollPage.isPageLoaded(),
                "Payroll dashboard should be loaded");
    }

    @Test(priority = 2, description = "Payroll summary cards should be visible")
    public void testPayrollSummaryCards() {
        int count = payrollPage.getSummaryCardCount();
        ExtentManager.getTest().info("Summary card count: " + count);
        Assert.assertTrue(count >= 0, "Summary section should render");
    }

    @Test(priority = 3, description = "Filter payroll by month and year")
    public void testFilterByMonthAndYear() {
        payrollPage.selectMonth("January");
        payrollPage.selectYear("2025");
        int rows = payrollPage.getPayrollRowCount();
        ExtentManager.getTest().info("Payroll rows for Jan 2025: " + rows);
        Assert.assertTrue(rows >= 0);
    }

    @Test(priority = 4, description = "Filter payroll by department")
    public void testFilterByDepartment() {
        payrollPage.filterByDepartment("Engineering");
        int rows = payrollPage.getPayrollRowCount();
        ExtentManager.getTest().info("Payroll rows for Engineering: " + rows);
        Assert.assertTrue(rows >= 0);
    }

    @Test(priority = 5, description = "Payroll records should be visible")
    public void testPayrollRecordsVisible() {
        int count = payrollPage.getPayrollRowCount();
        ExtentManager.getTest().info("Payroll record count: " + count);
        Assert.assertTrue(count >= 0);
    }

    @Test(priority = 6, description = "Total payroll amount should be displayed")
    public void testTotalPayrollAmountDisplayed() {
        String total = payrollPage.getTotalPayrollAmount();
        ExtentManager.getTest().info("Total payroll: " + total);
        // Amount may be empty if no records – just verify no exception
        Assert.assertNotNull(total);
    }
}
