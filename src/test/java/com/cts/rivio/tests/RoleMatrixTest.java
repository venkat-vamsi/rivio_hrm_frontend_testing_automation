package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.pages.SidebarPage;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * RoleMatrixTest – exhaustive role × feature coverage.
 *
 * For every role this verifies:
 *   1. Login succeeds
 *   2. The sidebar exposes ONLY the items the roleGuard allows
 *   3. Each allowed route loads without redirecting
 *   4. Each forbidden route redirects away from itself
 *   5. Self-service routes work (every role has access per app.routes.ts)
 *
 * Source of truth: Rivio_Angular-main/src/app/app.routes.ts
 *
 * Authoritative matrix:
 *   Super Admin     → /dashboard, /employees, /attendance, /leave, /payroll, /ats, /company, /ask-rivi
 *   Hr              → /dashboard, /employees, /attendance, /leave,           /ats,           /ask-rivi
 *   Manager         →             /employees, /attendance, /leave,                           /ask-rivi
 *   Payroll Manager →             /employees, /attendance,         /payroll,                 /ask-rivi
 *   Employee        →                                                                         (none)
 *   All roles       → /self-service/profile, /self-service/attendance, /self-service/leaves, /self-service/payslips
 */
public class RoleMatrixTest extends BaseTest {

    // ── Data providers describe the access matrix ────────────────────────────

    /** {role label, email, password, route, shouldBeAllowed} */
    @DataProvider(name = "roleRouteAccess")
    public Object[][] roleRouteAccess() {
        String[] adminRoutes    = {"/dashboard", "/employees", "/attendance", "/leave",
                                   "/payroll", "/ats", "/company", "/ask-rivi"};
        String[] hrAllowed      = {"/dashboard", "/employees", "/attendance", "/leave", "/ats", "/ask-rivi"};
        String[] hrForbidden    = {"/payroll", "/company"};
        String[] mgrAllowed     = {"/employees", "/attendance", "/leave", "/ask-rivi"};
        String[] mgrForbidden   = {"/dashboard", "/payroll", "/ats", "/company"};
        String[] payAllowed     = {"/employees", "/attendance", "/payroll", "/ask-rivi"};
        String[] payForbidden   = {"/dashboard", "/leave", "/ats", "/company"};
        String[] empForbidden   = {"/dashboard", "/employees", "/attendance", "/leave",
                                   "/payroll", "/ats", "/company", "/ask-rivi"};

        java.util.List<Object[]> rows = new java.util.ArrayList<>();

        for (String r : adminRoutes) rows.add(row("Super Admin", AppConstants.ADMIN_EMAIL,    AppConstants.ADMIN_PASSWORD,    r, true));
        for (String r : hrAllowed)   rows.add(row("Hr",          AppConstants.HR_EMAIL,       AppConstants.HR_PASSWORD,       r, true));
        for (String r : hrForbidden) rows.add(row("Hr",          AppConstants.HR_EMAIL,       AppConstants.HR_PASSWORD,       r, false));
        for (String r : mgrAllowed)  rows.add(row("Manager",     AppConstants.MANAGER_EMAIL,  AppConstants.MANAGER_PASSWORD,  r, true));
        for (String r : mgrForbidden)rows.add(row("Manager",     AppConstants.MANAGER_EMAIL,  AppConstants.MANAGER_PASSWORD,  r, false));
        for (String r : payAllowed)  rows.add(row("Payroll Mgr", AppConstants.PAYROLL_EMAIL,  AppConstants.PAYROLL_PASSWORD,  r, true));
        for (String r : payForbidden)rows.add(row("Payroll Mgr", AppConstants.PAYROLL_EMAIL,  AppConstants.PAYROLL_PASSWORD,  r, false));
        for (String r : empForbidden)rows.add(row("Employee",    AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD, r, false));

        return rows.toArray(new Object[0][]);
    }

    private Object[] row(String label, String email, String pwd, String route, boolean allowed) {
        return new Object[]{label, email, pwd, route, allowed};
    }

