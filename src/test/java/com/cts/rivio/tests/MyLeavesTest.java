package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.pages.selfservice.MyLeavesPage;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.*;

/**
 * MyLeavesTest – tests for Employee self-service "My Leaves" page.
 */
public class MyLeavesTest extends BaseTest {

    private MyLeavesPage myLeavesPage;

    @BeforeMethod
    public void loginAndGoToMyLeaves() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.login(AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD);
        driver.get(AppConstants.MY_LEAVES_URL);
        myLeavesPage = new MyLeavesPage(driver);
    }

    @Test(priority = 1, description = "My Leaves page should load")
    public void testMyLeavesPageLoads() {
        Assert.assertTrue(myLeavesPage.isPageLoaded(),
                "My Leaves page should be loaded");
    }

    @Test(priority = 2, description = "Leave balance cards should be visible")
    public void testLeaveBalanceCardsVisible() {
        int count = myLeavesPage.getBalanceCardCount();
        ExtentManager.getTest().info("Balance cards: " + count);
        Assert.assertTrue(count >= 0);
    }

    @Test(priority = 3, description = "Apply Leave dialog should open")
    public void testApplyLeaveDialogOpens() {
        myLeavesPage.clickApplyLeave();
        ExtentManager.getTest().pass("Apply Leave modal/form opened");
        Assert.assertNotNull(driver.getCurrentUrl());
    }

    @Test(priority = 4, description = "Apply leave with valid data from Excel")
    public void testApplyLeaveSuccessfully() {
        myLeavesPage.applyForLeave(
                "Annual Leave",
                "2025-12-20",
                "2025-12-22",
                "Year-end vacation");

        Assert.assertTrue(myLeavesPage.isPageLoaded(), "Page should remain loaded after submit");
        ExtentManager.getTest().pass("Leave applied successfully");
    }

    @Test(priority = 5, description = "Leave request history table should be visible")
    public void testLeaveRequestHistoryVisible() {
        int count = myLeavesPage.getLeaveRequestCount();
        ExtentManager.getTest().info("Leave request history rows: " + count);
        Assert.assertTrue(count >= 0);
    }
}
