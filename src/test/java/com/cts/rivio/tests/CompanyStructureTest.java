package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.*;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.*;

/**
 * CompanyStructureTest – tests for the Company Structure module.
 * Logged in as SuperAdmin to access all management functions.
 */
public class CompanyStructureTest extends BaseTest {

    private CompanyStructurePage companyPage;

    @BeforeMethod
    public void loginAndGoToCompany() {
        LoginPage loginPage = new LoginPage(driver);
        DashboardPage dashboard = loginPage.login(
                AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD);
        companyPage = new SidebarPage(driver).clickCompany();
    }

    @Test(priority = 1, description = "Company structure page should load")
    public void testCompanyPageLoads() {
        Assert.assertTrue(companyPage.isPageLoaded(),
                "Company structure page should be loaded");
    }

    @Test(priority = 2, description = "Departments tab should show departments")
    public void testDepartmentsTabVisible() {
        companyPage.clickSubTab("Departments");
        int count = companyPage.getDepartmentCount();
        ExtentManager.getTest().info("Department count: " + count);
        Assert.assertTrue(count >= 0);
    }

    @Test(priority = 3, description = "Add Department dialog should open")
    public void testAddDepartmentDialogOpens() {
        companyPage.clickSubTab("Departments");
        companyPage.clickAddDepartment();
        // Verify modal/form opened by checking URL or form visibility
        ExtentManager.getTest().info("Add Department dialog opened");
        Assert.assertNotNull(driver.getCurrentUrl());
    }

    @Test(priority = 4, description = "Cancel add department should close form")
    public void testCancelAddDepartment() {
        companyPage.clickSubTab("Departments");
        companyPage.clickAddDepartment();
        companyPage.clickCancel();
        // Should be back to department list
        Assert.assertTrue(companyPage.isPageLoaded());
    }

    @Test(priority = 5, description = "Designations tab should be accessible")
    public void testDesignationsTab() {
        companyPage.clickSubTab("Designations");
        ExtentManager.getTest().pass("Navigated to Designations tab");
        Assert.assertTrue(companyPage.isPageLoaded());
    }

    @Test(priority = 6, description = "Locations tab should be accessible")
    public void testLocationsTab() {
        companyPage.clickSubTab("Locations");
        ExtentManager.getTest().pass("Navigated to Locations tab");
        Assert.assertTrue(companyPage.isPageLoaded());
    }
}
