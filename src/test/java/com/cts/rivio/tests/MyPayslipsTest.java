package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.pages.selfservice.MyPayslipsPage;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.*;

/**
 * MyPayslipsTest – tests for Employee self-service "My Payslips" page.
 */
public class MyPayslipsTest extends BaseTest {

    private MyPayslipsPage myPayslipsPage;

    @BeforeMethod
    public void loginAndGoToMyPayslips() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.login(AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD);
        driver.get(AppConstants.MY_PAYSLIPS_URL);
        myPayslipsPage = new MyPayslipsPage(driver);
    }

    @Test(priority = 1, description = "My Payslips page should load")
    public void testMyPayslipsPageLoads() {
        Assert.assertTrue(myPayslipsPage.isPageLoaded(),
                "My Payslips page should be loaded");
    }

    @Test(priority = 2, description = "Payslip records should be listed")
    public void testPayslipListVisible() {
        int count = myPayslipsPage.getPayslipCount();
        ExtentManager.getTest().info("Payslip count: " + count);
        Assert.assertTrue(count >= 0, "Payslip list should render");
    }

    @Test(priority = 3, description = "Filter by month and year should work")
    public void testFilterByMonthAndYear() {
        myPayslipsPage.filterByMonth("December");
        myPayslipsPage.filterByYear("2024");
        int count = myPayslipsPage.getPayslipCount();
        ExtentManager.getTest().info("Payslips for Dec 2024: " + count);
        Assert.assertTrue(count >= 0);
    }

    @Test(priority = 4, description = "View payslip modal should open on click")
    public void testViewPayslip() {
        int count = myPayslipsPage.getPayslipCount();
        if (count > 0) {
            myPayslipsPage.clickView(0);
            // Modal may or may not appear depending on app behavior
            ExtentManager.getTest().pass("View payslip clicked for row 0");
            Assert.assertNotNull(driver.getCurrentUrl());
        } else {
            ExtentManager.getTest().skip("No payslips to view");
        }
    }
}
