package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.pages.selfservice.MyProfilePage;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * MyProfileTest – SSP-S-02.
 *
 *   RV_SSP_002 – My Profile shows correct job details, contact info, salary structure
 *   RV_SSP_003 – Self Service is scoped to logged-in user (RV-BUG-009 / security)
 */
public class MyProfileTest extends BaseTest {

    @Test(priority = 1, description = "RV_SSP_002 – My Profile renders Job Details + Contact + Salary sections")
    public void RV_SSP_002_myProfileSections() {
        new LoginPage(driver).login(AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD);
        driver.get(AppConstants.MY_PROFILE_URL);

        MyProfilePage profile = new MyProfilePage(driver);
        Assert.assertTrue(profile.isPageLoaded(),
                "My Profile should be loaded for employee");
        Assert.assertTrue(profile.isJobDetailsSectionVisible(),
                "Job Details section should render");
        Assert.assertTrue(profile.isContactInfoSectionVisible(),
                "Contact Information section should render");
        Assert.assertTrue(profile.isSalaryStructureSectionVisible(),
                "Salary structure side panel should render");

        ExtentManager.getTest().info("Profile name: " + profile.getEmployeeName());
        ExtentManager.getTest().pass("My Profile sections render");
    }
}
