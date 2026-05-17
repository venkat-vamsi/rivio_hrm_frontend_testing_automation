package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.DashboardPage;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.pages.RecruitmentDashboardPage;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * RecruitmentTest – REC-S-01 + REC-S-02.
 *
 *   RV_REC_001 – Kanban renders APPLIED/INTERVIEWING/OFFERED columns
 *   RV_REC_002 – "+ Add Sourced Candidate" button is visible
 *   RV_REC_003 – Job Openings tab renders
 *   RV_REC_004 – Applicant count live-updates (RV-BUG-007)
 */
public class RecruitmentTest extends BaseTest {

    private RecruitmentDashboardPage recruitment;

    @BeforeMethod
    public void loginAndOpenRecruitment() {
        DashboardPage dash = new LoginPage(driver)
                .login(AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD);
        recruitment = dash.goToRecruitment();
    }

    @Test(priority = 1, description = "RV_REC_001 – Kanban board shows three pipeline columns")
    public void RV_REC_001_kanbanColumns() {
        Assert.assertTrue(recruitment.isPageLoaded(),
                "Recruitment Pipeline page should be loaded");
        recruitment.openKanbanTab();
        Assert.assertTrue(recruitment.isStageVisible("APPLIED"),
                "APPLIED column should be visible");
        Assert.assertTrue(recruitment.isStageVisible("INTERVIEWING"),
                "INTERVIEWING column should be visible");
        Assert.assertTrue(recruitment.isStageVisible("OFFERED"),
                "OFFERED column should be visible");
        ExtentManager.getTest().pass("Kanban renders the three pipeline columns");
    }

    @Test(priority = 2, description = "RV_REC_002 – Add Sourced Candidate button is visible on Kanban tab")
    public void RV_REC_002_addSourcedCandidateButton() {
        recruitment.openKanbanTab();
        Assert.assertTrue(recruitment.isAddCandidateVisible(),
                "'Add Sourced Candidate' button should be present on the Kanban tab");
        ExtentManager.getTest().pass("Add Candidate button visible");
    }

    @Test(priority = 3, description = "RV_REC_003 – Job Openings tab renders")
    public void RV_REC_003_jobOpeningsTab() {
        recruitment.openJobOpeningsTab();
        Assert.assertTrue(driver.getCurrentUrl().contains("/ats"),
                "Job Openings tab should keep URL on /ats");
        ExtentManager.getTest().pass("Job Openings tab renders");
    }

    @Test(priority = 4, description = "RV_REC_004 – Applicant count live-updates after adding a candidate (mutating, skipped)")
    public void RV_REC_004_applicantCountLiveUpdate() {
        ExtentManager.getTest().skip(
            "Mutating: requires adding a candidate to verify the counter increments. "
            + "Defect log RV-BUG-007 says the counter does NOT live-update — run manually.");
        throw new org.testng.SkipException("Mutating test — not safe on shared demo backend");
    }
}
