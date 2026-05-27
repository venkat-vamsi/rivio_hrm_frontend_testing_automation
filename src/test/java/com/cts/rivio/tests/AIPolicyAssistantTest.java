package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.pages.SidebarPage;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * AIPolicyAssistantTest — Ask Rivi access for the Employee role.
 *
 * Naming pattern: {@code rbac_emp_<scenario>}.
 *
 * Positive access for non-Employee roles is verified by
 * `RoleMatrixTest.rbac_nonEmployee_askRivioVisible`. This class only
 * holds the Employee-side BUG.
 *
 *   rbac_emp_bug_askRivioAccess – FRD grants Employee policy-questions access
 *                                 via Ask Rivi; current build hides it.
 */
public class AIPolicyAssistantTest extends BaseTest {

    /**
     * Bug: FRD §2.8 / §4 grants the Employee role Ask Rivi access for
     * policy questions. The current sidebar hides the link and the
     * roleGuard rejects /ask-rivi for Employee.
     */
    @Test(groups = {"bug", "regression", "negative"},
          description = "rbac_emp_bug_askRivioAccess – Employee must be able to use Ask Rivi per FRD")
    public void rbac_emp_bug_askRivioAccess() {
        new LoginPage(driver).login(AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD);
        WaitUtils.waitForAngularLoad(driver);

        SidebarPage sb = new SidebarPage(driver);
        boolean sidebarShowsAskRivio = sb.isAskRiviVisible();

        driver.get(AppConstants.ASK_RIVI_URL);
        WaitUtils.waitForAngularLoad(driver);
        String finalUrl = WaitUtils.waitForUrlToBeStable(driver);
        boolean routeAllowed = finalUrl.endsWith("/ask-rivi");

        Assert.assertTrue(sidebarShowsAskRivio && routeAllowed,
                "FRD §2.8 grants Employee access to Ask Rivi for policy questions, "
              + "but Employee cannot use the feature. sidebar shows Ask Rivi? "
              + sidebarShowsAskRivio + " ; /ask-rivi route reachable? " + routeAllowed
              + " (URL=" + finalUrl + ").");
        ExtentManager.getTest().pass("Employee can ask policy questions");
    }
}
