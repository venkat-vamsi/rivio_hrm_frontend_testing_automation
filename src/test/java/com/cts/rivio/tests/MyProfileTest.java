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
 * MyProfileTest — Employee Self-Service → My Profile.
 *
 * Naming pattern: {@code ssp_profile_<scenario>}.
 *
 *   ssp_profile_bug_editAccess – FRD grants self-update; build has no Edit
 *                                affordance for the Employee role.
 */
public class MyProfileTest extends BaseTest {

    @Override protected String getRole() { return ROLE_EMPLOYEE; }

    /**
     * Bug: FRD role-based access matrix grants the Employee role
     * "self view AND self update". The current build renders a read-only
     * profile — no Edit / Update / Save affordance for Employee.
     */
    @Test(priority = 1, groups = {"bug", "regression", "negative"},
          description = "ssp_profile_bug_editAccess – Employee must be able to edit own profile per FRD")
    public void ssp_profile_bug_editAccess() {
        driver.get(AppConstants.MY_PROFILE_URL);
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.waitForUrlToBeStable(driver);

        MyProfilePage profile = new MyProfilePage(driver);
        Assert.assertTrue(profile.isPageLoaded(),
                "My Profile must load before checking edit affordance");

        boolean hasTextualButton = !driver.findElements(By.xpath(
            "(//a|//button)[contains(translate(.,'EDIT','edit'),'edit') "
          + "or contains(translate(.,'UPDATE','update'),'update') "
          + "or contains(translate(.,'SAVE','save'),'save')]")).isEmpty();

        boolean hasPencilOrAria = !driver.findElements(By.cssSelector(
                "button[aria-label*='edit' i], button[title*='edit' i], "
              + ".pi-pencil, [class*='pencil']")).isEmpty();

        Assert.assertTrue(hasTextualButton || hasPencilOrAria,
                "Employee has NO option to edit their own profile. FRD grants self-update.");
        ExtentManager.getTest().pass("Employee can edit own profile");
    }
}
