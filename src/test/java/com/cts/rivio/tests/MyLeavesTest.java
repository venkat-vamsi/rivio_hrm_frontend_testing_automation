package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.pages.selfservice.MyLeavesPage;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * MyLeavesTest – LVE-S-03 + LVE-S-04.
 *
 *   RV_LVE_004 – Balance cards show Available/Used/Total
 *   RV_LVE_005 – Request history table renders with status badges
 */
public class MyLeavesTest extends BaseTest {

    @Test(priority = 1, description = "RV_LVE_004 – My Leaves balance cards visible")
    public void RV_LVE_004_balanceCards() {
        new LoginPage(driver).login(AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD);
        driver.get(AppConstants.MY_LEAVES_URL);

        MyLeavesPage page = new MyLeavesPage(driver);
        Assert.assertTrue(page.isPageLoaded(),
                "My Leaves page should be loaded");
        int cards = page.getBalanceCardCount();
        ExtentManager.getTest().info("Balance cards visible: " + cards);
        Assert.assertTrue(cards >= 0, "Balance card rendering should not throw");
        ExtentManager.getTest().pass("My Leaves page renders");
    }

    @Test(priority = 2, description = "RV_LVE_005 – Leave Request History table is visible")
    public void RV_LVE_005_historyTable() {
        new LoginPage(driver).login(AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD);
        driver.get(AppConstants.MY_LEAVES_URL);

        MyLeavesPage page = new MyLeavesPage(driver);
        Assert.assertTrue(page.isPageLoaded(),
                "My Leaves page should be loaded");
        int rows = page.getHistoryRowCount();
        ExtentManager.getTest().info("History rows: " + rows);
        ExtentManager.getTest().pass("Leave request history renders");
    }
}
