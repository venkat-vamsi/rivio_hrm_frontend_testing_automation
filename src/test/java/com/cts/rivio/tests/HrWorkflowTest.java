package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * HrWorkflowTest — HR-role RBAC + the HR-specific bug.
 *
 * Naming pattern: {@code rbac_hr_<scenario>}.
 *
 * HR's positive-access paths are already proven by `rbac_hr_sidebar`
 * (RoleMatrixTest) — visible links match a known allowlist. This class
 * only verifies the route guard for one forbidden route, plus the HR-side
 * Active Pay Cycles KPI bug.
 *
 *   rbac_hr_blockedFromPayroll – HR is route-guard-blocked from /payroll
 *   rbac_hr_bug_payCyclesKpi   – Active Pay Cycles KPI must not be shown
 *                                / must not route HR to their own profile
 */
public class HrWorkflowTest extends BaseTest {

    @Override protected String getRole() { return ROLE_HR; }

    private String goAndStabilise(String url) {
        driver.get(url);
        WaitUtils.waitForAngularLoad(driver);
        return WaitUtils.waitForUrlToBeStable(driver);
    }

    @Test(groups = {"regression", "negative"},
          description = "rbac_hr_blockedFromPayroll – HR cannot reach /payroll (role guard)")
    public void rbac_hr_blockedFromPayroll() {
        String url = goAndStabilise(AppConstants.PAYROLL_URL);
        Assert.assertFalse(url.endsWith("/payroll"),
                "HR should be redirected away from /payroll. Final URL: " + url);
        ExtentManager.getTest().pass("HR blocked from /payroll");
    }

    /**
     * Bug: HR has no payroll access (proven above) yet the Admin Overview
     * KPI card "Active Pay Cycles" is rendered for HR. Worse, clicking it
     * routes HR to /self-service/profile instead of hiding the card or
     * routing to a safe destination.
     */
    @Test(groups = {"bug", "regression", "negative"},
          description = "rbac_hr_bug_payCyclesKpi – Active Pay Cycles KPI must not be shown / must not route HR to their own profile")
    public void rbac_hr_bug_payCyclesKpi() {
        goAndStabilise(AppConstants.DASHBOARD_URL);

        By kpiLocator = By.xpath(
            "//div[contains(@class,'glass-panel')]"
          + "[.//*[contains(translate(normalize-space(.),"
          + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'active pay cycles')]]");
        java.util.List<WebElement> kpis = driver.findElements(kpiLocator);

        if (kpis.isEmpty()) {
            ExtentManager.getTest().pass(
                "Active Pay Cycles KPI not shown to HR — correct behaviour");
            return;
        }

        WaitUtils.scrollAndClick(driver, kpis.get(0));
        WaitUtils.hardWait(800);
        String finalUrl = WaitUtils.waitForUrlToBeStable(driver);
        ExtentManager.getTest().info("Final URL after clicking Active Pay Cycles KPI: " + finalUrl);

        Assert.assertTrue(finalUrl.contains("/payroll"),
                "HR clicked Active Pay Cycles KPI and was routed to " + finalUrl
              + ". HR has no payroll access — the KPI should be hidden, or "
              + "at minimum the click must route to /payroll.");
        ExtentManager.getTest().pass("Active Pay Cycles KPI behaviour acceptable for HR");
    }
}
