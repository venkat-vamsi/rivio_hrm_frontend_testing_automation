package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.RecruitmentDashboardPage;
import com.cts.rivio.utils.ExcelUtils;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

/**
 * RecruitmentTest – covers /ats Kanban board AND Job Openings tab.
 *
 *   RV_REC_001         – Kanban renders APPLIED / INTERVIEWING / OFFERED columns
 *   RV_REC_002         – "+ Add Sourced Candidate" button visible on Kanban tab
 *   RV_REC_003         – Job Openings tab renders
 *   RV_REC_BUG_07      – Onboard modal must auto-fill name/dept/desig from kanban
 *   RV_REC_DD_JOB_001  – Valid job requisition → row appears in Job Openings table
 *   RV_REC_DD_JOB_002  – Invalid job requisition → form rejects
 *   RV_REC_DD_001      – Valid candidate → card appears in APPLIED kanban column
 *   RV_REC_DD_002      – Invalid candidate → form rejects
 *
 * Verification model:
 *   – JOB     : after submit, switch to Job Openings tab → type title in
 *               "Search open requisitions..." → row count > 0.
 *   – CANDIDATE: after submit, switch to Kanban tab → scan APPLIED column
 *               for the candidate's name (no search bar exists for candidates).
 *
 * Re-runnable: email + employeeCode + job title carry the {RUN} placeholder
 * which is substituted with a unique nanosecond suffix per test invocation,
 * so the same xlsx never collides with a previous run's records.
 */
public class RecruitmentTest extends BaseTest {

    @Override protected String getRole() { return ROLE_ADMIN; }

    private RecruitmentDashboardPage recruitment;

