package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.*;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * PayrollManagerWorkflowTest – Payroll Manager has /employees, /attendance,
 * /payroll, /ask-rivi. No /dashboard, no /leave, no /ats, no /company.
 */
public class PayrollManagerWorkflowTest extends BaseTest {

    private String goAndStabilise(String url) {
        driver.get(url);
        WaitUtils.waitForAngularLoad(driver);
        return WaitUtils.waitForUrlToBeStable(driver);
    }

    @Test(description = "Payroll Manager opens /payroll")
    public void payroll_dashboardLoads() {
        new LoginPage(driver).login(AppConstants.PAYROLL_EMAIL, AppConstants.PAYROLL_PASSWORD);
        String url = goAndStabilise(AppConstants.PAYROLL_URL);
        Assert.assertTrue(url.contains("/payroll"),
                "Payroll Manager should reach /payroll. URL: " + url);
        PayrollDashboardPage p = new PayrollDashboardPage(driver);
        Assert.assertTrue(p.isPageLoaded(), "Payroll page should render");
        ExtentManager.getTest().pass("Payroll Manager can open /payroll");
    }

    @Test(description = "Payroll Manager opens Employees directory")
    public void payroll_employeesDirectoryLoads() {
        new LoginPage(driver).login(AppConstants.PAYROLL_EMAIL, AppConstants.PAYROLL_PASSWORD);
        String url = goAndStabilise(AppConstants.EMPLOYEE_DIR_URL);
        Assert.assertTrue(url.contains("/employees"),
                "Payroll Manager should reach Employees directory. URL: " + url);
        EmployeeDirectoryPage dir = new EmployeeDirectoryPage(driver);
        Assert.assertTrue(dir.isPageLoaded(), "Employees page should render");
        ExtentManager.getTest().pass("Payroll Manager can open Employees");
    }

    @Test(description = "Payroll Manager opens Attendance")
    public void payroll_attendanceLoads() {
        new LoginPage(driver).login(AppConstants.PAYROLL_EMAIL, AppConstants.PAYROLL_PASSWORD);
        String url = goAndStabilise(AppConstants.ATTENDANCE_URL);
        Assert.assertTrue(url.contains("/attendance"),
                "Payroll Manager should reach Attendance. URL: " + url);
        AttendancePage att = new AttendancePage(driver);
        Assert.assertTrue(att.isPageLoaded(), "Attendance page should render");
        ExtentManager.getTest().pass("Payroll Manager can open Attendance");
    }

    @Test(description = "Payroll Manager cannot reach /dashboard — redirected")
    public void payroll_dashboardIsBlocked() {
        new LoginPage(driver).login(AppConstants.PAYROLL_EMAIL, AppConstants.PAYROLL_PASSWORD);
        String url = goAndStabilise(AppConstants.DASHBOARD_URL);
        Assert.assertFalse(url.endsWith("/dashboard"),
                "Payroll Manager should be redirected from /dashboard. Final URL: " + url);
    }

    @Test(description = "Payroll Manager cannot reach /leave — redirected")
    public void payroll_leaveIsBlocked() {
        new LoginPage(driver).login(AppConstants.PAYROLL_EMAIL, AppConstants.PAYROLL_PASSWORD);
        String url = goAndStabilise(AppConstants.LEAVE_URL);
        Assert.assertFalse(url.endsWith("/leave"),
                "Payroll Manager should be redirected from /leave. Final URL: " + url);
    }

    @Test(description = "Payroll Manager cannot reach /ats — redirected")
    public void payroll_atsIsBlocked() {
        new LoginPage(driver).login(AppConstants.PAYROLL_EMAIL, AppConstants.PAYROLL_PASSWORD);
        String url = goAndStabilise(AppConstants.RECRUITMENT_URL);
        Assert.assertFalse(url.endsWith("/ats"),
                "Payroll Manager should be redirected from /ats. Final URL: " + url);
    }

