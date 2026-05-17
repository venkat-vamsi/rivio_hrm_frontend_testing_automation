package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.pages.selfservice.MyPayslipsPage;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * MyPayslipsTest – PAY-S-05.
 *
 *   RV_PAY_005 – Employees can view payslip history; "View Slip" opens detail modal
 */
public class MyPayslipsTest extends BaseTest {

    @Test(priority = 1, description = "RV_PAY_005 – My Payslips page renders the slips table")
    public void RV_PAY_005_payslipsTableRenders() {
        new LoginPage(driver).login(AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD);
        driver.get(AppConstants.MY_PAYSLIPS_URL);

        MyPayslipsPage page = new MyPayslipsPage(driver);
        Assert.assertTrue(page.isPageLoaded(),
                "My Payslips page should be loaded");
        ExtentManager.getTest().info("Payslip rows: " + page.getPayslipRowCount());

        if (page.hasViewSlipButtons()) {
            page.clickFirstViewSlip();
            Assert.assertTrue(page.isSlipModalOpen(),
                    "Detailed payslip modal should open");
        }
        ExtentManager.getTest().pass("My Payslips page renders correctly");
    }
}
