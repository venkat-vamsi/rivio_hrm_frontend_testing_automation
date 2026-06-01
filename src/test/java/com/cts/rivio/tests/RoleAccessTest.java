package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * RoleAccessTest — unauthenticated-access guard.
 *
 * Naming pattern: {@code rbac_unauthRedirect}.
 *
 * Per-role access is covered by:
 *   – `RoleMatrixTest.rbac_<role>_sidebar` (positive sidebar)
 *   – `<Role>WorkflowTest.rbac_<role>_blockedFromXxx` (one forbidden route)
 *
 * This class only tests the unauthenticated case.
 */
public class RoleAccessTest extends BaseTest {

    @Test(groups = {"smoke", "regression", "negative"},
          description = "rbac_unauthRedirect – Unauthenticated user is sent to /login when deep-linking a protected route")
    public void rbac_unauthRedirect() {
        clearAuthStorage();
        driver.get(AppConstants.DASHBOARD_URL);
        WaitUtils.waitForAngularLoad(driver);
        String url = WaitUtils.waitForUrlToBeStable(driver);
        Assert.assertTrue(url.contains("/login"),
                "Unauthenticated deep-link should redirect to /login. Final URL: " + url);
        ExtentManager.getTest().pass("Unauthenticated redirect → /login OK");
    }
}