    /**
     * RV-BUG-NEW-09: FRD §2 role-based access matrix restricts the view + edit
     * of employee details to Super Admin, HR and Manager only. The Payroll
     * Manager must NOT be able to:
     *   - reach /employees and click the eye icon to open a profile, AND
     *   - see the pencil icons / "Edit Details" / "Edit Contact Info"
     *     buttons on /employees/:id.
     *
     * Per Rivio_Angular-main employee-profile.component.html, the profile
     * exposes three edit affordances:
     *   - line 53: <button (click)="openBasicInfoModal()" title="Edit Contact Info"><i class="pi pi-pencil">
     *   - line 100: <button (click)="openJobDetailsModal()">…<i class="pi pi-pencil"></i> Edit Details
     *   - sundry pi-pencil icons in tabs
     */
    @Test(description = "RV_PMW_BUG_09 – Payroll Manager must NOT view or edit employee details (FRD RBAC)")
    public void RV_PMW_BUG_09_payrollManagerCannotEditEmployees() {
        new LoginPage(driver).login(AppConstants.PAYROLL_EMAIL, AppConstants.PAYROLL_PASSWORD);
        String dirUrl = goAndStabilise(AppConstants.EMPLOYEE_DIR_URL);

        // FRD outcome 1: Payroll Manager is bounced from /employees outright.
        if (!dirUrl.contains("/employees")) {
            ExtentManager.getTest().pass(
                "Payroll Manager blocked at /employees route — FRD compliant");
            return;
        }

        // The eye-icon button per Rivio_Angular-main employee-directory.html
        // line 101 is: <button [routerLink]="['/employees', employee.id]"
        //                      pTooltip="View Profile"><i class="pi pi-eye"></i></button>
        // Angular renders <button ng-reflect-router-link="/employees,<id>">.
        // Use XPath to find the first row's eye-icon button without invalid CSS.
        java.util.List<org.openqa.selenium.WebElement> eyes = driver.findElements(
            By.xpath("//p-table//tbody/tr[1]//button[.//i[contains(@class,'pi-eye')]]"));
        if (eyes.isEmpty()) {
            // Fallback: any button containing the pi-eye icon anywhere in the table
            eyes = driver.findElements(
                By.xpath("//p-table//button[.//i[contains(@class,'pi-eye')]]"));
        }
        Assert.assertFalse(eyes.isEmpty(),
                "RV-BUG-NEW-09 setup: no eye-icon profile button rendered for Payroll "
              + "Manager on " + dirUrl + ". Cannot drill in to verify edit affordances.");

        WaitUtils.scrollAndClick(driver, eyes.get(0));
        WaitUtils.waitForAngularLoad(driver);
        String profileUrl = WaitUtils.waitForUrlToBeStable(driver);
        ExtentManager.getTest().info("Profile URL after eye-icon click: " + profileUrl);

        // FRD outcome 2: the role guard sends them away from /employees/:id.
        boolean reachedProfile = profileUrl.matches(".*/employees/\\d+.*");
        if (!reachedProfile) {
            ExtentManager.getTest().pass(
                "Payroll Manager blocked from /employees/:id — FRD compliant");
            return;
        }

        // FRD outcome 3: profile renders read-only — no pencil / Edit Details /
        // Edit Contact Info buttons should be visible.
        java.util.List<org.openqa.selenium.WebElement> editAffordances = driver.findElements(By.xpath(
            "//button[.//i[contains(@class,'pi-pencil')]] | "
          + "//button[contains(normalize-space(.),'Edit Details')] | "
          + "//button[contains(normalize-space(.),'Edit Contact')] | "
          + "//button[@title='Edit Contact Info']"
        ));

        Assert.assertTrue(editAffordances.isEmpty(),
                "RV-BUG-NEW-09: Payroll Manager opened employee profile " + profileUrl
              + " by clicking the eye icon, and the page exposes " + editAffordances.size()
              + " edit affordance(s) (pencil icon / 'Edit Details' / 'Edit Contact Info'). "
              + "FRD restricts view+edit of employee details to Admin, HR and Manager — "
              + "Payroll Manager must be denied at the route level or rendered a read-only view.");
        ExtentManager.getTest().pass(
            "Payroll Manager profile view is read-only — FRD compliant");
    }
}
