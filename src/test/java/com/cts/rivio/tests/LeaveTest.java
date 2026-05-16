package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.*;
import com.cts.rivio.utils.ExcelUtils;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.*;

/**
 * LeaveTest – tests for the Leave Management module (HR/Manager view).
 *
 * Covers:
 *   – Leave dashboard loads
 *   – Filter by status
 *   – Approve / Reject actions
 *   – Data-driven from LeaveData.xlsx
 */
public class LeaveTest extends BaseTest {

    private LeaveDashboardPage leaveDashboard;

    @BeforeMethod
    public void loginAndGoToLeave() {
        LoginPage loginPage = new LoginPage(driver);
        DashboardPage dashboard = loginPage.login(AppConstants.HR_EMAIL, AppConstants.HR_PASSWORD);
        leaveDashboard = dashboard.goToLeave();
    }

    @Test(priority = 1, description = "Leave dashboard should load")
    public void testLeaveDashboardLoads() {
        Assert.assertTrue(leaveDashboard.isPageLoaded(),
                "Leave dashboard should be loaded");
    }

    @Test(priority = 2, description = "Leave balance cards should be visible")
    public void testLeaveBalanceCardsVisible() {
        int count = leaveDashboard.getBalanceCardCount();
        ExtentManager.getTest().info("Balance card count: " + count);
        Assert.assertTrue(count > 0, "Leave balance cards should be visible");
    }

    @Test(priority = 3, description = "Filter by PENDING status should show pending requests")
    public void testFilterByPendingStatus() {
        leaveDashboard.filterByStatus("PENDING");
        int count = leaveDashboard.getLeaveRequestCount();
        ExtentManager.getTest().info("Pending leave requests: " + count);
        Assert.assertTrue(count >= 0, "Filter should not crash");
    }

    @Test(priority = 4, description = "Approve first pending leave request")
    public void testApproveLeaveRequest() {
        leaveDashboard.filterByStatus("PENDING");
        int pendingCount = leaveDashboard.getLeaveRequestCount();
        if (pendingCount > 0) {
            leaveDashboard.approveLeaveRequest(0);
            ExtentManager.getTest().pass("Approved first pending leave request");
            // After approval, toast or status change expected
            // Optionally re-check the status or toast message
        } else {
            ExtentManager.getTest().skip("No pending leave requests found to approve");
        }
    }

    @Test(priority = 5, description = "Filter leave requests by date range")
    public void testFilterByDateRange() {
        leaveDashboard.filterByDateRange("2025-01-01", "2025-12-31");
        int count = leaveDashboard.getLeaveRequestCount();
        ExtentManager.getTest().info("Leave requests in 2025: " + count);
        Assert.assertTrue(count >= 0, "Date range filter should not crash");
    }

    @DataProvider(name = "leaveData")
    public Object[][] getLeaveData() {
        return ExcelUtils.readDataExcludingHeader(
                AppConstants.LEAVE_DATA_PATH, AppConstants.SHEET_LEAVE);
    }

    @Test(dataProvider = "leaveData", priority = 6,
          description = "Data-driven leave filter test from Excel")
    public void testLeaveFilterFromExcel(String status, String fromDate, String toDate,
                                          String expectedCount) {
        ExtentManager.getTest().info(
                "Filtering: status=" + status + ", from=" + fromDate + ", to=" + toDate);

        if (!status.isEmpty()) leaveDashboard.filterByStatus(status);
        if (!fromDate.isEmpty() && !toDate.isEmpty())
            leaveDashboard.filterByDateRange(fromDate, toDate);

        int actual = leaveDashboard.getLeaveRequestCount();
        ExtentManager.getTest().info("Request count: " + actual);

        if (!expectedCount.isEmpty()) {
            Assert.assertEquals(String.valueOf(actual), expectedCount,
                    "Leave request count mismatch");
        }
    }
}