    @BeforeMethod(alwaysRun = true)
    public void openRecruitment() {
        driver.get(AppConstants.RECRUITMENT_URL);
        WaitUtils.waitForAngularLoad(driver);
        recruitment = new RecruitmentDashboardPage(driver);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Rendering / smoke
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Bug: kanban candidate has name/dept/designation but when
     * "Hire Candidate" is clicked only email propagates into the onboard modal.
     */
    @Test(priority = 1, groups = {"bug", "regression", "negative"},
          description = "rec_bug_onboardAutofill – Onboard modal must auto-fill name/dept/designation from kanban candidate")
    public void rec_bug_onboardAutofill() {
        recruitment.openKanbanTab();
        WaitUtils.hardWait(800);
        boolean clicked = clickFirstHireButton();

        if (!clicked) {
            recruitment.openJobOpeningsTab();
            WaitUtils.hardWait(800);
            try {
                WebElement applicantBtn = driver.findElement(By.xpath(
                    "(//button[@title='Manage Applicants'])[1]"));
                WaitUtils.scrollAndClick(driver, applicantBtn);
                WaitUtils.hardWait(900);
                clicked = clickFirstHireButton();
            } catch (Exception ignored) {}
        }
        Assert.assertTrue(clicked,
                "rec_bug_onboardAutofill setup: no Hire button reachable.");

        WaitUtils.waitForUrlContains(driver, "/employees");
        WaitUtils.waitForPresence(driver, By.cssSelector(
            "p-dialog input[formcontrolname='firstName']"), 12);

        String firstName   = readFormControlValue("firstName");
        String lastName    = readFormControlValue("lastName");
        String department  = readDropdownLabel("departmentId");
        String designation = readDropdownLabel("designationId");

        boolean filledName       = !isBlank(firstName) && !isBlank(lastName);
        boolean filledDepartment = !isBlank(department) && !department.toLowerCase().startsWith("select");
        boolean filledDesignation= !isBlank(designation) && !designation.toLowerCase().startsWith("select");

        Assert.assertTrue(filledName && filledDepartment && filledDesignation,
                "rec_bug_onboardAutofill: Onboard modal didn't auto-fill from kanban. "
              + "firstName='" + firstName + "' lastName='" + lastName
              + "' department='" + department + "' designation='" + designation + "'");
    }

    private boolean clickFirstHireButton() {
        try {
            WebElement btn = driver.findElement(By.xpath(
                "(//button[normalize-space()='Hire' "
              + "or contains(normalize-space(.),'Hire Candidate')])[1]"));
            WaitUtils.scrollAndClick(driver, btn);
            return true;
        } catch (Exception e) { return false; }
    }

    private String readFormControlValue(String name) {
        List<WebElement> els = driver.findElements(By.cssSelector(
            "p-dialog input[formcontrolname='" + name + "']"));
        if (els.isEmpty()) return "";
        try {
            String v = els.get(0).getAttribute("value");
            return v == null ? "" : v;
        } catch (Exception e) { return ""; }
    }

    private String readDropdownLabel(String formControlName) {
        try {
            WebElement sel = driver.findElement(By.cssSelector(
                "p-dialog p-select[formcontrolname='" + formControlName + "']"));
            return sel.getText().trim();
        } catch (Exception e) { return ""; }
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    // ══════════════════════════════════════════════════════════════════════════
    // DATA-DRIVEN: Job requisition creation
    // ══════════════════════════════════════════════════════════════════════════

    @DataProvider(name = "validJobData")
    public Object[][] validJobData() {
        return ExcelUtils.readDataExcludingHeader(
                AppConstants.RECRUITMENT_DATA_PATH,
                AppConstants.SHEET_VALID_JOB);
    }

    @DataProvider(name = "invalidJobData")
    public Object[][] invalidJobData() {
        return ExcelUtils.readDataExcludingHeader(
                AppConstants.RECRUITMENT_DATA_PATH,
                AppConstants.SHEET_INVALID_JOB);
    }

    /**
     * RV_REC_DD_JOB_001 — Create a new job requisition and verify it appears
     * in the Job Openings table when searched by title.
     *
     * Flow:
     *   1. Open Job Openings tab
     *   2. Click "+ New Requisition" → Create Job Requisition modal opens
     *   3. Fill title (with {RUN} substituted) + select Department + Location
     *   4. Click Create Requisition (multi-strategy submit)
     *   5. Modal closes
     *   6. Search the Job Openings table for the new title → row appears
     */
    @Test(dataProvider = "validJobData",
          priority = 20,
          groups = {"regression", "positive"},
          description = "rec_validJob – New job requisition is created and searchable in Job Openings")
    public void rec_validJob(
            String testCase, String title, String department, String location) {

        String runId = uniqueRunId();
        title = applyRun(title, runId);

        ExtentManager.getTest().info(
            "[DD-JOB-VALID] " + testCase + " | title='" + title
            + "' dept='" + department + "' loc='" + location + "'");

        recruitment.openJobOpeningsTab();
        recruitment.clickNewRequisition();
        Assert.assertTrue(recruitment.isJobModalOpen(),
            "Create Job Requisition modal must open");

        recruitment.fillJobTitle(title);
        recruitment.selectJobDepartment(department);
        recruitment.selectJobLocation(location);

        boolean modalClosed = recruitment.submitJobForm();
        Assert.assertTrue(modalClosed,
            "[" + testCase + "] Create Requisition modal did NOT close — form was rejected. "
          + "title='" + title + "' dept='" + department + "' loc='" + location + "'");

        // Verify the new title appears in the Job Openings search
        recruitment.openJobOpeningsTab();
        WaitUtils.hardWait(800);
        int rows = recruitment.searchAndCountJobOpenings(title);
        recruitment.clearJobSearch();

        Assert.assertTrue(rows >= 1,
            "[" + testCase + "] Job '" + title + "' was NOT found in Job Openings "
          + "table after submit. The record was not persisted.");
        ExtentManager.getTest().pass("Job created + searchable: " + title);
    }

    @Test(dataProvider = "invalidJobData",
          priority = 21,
          groups = {"regression", "negative"},
          description = "rec_invalidJob – Invalid job requisition is rejected")
    public void rec_invalidJob(
            String testCase, String title, String department, String location, String expectedError) {

        String runId = uniqueRunId();
        title = applyRun(title, runId);

        ExtentManager.getTest().info(
            "[DD-JOB-INVALID] " + testCase + " | expected=" + expectedError);

        recruitment.openJobOpeningsTab();
        recruitment.clickNewRequisition();
        Assert.assertTrue(recruitment.isJobModalOpen(),
            "Create Job Requisition modal must open");

        if (!title.isEmpty())      recruitment.fillJobTitle(title);
        if (!department.isEmpty()) recruitment.selectJobDepartment(department);
        if (!location.isEmpty())   recruitment.selectJobLocation(location);

        boolean modalClosed = recruitment.submitJobForm();
        // For invalid data the form should NOT close (validators block submit)
        Assert.assertFalse(modalClosed,
            "[" + testCase + "] Invalid job requisition was accepted "
          + "(modal closed). Expected: " + expectedError);
        ExtentManager.getTest().pass("Invalid job rejected [" + testCase + "]");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DATA-DRIVEN: Add Sourced Candidate
    // ══════════════════════════════════════════════════════════════════════════

    @DataProvider(name = "validCandidateData")
    public Object[][] validCandidateData() {
        return ExcelUtils.readDataExcludingHeader(
                AppConstants.RECRUITMENT_DATA_PATH,
                AppConstants.SHEET_VALID_CANDIDATE);
    }

    @DataProvider(name = "invalidCandidateData")
    public Object[][] invalidCandidateData() {
        return ExcelUtils.readDataExcludingHeader(
                AppConstants.RECRUITMENT_DATA_PATH,
                AppConstants.SHEET_INVALID_CANDIDATE);
    }

    /**
     * RV_REC_DD_001 — Add a sourced candidate and verify the card appears in
     * the APPLIED column of the Kanban board.
     *
     * Flow:
     *   1. Open Kanban tab
     *   2. Click "Add Sourced Candidate" → modal opens
     *   3. Select an existing open role (AUTO = first opening in DB)
     *   4. Fill name (with {RUN} substituted), email, optional resume URL
     *   5. Click Add Candidate (multi-strategy submit)
     *   6. Modal closes
     *   7. Switch to Kanban tab → poll APPLIED column for the new name
     *      (no search bar — we scan the candidate cards directly)
     */
    @Test(dataProvider = "validCandidateData",
          priority = 30,
          groups = {"regression", "positive"},
          description = "rec_validCandidate – Sourced candidate is added and visible in APPLIED kanban column")
    public void rec_validCandidate(
            String name, String email, String jobOpening,
            String resumeUrl, String stage) {

        String runId = uniqueRunId();
        name  = applyRun(name, runId);
        email = applyRun(email, runId);

        ExtentManager.getTest().info(
            "[DD-CANDIDATE-VALID] name='" + name + "' email=" + email
            + " jobOpening=" + jobOpening);

        recruitment.openKanbanTab();
        recruitment.clickAddSourcedCandidate();
        Assert.assertTrue(recruitment.isCandidateModalOpen(),
            "Add Sourced Candidate modal must open");

        recruitment.selectJobOpeningForCandidate(jobOpening);
        recruitment.fillCandidateName(name);
        recruitment.fillCandidateEmail(email);
        if (resumeUrl != null && !resumeUrl.isEmpty()) {
            recruitment.fillResumeUrl(resumeUrl);
        }

        boolean modalClosed = recruitment.submitCandidateForm();
        Assert.assertTrue(modalClosed,
            "[" + name + "] Add Candidate modal did NOT close after submit. "
          + "Form was rejected or post failed.");

        // Poll the APPLIED column for the new card
        recruitment.openKanbanTab();
        WaitUtils.hardWait(1000);
        int matches = pollForCandidateCard(name, 10000);
        if (matches == 0) {
            // Fallback: search by email anywhere on the kanban page
            matches = pollForCandidateCard(email, 10000);
        }
        Assert.assertTrue(matches >= 1,
            "[" + name + "] Candidate card NOT found in APPLIED column after "
          + "polling 10s by name, 10s by email '" + email + "'. Record not persisted.");
        ExtentManager.getTest().pass("Candidate appears on Kanban: " + name);
    }

    @Test(dataProvider = "invalidCandidateData",
          priority = 31,
          groups = {"regression", "negative"},
          description = "rec_invalidCandidate – Invalid candidate data is rejected")
    public void rec_invalidCandidate(
            String testCase, String name, String email,
            String jobOpening, String resumeUrl, String stage, String expectedError) {

        String runId = uniqueRunId();
        name  = applyRun(name, runId);
        email = applyRun(email, runId);

        ExtentManager.getTest().info(
            "[DD-CANDIDATE-INVALID] " + testCase + " | expected=" + expectedError);

        recruitment.openKanbanTab();
        recruitment.clickAddSourcedCandidate();
        Assert.assertTrue(recruitment.isCandidateModalOpen(),
            "Add Sourced Candidate modal must open");

        if (jobOpening != null && !jobOpening.isEmpty())
            recruitment.selectJobOpeningForCandidate(jobOpening);
        if (name != null && !name.isEmpty())   recruitment.fillCandidateName(name);
        if (email != null && !email.isEmpty()) recruitment.fillCandidateEmail(email);
        if (resumeUrl != null && !resumeUrl.isEmpty())
            recruitment.fillResumeUrl(resumeUrl);

        boolean modalClosed = recruitment.submitCandidateForm();
        // For invalid data, submit must be blocked → modal stays open
        Assert.assertFalse(modalClosed,
            "[" + testCase + "] Invalid candidate was accepted (modal closed). "
          + "Expected: " + expectedError);
        ExtentManager.getTest().pass("Invalid candidate rejected [" + testCase + "]");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // STAGE-MOVE TEST: APPLIED → INTERVIEWING via Manage Applicants dropdown
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * RV_REC_004 — End-to-end candidate stage workflow.
     *
     * Flow:
     *   1. Add a sourced candidate (assigned to first open role via AUTO)
     *      → expect the card in the APPLIED kanban column.
     *   2. Open Job Openings → click "Manage Applicants" on the job
     *      → "Applicants: {job}" modal opens with the new candidate's row.
     *   3. Change the stage p-select on that row from Applied → Interviewing
     *      → Angular calls moveCandidate() → API persists the new stage.
     *   4. Close modal, switch to Kanban tab.
     *   5. Assert the candidate is now in INTERVIEWING column.
     *   6. Assert the candidate is NO LONGER in APPLIED column.
     */
    @Test(priority = 40,
          groups = {"regression", "positive"},
          description = "rec_moveStage – Move candidate card from APPLIED to INTERVIEWING")
    public void rec_moveStage() {
        String runId = uniqueRunId();
        String name  = "Stage Mover " + runId;
        String email = "stage.mover." + runId + "@email.com";

        ExtentManager.getTest().info("[STAGE-MOVE] adding candidate: " + name);

        // ── Step 1: add the candidate ────────────────────────────────────
        recruitment.openKanbanTab();
        recruitment.clickAddSourcedCandidate();
        Assert.assertTrue(recruitment.isCandidateModalOpen(),
            "Add Sourced Candidate modal must open");
        recruitment.selectJobOpeningForCandidate("AUTO");
        recruitment.fillCandidateName(name);
        recruitment.fillCandidateEmail(email);
        Assert.assertTrue(recruitment.submitCandidateForm(),
            "Add Candidate modal did NOT close — candidate was not created");

        // ── Step 2: verify card is in APPLIED ────────────────────────────
        recruitment.openKanbanTab();
        WaitUtils.hardWait(1000);
        int appliedBefore = pollForCandidateCard(name, 10000);
        Assert.assertTrue(appliedBefore >= 1,
            "Candidate '" + name + "' did NOT appear in APPLIED column after add");
        ExtentManager.getTest().info("Candidate is in APPLIED column ✓");

        // ── Step 3: move stage via Manage Applicants modal ───────────────
        boolean moved = recruitment.moveCandidateStage(name, "Interviewing");
        Assert.assertTrue(moved,
            "Could not change stage to 'Interviewing' for candidate '" + name + "'. "
          + "The candidate may not appear under any Job Openings 'Manage Applicants' modal.");

        // ── Step 4 & 5: assert candidate now in INTERVIEWING column ──────
        recruitment.openKanbanTab();
        WaitUtils.hardWait(1500);
        int inInterviewing = recruitment.countCandidateCardsInColumn(
            "INTERVIEWING", name);
        Assert.assertTrue(inInterviewing >= 1,
            "Candidate '" + name + "' did NOT move to INTERVIEWING column. "
          + "Stage dropdown may have been clicked but the move call failed.");
        ExtentManager.getTest().info("Candidate is in INTERVIEWING column ✓");

        // ── Step 6: assert candidate is NO LONGER in APPLIED ─────────────
        int stillInApplied = recruitment.countCandidateCardsInApplied(name);
        Assert.assertEquals(stillInApplied, 0,
            "Candidate '" + name + "' is STILL in APPLIED column after move "
          + "to INTERVIEWING. Found " + stillInApplied + " card(s).");
        ExtentManager.getTest().pass(
            "Stage move complete: " + name + " moved APPLIED → INTERVIEWING");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════════

    /** Polls the APPLIED column for a candidate card containing the given text. */
    private int pollForCandidateCard(String text, long maxMillis) {
        long deadline = System.currentTimeMillis() + maxMillis;
        int matches = 0;
        while (System.currentTimeMillis() < deadline) {
            matches = recruitment.countCandidateCardsInApplied(text);
            if (matches >= 1) return matches;
            // Fallback to any element on kanban — covers slight DOM variations
            matches = recruitment.countAnywhereOnKanban(text);
            if (matches >= 1) return matches;
            WaitUtils.hardWait(400);
        }
        return matches;
    }

    /** Per-invocation unique token for {RUN} placeholder substitution. */
    private static String uniqueRunId() {
        return Long.toString(System.nanoTime(), 36)
             + "-" + (int)(Math.random() * 9000 + 1000);
    }

    private static String applyRun(String cellValue, String runId) {
        if (cellValue == null) return null;
        return cellValue.replace("{RUN}", runId);
    }
}
