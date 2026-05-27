package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * EmployeeAccessTest — Employee-role RBAC.
 *
 * Naming pattern: {@code rbac_emp_<scenario>}.
 *
 * Positive sidebar access is verified by `rbac_emp_sidebar`.
 *
 *   rbac_emp_blockedFromDashboard – /dashboard is guarded for Employee
 */
public class EmployeeAccessTest extends BaseTest {

    @Override protected String getRole() { return ROLE_EMPLOYEE; }

    @Test(groups = {"regression", "negative"},
          description = "rbac_emp_blockedFromDashboard – Employee cannot reach /dashboard")
    public void rbac_emp_blockedFromDashboard() {
        driver.get(AppConstants.DASHBOARD_URL);
        WaitUtils.waitForAngularLoad(driver);
        String url = WaitUtils.waitForUrlToBeStable(driver);
        Assert.assertFalse(url.endsWith("/dashboard"),
                "Employee should be redirected away from /dashboard. Final URL: " + url);
        ExtentManager.getTest().pass("Employee blocked from /dashboard");
    }
}
