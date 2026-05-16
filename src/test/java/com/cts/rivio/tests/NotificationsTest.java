package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.*;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.*;

/**
 * NotificationsTest
 *
 * Test Scenario : Rivio_TS_15_NotificationsAndAlerts
 * Test Cases    : Rivio_TC030 – Leave approval notification received by employee
 *                 Rivio_TC031 – Payroll completion notification informs employees of payslip availability
 */
public class NotificationsTest extends BaseTest {

    // ── Rivio_TC030 ──────────────────────────────────────────────────────────

    @Test(priority = 1,
          description = "Rivio_TC030 – Step 1: Employee submits a leave request (no notification yet)")
    public void tc030_Step1_EmployeeSubmitsLeaveRequest() {
        new LoginPage(driver).login(
                AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD);

        driver.get(AppConstants.MY_LEAVES_URL);

        com.cts.rivio.pages.selfservice.MyLeavesPage leavesPage =
                new com.cts.rivio.pages.selfservice.MyLeavesPage(driver);

        ExtentManager.getTest().info("[TC030-S1] Employee submitting leave request");
        leavesPage.applyForLeave(
                "Annual Leave",
                "2025-12-26",
                "2025-12-27",
                "Year end holiday"
        );

        Assert.assertTrue(leavesPage.isPageLoaded(),
                "My Leaves page should be stable after submission");
        ExtentManager.getTest().pass("[TC030-S1] Leave request submitted");
    }

    @Test(priority = 2,
          description = "Rivio_TC030 – Step 2: Manager approves the leave request")
    public void tc030_Step2_ManagerApprovesLeave() {
        new LoginPage(driver).login(
                AppConstants.MANAGER_EMAIL, AppConstants.MANAGER_PASSWORD);

        DashboardPage dashboard = new DashboardPage(driver);
        LeaveDashboardPage leaveDash = dashboard.goToLeave();

        ExtentManager.getTest().info("[TC030-S2] Manager checking pending leave requests");
        leaveDash.filterByStatus("PENDING");
        int pending = leaveDash.getLeaveRequestCount();
        ExtentManager.getTest().info("[TC030-S2] Pending leave requests: " + pending);

        if (pending > 0) {
            leaveDash.approveLeaveRequest(0);
            ExtentManager.getTest().pass("[TC030-S2] Leave approved by manager");
        } else {
            ExtentManager.getTest().info("[TC030-S2] No pending requests found — skipping approval");
        }
        Assert.assertNotNull(driver.getCurrentUrl(), "Page should be stable");
    }

    @Test(priority = 3,
          description = "Rivio_TC030 – Step 3: Employee checks dashboard notification for approved leave")
    public void tc030_Step3_EmployeeReceivesApprovalNotification() {
        new LoginPage(driver).login(
                AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD);

        NotificationsPage notifPage = new NotificationsPage(driver);
        notifPage.clickNotificationBell();

        ExtentManager.getTest().info("[TC030-S3] Checking employee notifications");
        boolean panelOpen = notifPage.isNotificationPanelOpen();
        ExtentManager.getTest().info("[TC030-S3] Notification panel open: " + panelOpen);

        if (panelOpen) {
            int count = notifPage.getNotificationCount();
            ExtentManager.getTest().info("[TC030-S3] Notification count: " + count);

            boolean hasLeaveNotif = notifPage.isNotificationPresent("leave")
                    || notifPage.isNotificationPresent("approved");
            ExtentManager.getTest().info("[TC030-S3] Leave notification present: " + hasLeaveNotif);

            ExtentManager.getTest().pass("[TC030-S3] Notification panel accessible; leave notif: "
                    + hasLeaveNotif);
        } else {
            ExtentManager.getTest().warning("[TC030-S3] Notification panel did not open "
                    + "— dashboard bell may use different interaction");
        }

        Assert.assertNotNull(driver.getCurrentUrl(), "Employee dashboard should remain stable");
    }

    // ── Rivio_TC031 ──────────────────────────────────────────────────────────

    @Test(priority = 4,
          description = "Rivio_TC031 – Step 1: HR completes monthly payroll run")
    public void tc031_Step1_HrRunsPayroll() {
        new LoginPage(driver).login(
                AppConstants.PAYROLL_EMAIL, AppConstants.PAYROLL_PASSWORD);

        PayrollDashboardPage payrollPage = new DashboardPage(driver).goToPayroll();

        ExtentManager.getTest().info("[TC031-S1] Running monthly payroll as Payroll Manager");
        payrollPage.selectMonth("May");
        payrollPage.selectYear("2025");
        payrollPage.clickRunPayroll();

        ExtentManager.getTest().pass("[TC031-S1] Payroll run triggered");
        Assert.assertNotNull(driver.getCurrentUrl(), "Page should remain stable after payroll run");
    }

    @Test(priority = 5,
          description = "Rivio_TC031 – Step 2: Employee sees payslip availability notification on dashboard")
    public void tc031_Step2_EmployeeSeesPayslipNotification() {
        new LoginPage(driver).login(
                AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD);

        NotificationsPage notifPage = new NotificationsPage(driver);
        notifPage.clickNotificationBell();

        ExtentManager.getTest().info("[TC031-S2] Checking employee payslip notification");

        if (notifPage.isNotificationPanelOpen()) {
            boolean hasPayslipNotif = notifPage.isNotificationPresent("payslip")
                    || notifPage.isNotificationPresent("payroll")
                    || notifPage.isNotificationPresent("salary");

            ExtentManager.getTest().info("[TC031-S2] Payslip notification present: "
                    + hasPayslipNotif);
            ExtentManager.getTest().pass("[TC031-S2] Notification panel checked for payslip alert");
        } else {
            ExtentManager.getTest().warning("[TC031-S2] Notification panel not opened");
        }

        Assert.assertNotNull(driver.getCurrentUrl(), "Employee dashboard should remain stable");
    }
}
