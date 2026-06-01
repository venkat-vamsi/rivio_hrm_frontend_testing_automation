package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.pages.SidebarPage;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * RoleMatrixTest — sidebar visibility per role.
 *
 * Naming pattern: {@code rbac_<role>_sidebar}.
 *
 * One test per role asserts the sidebar shows exactly the routes the role
 * is allowed to navigate to. This is the SOURCE OF TRUTH for "what each
 * role can see" — all other workflow tests trust this and only verify
 * one forbidden route via the route guard.
 *
 *   rbac_admin_sidebar                – Admin sees all 7 admin routes
 *   rbac_hr_sidebar                   – HR sees 5 admin routes, no payroll/company
 *   rbac_manager_sidebar              – Manager sees 3 routes
 *   rbac_payroll_sidebar              – Payroll Mgr sees 3 routes (incl payroll)
 *   rbac_emp_sidebar                  – Employee sees only Self Service
 *   rbac_nonEmployee_askRivioVisible  – 4 non-Employee roles all see Ask Rivi
 */
public class RoleMatrixTest extends BaseTest {

    @Test(groups = {"regression", "positive"},
          description = "rbac_admin_sidebar – Admin sees all admin nav routes")
    public void rbac_admin_sidebar() {
        new LoginPage(driver).login(AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD);
        new SidebarPage(driver).waitForSidebarToRender();
        assertSidebar(new String[]{"/dashboard","/employees","/attendance","/leave","/payroll","/ats","/company"},
                      new String[]{});
    }

    @Test(groups = {"regression", "positive"},
          description = "rbac_hr_sidebar – HR sees dashboard/emp/att/leave/ats, NOT payroll/company")
    public void rbac_hr_sidebar() {
        new LoginPage(driver).login(AppConstants.HR_EMAIL, AppConstants.HR_PASSWORD);
        new SidebarPage(driver).waitForSidebarToRender();
        assertSidebar(new String[]{"/dashboard","/employees","/attendance","/leave","/ats"},
                      new String[]{"/payroll","/company"});
    }

    @Test(groups = {"regression", "positive"},
          description = "rbac_manager_sidebar – Manager sees emp/att/leave only")
    public void rbac_manager_sidebar() {
        new LoginPage(driver).login(AppConstants.MANAGER_EMAIL, AppConstants.MANAGER_PASSWORD);
        new SidebarPage(driver).waitForSidebarToRender();
        assertSidebar(new String[]{"/employees","/attendance","/leave"},
                      new String[]{"/dashboard","/payroll","/ats","/company"});
    }

    @Test(groups = {"regression", "positive"},
          description = "rbac_payroll_sidebar – Payroll Mgr sees emp/att/payroll")
    public void rbac_payroll_sidebar() {
        new LoginPage(driver).login(AppConstants.PAYROLL_EMAIL, AppConstants.PAYROLL_PASSWORD);
        new SidebarPage(driver).waitForSidebarToRender();
        assertSidebar(new String[]{"/employees","/attendance","/payroll"},
                      new String[]{"/dashboard","/leave","/ats","/company"});
    }

    @Test(groups = {"regression", "positive"},
          description = "rbac_emp_sidebar – Employee sees only Self Service group, no admin routes")
    public void rbac_emp_sidebar() {
        new LoginPage(driver).login(AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD);
        SidebarPage sb = new SidebarPage(driver);
        sb.waitForSidebarToRender();
        String[] adminRoutes = {"/dashboard","/employees","/attendance","/leave","/payroll","/ats","/company"};
        for (String r : adminRoutes) {
            Assert.assertFalse(sb.isItemImmediatelyVisible(r),
                    "Employee sidebar must NOT include admin route: " + r);
        }
        sb.openSelfServiceGroup();
        Assert.assertTrue(sb.isItemVisible("/self-service/profile"),
                "Employee sidebar should show Self Service items");
    }

    @Test(groups = {"regression", "positive"},
          description = "rbac_nonEmployee_askRivioVisible – Admin/HR/Manager/PayrollMgr all see Ask Rivi link")
    public void rbac_nonEmployee_askRivioVisible() {
        String[][] nonEmployees = {
            {AppConstants.ADMIN_EMAIL,   AppConstants.ADMIN_PASSWORD,   "Super Admin"},
            {AppConstants.HR_EMAIL,      AppConstants.HR_PASSWORD,      "Hr"},
            {AppConstants.MANAGER_EMAIL, AppConstants.MANAGER_PASSWORD, "Manager"},
            {AppConstants.PAYROLL_EMAIL, AppConstants.PAYROLL_PASSWORD, "Payroll Mgr"}
        };
        for (String[] role : nonEmployees) {
            driver.get(AppConstants.LOGIN_URL);
            clearAuthStorage();
            driver.get(AppConstants.LOGIN_URL);
            com.cts.rivio.utils.WaitUtils.waitForAngularLoad(driver);
            new LoginPage(driver).login(role[0], role[1]);

            SidebarPage sb = new SidebarPage(driver);
            Assert.assertTrue(sb.isAskRiviVisible(),
                    role[2] + " should see the Ask Rivi sidebar entry");
            ExtentManager.getTest().info(role[2] + " sees Ask Rivi: ok");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void assertSidebar(String[] mustShow, String[] mustHide) {
        SidebarPage sb = new SidebarPage(driver);
        for (String r : mustShow) {
            Assert.assertTrue(sb.isItemVisible(r), "Sidebar should include " + r);
        }
        for (String r : mustHide) {
            Assert.assertFalse(sb.isItemImmediatelyVisible(r),
                    "Sidebar should NOT include " + r);
        }
    }
}