    /** {role label, email, password, selfServiceRoute} — every role must reach every self-service tab */
    @DataProvider(name = "selfServicePerRole")
    public Object[][] selfServicePerRole() {
        String[] roles = {"Super Admin", "Hr", "Manager", "Payroll Mgr", "Employee"};
        String[][] creds = {
            {AppConstants.ADMIN_EMAIL,    AppConstants.ADMIN_PASSWORD},
            {AppConstants.HR_EMAIL,       AppConstants.HR_PASSWORD},
            {AppConstants.MANAGER_EMAIL,  AppConstants.MANAGER_PASSWORD},
            {AppConstants.PAYROLL_EMAIL,  AppConstants.PAYROLL_PASSWORD},
            {AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD},
        };
        String[] selfRoutes = {"/self-service/profile", "/self-service/attendance",
                               "/self-service/leaves",  "/self-service/payslips"};

        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        for (int i = 0; i < roles.length; i++) {
            for (String r : selfRoutes) {
                rows.add(new Object[]{roles[i], creds[i][0], creds[i][1], r});
            }
        }
        return rows.toArray(new Object[0][]);
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test(dataProvider = "roleRouteAccess",
          description = "Each role × route combination matches the roleGuard policy")
    public void roleCanAccessOrIsRedirected(String roleLabel, String email, String password,
                                             String route, boolean shouldBeAllowed) {
        ExtentManager.getTest().info("[" + roleLabel + "] route=" + route
                + " expected=" + (shouldBeAllowed ? "ALLOWED" : "DENIED"));

        new LoginPage(driver).login(email, password);
        driver.get(AppConstants.BASE_URL + route.replaceFirst("^/", ""));
        WaitUtils.waitForAngularLoad(driver);
        // Wait up to 8s for Angular's roleGuard redirect to settle.
        String actual = WaitUtils.waitForUrlToBeStable(driver);
        ExtentManager.getTest().info("Final URL after stability: " + actual);

        if (shouldBeAllowed) {
            Assert.assertTrue(actual.contains(route),
                    roleLabel + " should access " + route + " but was redirected to: " + actual);
        } else {
            // After roleGuard redirect, URL must NOT end with the forbidden route.
            // Use endsWith so /ats doesn't accidentally match /ats-extension etc.
            Assert.assertFalse(actual.endsWith(route),
                    roleLabel + " should NOT reach " + route + " — URL was: " + actual);
        }
    }

    @Test(dataProvider = "selfServicePerRole",
          description = "Every role can reach every self-service page")
    public void everyRoleCanUseSelfService(String roleLabel, String email, String password, String route) {
        ExtentManager.getTest().info("[" + roleLabel + "] self-service route=" + route);

        new LoginPage(driver).login(email, password);
        driver.get(AppConstants.BASE_URL + route.replaceFirst("^/", ""));
        WaitUtils.waitForAngularLoad(driver);
        String actual = WaitUtils.waitForUrlToBeStable(driver);

        Assert.assertTrue(actual.contains(route),
                roleLabel + " should reach " + route + " but ended at: " + actual);
    }

    @Test(description = "Sidebar items exactly match the role's allow-list")
    public void sidebarItemsForAdmin() {
        new LoginPage(driver).login(AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD);
        new SidebarPage(driver).waitForSidebarToRender();
        assertSidebar(new String[]{"/dashboard","/employees","/attendance","/leave","/payroll","/ats","/company"},
                      new String[]{});
    }

    @Test(description = "Sidebar items for HR include leave + recruitment but NOT payroll/company")
    public void sidebarItemsForHr() {
        new LoginPage(driver).login(AppConstants.HR_EMAIL, AppConstants.HR_PASSWORD);
        new SidebarPage(driver).waitForSidebarToRender();
        assertSidebar(new String[]{"/dashboard","/employees","/attendance","/leave","/ats"},
                      new String[]{"/payroll","/company"});
    }

    @Test(description = "Sidebar for Manager: employees + attendance + leave only (no dashboard, no payroll)")
    public void sidebarItemsForManager() {
        new LoginPage(driver).login(AppConstants.MANAGER_EMAIL, AppConstants.MANAGER_PASSWORD);
        new SidebarPage(driver).waitForSidebarToRender();
        assertSidebar(new String[]{"/employees","/attendance","/leave"},
                      new String[]{"/dashboard","/payroll","/ats","/company"});
    }

    @Test(description = "Sidebar for Payroll Manager: employees + attendance + payroll (no leave, no ats)")
    public void sidebarItemsForPayrollManager() {
        new LoginPage(driver).login(AppConstants.PAYROLL_EMAIL, AppConstants.PAYROLL_PASSWORD);
        new SidebarPage(driver).waitForSidebarToRender();
        assertSidebar(new String[]{"/employees","/attendance","/payroll"},
                      new String[]{"/dashboard","/leave","/ats","/company"});
    }

    @Test(description = "Sidebar for Employee: NO admin routes — only Self Service group")
    public void sidebarItemsForEmployee() {
        new LoginPage(driver).login(AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD);
        SidebarPage sb = new SidebarPage(driver);
        sb.waitForSidebarToRender();
        // Use immediate check for negative assertions — don't waste 5s per route.
        // NB: /self-service/attendance is allowed (My Attendance), but we're checking
        // exact href '/attendance' which is the admin route, so they won't collide.
        String[] adminRoutes = {"/dashboard","/employees","/attendance","/leave","/payroll","/ats","/company"};
        for (String r : adminRoutes) {
            Assert.assertFalse(sb.isItemImmediatelyVisible(r),
                    "Employee sidebar must NOT include admin route: " + r);
        }
        sb.openSelfServiceGroup();
        Assert.assertTrue(sb.isItemVisible("/self-service/profile"),
                "Employee sidebar should show Self Service items");
    }

    @Test(description = "Ask Rivi link visible to all non-Employee roles")
    public void askRiviVisibleToAllNonEmployee() {
        String[][] nonEmployees = {
            {AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD, "Super Admin"},
            {AppConstants.HR_EMAIL, AppConstants.HR_PASSWORD, "Hr"},
            {AppConstants.MANAGER_EMAIL, AppConstants.MANAGER_PASSWORD, "Manager"},
            {AppConstants.PAYROLL_EMAIL, AppConstants.PAYROLL_PASSWORD, "Payroll Mgr"}
        };
        for (String[] role : nonEmployees) {
            // Fresh login each iteration — clearAuthStorage isn't enough; we need fresh page state
            driver.get(AppConstants.LOGIN_URL);
            clearAuthStorage();
            driver.get(AppConstants.LOGIN_URL);
            WaitUtils.waitForAngularLoad(driver);
            new LoginPage(driver).login(role[0], role[1]);

            SidebarPage sb = new SidebarPage(driver);
            Assert.assertTrue(sb.isAskRiviVisible(),
                    role[2] + " should see the Ask Rivi sidebar entry");
            ExtentManager.getTest().info(role[2] + " sees Ask Rivi: ok");
        }
    }

    @Test(description = "Ask Rivi link is HIDDEN for Employee")
    public void askRiviHiddenForEmployee() {
        new LoginPage(driver).login(AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD);
        SidebarPage sb = new SidebarPage(driver);
        Assert.assertFalse(sb.isAskRiviVisible(),
                "Employee should NOT see the Ask Rivi sidebar entry (sidebar.component.html:44)");
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private void assertSidebar(String[] mustShow, String[] mustHide) {
        SidebarPage sb = new SidebarPage(driver);
        for (String r : mustShow) {
            Assert.assertTrue(sb.isItemVisible(r), "Sidebar should include " + r);
        }
        for (String r : mustHide) {
            // Use immediate check for negatives to avoid 5s wait per route.
            Assert.assertFalse(sb.isItemImmediatelyVisible(r), "Sidebar should NOT include " + r);
        }
    }
}
