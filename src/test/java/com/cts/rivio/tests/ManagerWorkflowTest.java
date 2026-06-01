package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * ManagerWorkflowTest — Manager-role RBAC.
 *
 * Naming pattern: {@code rbac_manager_<scenario>}.
 *
 * Manager's positive access is covered by `rbac_manager_sidebar`
 * (RoleMatrixTest). This class verifies the route guard for one
 * forbidden route — Manager must not see the Admin Overview dashboard.
 *
 *   rbac_manager_blockedFromDashboard – /dashboard is guarded for Manager
 */
public class ManagerWorkflowTest extends BaseTest {

    @Override protected String getRole() { return ROLE_MANAGER; }

    @Test(groups = {"regression", "negative"},
          description = "rbac_manager_blockedFromDashboard – Manager cannot reach /dashboard")
    public void rbac_manager_blockedFromDashboard() {
        driver.get(AppConstants.DASHBOARD_URL);
        WaitUtils.waitForAngularLoad(driver);
        String url = WaitUtils.waitForUrlToBeStable(driver);
        Assert.assertFalse(url.endsWith("/dashboard"),
                "Manager should be redirected away from /dashboard. Final URL: " + url);
        ExtentManager.getTest().pass("Manager blocked from /dashboard");
    }
}
