package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * PayrollManagerWorkflowTest — Payroll-Manager role RBAC + the
 * payroll-manager-specific bug.
 *
 * Naming pattern: {@code rbac_payroll_<scenario>}.
 *
 * Positive sidebar access is verified by `rbac_payroll_sidebar`.
 *
 *   rbac_payroll_blockedFromDashboard – /dashboard is guarded for Payroll Mgr
 *   rbac_payroll_bug_editEmployees    – Payroll Mgr can edit employees
 *                                       (FRD permits only Admin/HR/Manager)
 */
public class PayrollManagerWorkflowTest extends BaseTest {

    @Override protected String getRole() { return ROLE_PAYROLL; }

    private String goAndStabilise(String url) {
        driver.get(url);
        WaitUtils.waitForAngularLoad(driver);
        return WaitUtils.waitForUrlToBeStable(driver);
    }

    @Test(groups = {"regression", "negative"},
          description = "rbac_payroll_blockedFromDashboard – Payroll Manager cannot reach /dashboard")
    public void rbac_payroll_blockedFromDashboard() {
        String url = goAndStabilise(AppConstants.DASHBOARD_URL);
        Assert.assertFalse(url.endsWith("/dashboard"),
                "Payroll Manager should be redirected from /dashboard. Final URL: " + url);
        ExtentManager.getTest().pass("Payroll Manager blocked from /dashboard");
    }

    /**
     * Bug: FRD §4 role-based access matrix restricts view + edit of employee
     * details to Admin/HR/Manager. The Payroll Manager must NOT be able to
     * open an employee profile and see pencil/Edit Details/Edit Contact Info
     * buttons.
     */
    @Test(groups = {"bug", "regression", "negative"},
          description = "rbac_payroll_bug_editEmployees – Payroll Manager must NOT view/edit employee details (FRD RBAC)")
    public void rbac_payroll_bug_editEmployees() {
        String dirUrl = goAndStabilise(AppConstants.EMPLOYEE_DIR_URL);

        if (!dirUrl.contains("/employees")) {
            ExtentManager.getTest().pass(
                "Payroll Manager blocked at /employees route — FRD compliant");
            return;
        }

        java.util.List<org.openqa.selenium.WebElement> eyes = driver.findElements(
            By.xpath("//p-table//tbody/tr[1]//button[.//i[contains(@class,'pi-eye')]]"));
        if (eyes.isEmpty()) {
            eyes = driver.findElements(
                By.xpath("//p-table//button[.//i[contains(@class,'pi-eye')]]"));
        }
        Assert.assertFalse(eyes.isEmpty(),
                "Setup: no eye-icon profile button rendered for Payroll Manager on "
              + dirUrl + ". Cannot drill in to verify edit affordances.");

        WaitUtils.scrollAndClick(driver, eyes.get(0));
        WaitUtils.waitForAngularLoad(driver);
        String profileUrl = WaitUtils.waitForUrlToBeStable(driver);
        ExtentManager.getTest().info("Profile URL after eye-icon click: " + profileUrl);

        boolean reachedProfile = profileUrl.matches(".*/employees/\\d+.*");
        if (!reachedProfile) {
            ExtentManager.getTest().pass(
                "Payroll Manager blocked from /employees/:id — FRD compliant");
            return;
        }

        java.util.List<org.openqa.selenium.WebElement> editAffordances = driver.findElements(By.xpath(
            "//button[.//i[contains(@class,'pi-pencil')]] | "
          + "//button[contains(normalize-space(.),'Edit Details')] | "
          + "//button[contains(normalize-space(.),'Edit Contact')] | "
          + "//button[@title='Edit Contact Info']"
        ));

        Assert.assertTrue(editAffordances.isEmpty(),
                "Payroll Manager opened employee profile " + profileUrl
              + " and the page exposes " + editAffordances.size()
              + " edit affordance(s). FRD restricts view+edit of employee details to "
              + "Admin/HR/Manager only.");
        ExtentManager.getTest().pass(
            "Payroll Manager profile view is read-only — FRD compliant");
    }
}
