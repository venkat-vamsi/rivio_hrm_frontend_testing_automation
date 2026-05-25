package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.selfservice.MyProfilePage;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * MyProfileTest – SSP-S-02.
 *
 *   RV_SSP_002 – My Profile shows correct job details, contact info, salary structure
 *   RV_SSP_003 – Self Service is scoped to logged-in user (RV-BUG-009 / security)
 */
public class MyProfileTest extends BaseTest {

    @Override protected String getRole() { return ROLE_EMPLOYEE; }

    @Test(priority = 1, groups = {"smoke", "regression"}, description = "RV_SSP_002 – My Profile renders Job Details + Contact + Salary sections")
    public void RV_SSP_002_myProfileSections() {
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

    /**
     * RV-BUG-NEW-10: FRD role-based access matrix grants the Employee role
     * "self view AND self update" of their profile. The current build only
     * renders a read-only profile — there is no Edit / Update / Save affordance
     * for the Employee to modify their own details.
     */
    @Test(priority = 2, groups = {"bug", "regression"}, description =
        "RV_SSP_BUG_10 – Employee must be able to edit own profile details (FRD self-update)")
    public void RV_SSP_BUG_10_employeeCanEditOwnProfile() {
        driver.get(AppConstants.MY_PROFILE_URL);
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.waitForUrlToBeStable(driver);

        MyProfilePage profile = new MyProfilePage(driver);
        Assert.assertTrue(profile.isPageLoaded(), "My Profile must load before checking edit affordance");

        boolean hasTextualButton = !driver.findElements(By.xpath(
            "(//a|//button)[contains(translate(.,'EDIT','edit'),'edit') "
          + "or contains(translate(.,'UPDATE','update'),'update') "
          + "or contains(translate(.,'SAVE','save'),'save')]")).isEmpty();

        boolean hasPencilOrAria = !driver.findElements(By.cssSelector(
                "button[aria-label*='edit' i], button[title*='edit' i], "
              + ".pi-pencil, [class*='pencil']")).isEmpty();

        Assert.assertTrue(hasTextualButton || hasPencilOrAria,
                "RV-BUG-NEW-10: Employee has NO option to edit their own profile on "
              + AppConstants.MY_PROFILE_URL + ". FRD role-based access matrix grants the "
              + "Employee role self-view AND self-update of profile details — the build "
              + "renders a read-only profile, contradicting the FRD.");
        ExtentManager.getTest().pass("Employee can edit own profile");
    }
}
