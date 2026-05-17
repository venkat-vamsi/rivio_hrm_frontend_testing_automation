package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.pages.SidebarPage;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * FrdComplianceTest – streamlined FRD-vs-implementation checks.
 *
 * Only tests for genuine FRD requirements that DO NOT have an equivalent
 * surface elsewhere in the app are retained. Self-service surfaces (My
 * Attendance, My Leaves, My Payslips, My Profile view) already satisfy several
 * FRD requirements via different UI paths; those checks were removed after
 * manual verification confirmed the features are functional.
 *
 * Retained tests = ~10 genuine missing features the team should fix.
 */
public class FrdComplianceTest extends BaseTest {

    private boolean linkOrButtonWithTextExists(String partialText) {
        String lower = partialText.toLowerCase();
        return !driver.findElements(By.xpath(
            "(//a|//button)[contains(translate(normalize-space(.),"
            + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" + lower + "')]")).isEmpty();
    }

    private boolean elementWithTextExists(String partialText) {
        String lower = partialText.toLowerCase();
        return !driver.findElements(By.xpath(
            "//*[contains(translate(normalize-space(.),"
            + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'" + lower + "')]")).isEmpty();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // §2.1 Login Page — Forgot Password is genuinely missing
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "RV_FRD_004 – FRD §2.1: Login page must show 'Forgot Password' option")
    public void RV_FRD_004_forgotPasswordExists() {
        WaitUtils.waitForAngularLoad(driver);
        Assert.assertTrue(linkOrButtonWithTextExists("forgot"),
                "RV-BUG-026: Login page has NO 'Forgot Password' option. FRD §2.1 requires it.");
        ExtentManager.getTest().pass("Forgot Password option present");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // §2.5/2.9 — Employee profile edit affordance is missing
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "RV_FRD_001 – FRD §2.5/§2.9: Employee can Update Profile (edit affordance)")
    public void RV_FRD_001_employeeCanUpdateProfile() {
        new LoginPage(driver).login(AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD);
        driver.get(AppConstants.MY_PROFILE_URL);
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.waitForUrlToBeStable(driver);
        Assert.assertTrue(linkOrButtonWithTextExists("edit")
                       || linkOrButtonWithTextExists("update")
                       || linkOrButtonWithTextExists("save"),
                "RV-BUG-022: Employee profile has NO edit/update affordance. FRD §2.5/2.9.");
        ExtentManager.getTest().pass("Profile edit affordance present");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // §2.8/2.9 — AI Policy Assistant is hidden for Employee role
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "RV_FRD_002 – FRD §2.8/§2.9: Employee can access Ask Rivi (AI Policy)")
    public void RV_FRD_002_employeeCanAskPolicyQuestions() {
        new LoginPage(driver).login(AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD);
        WaitUtils.waitForAngularLoad(driver);
        SidebarPage sb = new SidebarPage(driver);
        Assert.assertTrue(sb.isAskRiviVisible(),
                "RV-BUG-023: Employee cannot see Ask Rivi in sidebar. "
              + "FRD §2.8 + §2.9 grant Employee access to policy AI. "
              + "Code: sidebar.component.html line 44 wrongly hides this from Employee.");
        ExtentManager.getTest().pass("Ask Rivi visible to Employee");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // §2.9 — Manager team reports surface is missing
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "RV_FRD_003 – FRD §2.9: Manager can View Team Reports")
    public void RV_FRD_003_managerCanViewTeamReports() {
        new LoginPage(driver).login(AppConstants.MANAGER_EMAIL, AppConstants.MANAGER_PASSWORD);
        WaitUtils.waitForAngularLoad(driver);
        Assert.assertTrue(linkOrButtonWithTextExists("team reports")
                       || linkOrButtonWithTextExists("reports"),
                "RV-BUG-024: No Team Reports surface for Manager. FRD §2.9.");
        ExtentManager.getTest().pass("Team Reports link present for Manager");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // §4 — Dashboard notifications surface is missing
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "RV_FRD_005 – FRD §4: Notifications surface inside the dashboard")
    public void RV_FRD_005_dashboardHasNotifications() {
        new LoginPage(driver).login(AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD);
        driver.get(AppConstants.DASHBOARD_URL);
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.waitForUrlToBeStable(driver);
        boolean hasNotif =
            !driver.findElements(By.cssSelector(
                "header [class*='notif'], header [class*='bell'], "
              + ".pi-bell, [title*='Notification' i], [aria-label*='Notification' i]"
            )).isEmpty();
        Assert.assertTrue(hasNotif,
                "RV-BUG-027: Dashboard has NO notifications surface. FRD §4.");
        ExtentManager.getTest().pass("Notifications surface present");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // §2.7 — Performance Management module is missing
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "RV_FRD_007 – FRD §2.7: Performance Management module exists")
    public void RV_FRD_007_performanceManagementExists() {
        new LoginPage(driver).login(AppConstants.MANAGER_EMAIL, AppConstants.MANAGER_PASSWORD);
        WaitUtils.waitForAngularLoad(driver);
        Assert.assertTrue(linkOrButtonWithTextExists("performance")
                       || linkOrButtonWithTextExists("review")
                       || linkOrButtonWithTextExists("goal"),
                "RV-BUG-029: No Performance Management module. FRD §2.7.");
        ExtentManager.getTest().pass("Performance Management link present");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // §5 — Reports & Analytics module is missing
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "RV_FRD_009 – FRD §5: Reports & Analytics module")
    public void RV_FRD_009_reportsAndAnalyticsExists() {
        new LoginPage(driver).login(AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD);
        WaitUtils.waitForAngularLoad(driver);
        Assert.assertTrue(linkOrButtonWithTextExists("reports") || linkOrButtonWithTextExists("analytics"),
                "RV-BUG-031: No Reports & Analytics module. FRD §5.");
        ExtentManager.getTest().pass("Reports module link present");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // §2.4 — Apply Leave modal should accept supporting documents
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "RV_FRD_017 – FRD §2.4: Apply-leave dialog should accept supporting documents")
    public void RV_FRD_017_applyLeaveAcceptsDocuments() {
        new LoginPage(driver).login(AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD);
        driver.get(AppConstants.MY_LEAVES_URL);
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.waitForUrlToBeStable(driver);

        // Open the Apply modal
        try {
            driver.findElement(By.xpath("//button[contains(.,'Apply')]")).click();
        } catch (Exception ignored) {}
        WaitUtils.hardWait(1000);

        boolean hasFileInput =
            !driver.findElements(By.cssSelector("p-dialog input[type='file'], p-dialog p-fileupload")).isEmpty();

        Assert.assertTrue(hasFileInput,
                "RV-BUG-039: Apply Leave modal has NO file upload. FRD §2.4.");
        ExtentManager.getTest().pass("Document upload in Apply Leave present");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // §2.6 — HR/Admin can export salary bank file (CSV/TXT)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "RV_FRD_022 – FRD §2.6: Payroll exposes Bank File Export (CSV/TXT)")
    public void RV_FRD_022_bankFileExport() {
        new LoginPage(driver).login(AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD);
        driver.get(AppConstants.PAYROLL_URL);
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.waitForUrlToBeStable(driver);
        Assert.assertTrue(linkOrButtonWithTextExists("export")
                       || linkOrButtonWithTextExists("download csv")
                       || elementWithTextExists("bank file"),
                "RV-BUG-044: Payroll has NO bank-file export. FRD §2.6.");
        ExtentManager.getTest().pass("Bank-file export present");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // §2.8 — HR/Admin can upload new policies (Admin Controls)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "RV_FRD_023 – FRD §2.8: HR/Admin can upload new policies")
    public void RV_FRD_023_hrCanUploadPolicies() {
        new LoginPage(driver).login(AppConstants.HR_EMAIL, AppConstants.HR_PASSWORD);
        WaitUtils.waitForAngularLoad(driver);
        driver.get(AppConstants.ASK_RIVI_URL);
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.waitForUrlToBeStable(driver);
        Assert.assertTrue(linkOrButtonWithTextExists("upload")
                       || linkOrButtonWithTextExists("add policy")
                       || linkOrButtonWithTextExists("manage polic"),
                "RV-BUG-045: HR has NO upload-policy affordance in Ask Rivi. FRD §2.8.");
        ExtentManager.getTest().pass("HR can upload policies");
    }
}
