package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.DashboardPage;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.pages.RecruitmentDashboardPage;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * RecruitmentTest – REC-S-01 + REC-S-02.
 *
 *   RV_REC_001 – Kanban renders APPLIED/INTERVIEWING/OFFERED columns
 *   RV_REC_002 – "+ Add Sourced Candidate" button is visible
 *   RV_REC_003 – Job Openings tab renders
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

    /**
     * RV-BUG-NEW-07: A sourced candidate's name, department, and designation
     * are visible on the Kanban card (Applied/Interviewing/Offered), but when
     * the "Hire" action is triggered on an OFFERED candidate (or the Hire
     * Candidate button in the job applicants modal), only the email
     * propagates into the onboard modal.
     *
     * Per Rivio_Angular-main:
     *   - recruitment-dashboard.component.ts.hireCandidate(c) navigates to
     *     /employees with router state hireCandidateData = candidate
     *   - employee-onboard.ts.open(candidateData) calls patchValue with
     *     firstName, lastName, email — but the kanban candidate object only
     *     has a single `name` field, so firstName/lastName arrive undefined.
     *     Department and designation are never patched at all.
     *
     * This test triggers the Hire flow and asserts firstName, lastName,
     * department, designation are all populated in the onboard modal.
     */
    @Test(priority = 4, description =
        "RV_REC_BUG_07 – Onboard modal must auto-fill name, department, designation from kanban candidate")
    public void RV_REC_BUG_07_onboardAutofillFromSourcedCandidate() {
        recruitment.openKanbanTab();
        WaitUtils.hardWait(800);

        // 1. Try the kanban OFFERED-stage Hire button first.
        boolean clicked = clickFirstHireButton();

        // 2. Fallback: open the Job Openings tab → applicant modal → Hire Candidate
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
                "RV-BUG-NEW-07 setup: no Hire button reachable in recruitment. "
              + "Need at least one OFFERED candidate to exercise the autofill flow.");

        // Hire navigates to /employees with state, then opens the onboard dialog.
        WaitUtils.waitForUrlContains(driver, "/employees");
        WaitUtils.waitForPresence(driver, By.cssSelector(
            "p-dialog input[formcontrolname='firstName']"), 12);

        String firstName   = readFormControlValue("firstName");
        String lastName    = readFormControlValue("lastName");
        String department  = readDropdownLabel("departmentId");
        String designation = readDropdownLabel("designationId");

        ExtentManager.getTest().info("Autofill snapshot — firstName='" + firstName
            + "' lastName='" + lastName + "' departmentLabel='" + department
            + "' designationLabel='" + designation + "'");

        boolean filledName       = !isBlank(firstName) && !isBlank(lastName);
        boolean filledDepartment = !isBlank(department) && !department.toLowerCase().startsWith("select");
        boolean filledDesignation= !isBlank(designation) && !designation.toLowerCase().startsWith("select");

        Assert.assertTrue(filledName && filledDepartment && filledDesignation,
                "RV-BUG-NEW-07: Onboard modal did not auto-fill candidate data from the "
              + "kanban card. Only the email propagates through; name, department and "
              + "designation must also carry over from the sourced candidate. "
              + "Snapshot — firstName='" + firstName + "' lastName='" + lastName
              + "' department='" + department + "' designation='" + designation + "'.");
        ExtentManager.getTest().pass("Onboard modal auto-fills sourced candidate fields");
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

    /** Reads the visible label inside a p-select bound to a given formControlName. */
    private String readDropdownLabel(String formControlName) {
        try {
            WebElement sel = driver.findElement(By.cssSelector(
                "p-dialog p-select[formcontrolname='" + formControlName + "']"));
            return sel.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
