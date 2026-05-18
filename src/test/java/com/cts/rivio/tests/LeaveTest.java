package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.LeaveDashboardPage;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * LeaveTest – LVE-S-01 + LVE-S-02 (admin side).
 *
 *   RV_LVE_001 – Leave Approvals page renders with ACTION REQUIRED badge and pending table
 *   RV_LVE_002 – Review modal opens for pending requests (if any)
 *
 * The deduction flow depends on actual leave data; here we cover the UI
 * invariants that hold regardless of the dataset.
 */
public class LeaveTest extends BaseTest {

    @Override protected String getRole() { return ROLE_ADMIN; }

    private LeaveDashboardPage leave;

    @BeforeMethod
    public void openLeave() {
        // Bucket session is already logged in as Admin via BaseTest @BeforeClass.
        driver.get(AppConstants.LEAVE_URL);
        com.cts.rivio.utils.WaitUtils.waitForAngularLoad(driver);
        leave = new LeaveDashboardPage(driver);
    }

    @Test(priority = 1, description = "RV_LVE_001 – Leave Approvals page renders")
    public void RV_LVE_001_leaveApprovalsPageRenders() {
        Assert.assertTrue(leave.isPageLoaded(),
                "Leave Approvals page should be loaded");
        Assert.assertTrue(leave.isActionRequiredBadgeVisible(),
                "'Action Required' label should be visible above the pending table");
        ExtentManager.getTest().info("Pending request rows: " + leave.getLeaveRequestCount());
        ExtentManager.getTest().pass("Leave Approvals page renders");
    }

    @Test(priority = 2, description = "RV_LVE_002 – Review modal opens for pending requests (if any)")
    public void RV_LVE_002_reviewModalOpens() {
        if (leave.getLeaveRequestCount() == 0 || !leave.hasReviewButton()) {
            ExtentManager.getTest().info("No pending requests in current environment — skipping modal open");
            return;
        }
        leave.clickFirstReview();
        Assert.assertTrue(leave.isReviewModalOpen(),
                "Review modal should open on clicking Review");
        ExtentManager.getTest().pass("Review modal opens");
    }

}
