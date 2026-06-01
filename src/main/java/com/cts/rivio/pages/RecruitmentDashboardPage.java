package com.cts.rivio.pages;

import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

/**
 * RecruitmentDashboardPage – mirrors
 *   features/recruitment/recruitment-dashboard/recruitment-dashboard.component.html
 *
 * Real DOM (verified against the live Angular code):
 *   - &lt;h1&gt;Recruitment Pipeline&lt;/h1&gt;
 *   - Two tabs: "Kanban Board", "Job Openings"
 *   - Kanban pipeline stages: APPLIED, INTERVIEWING, OFFERED
 *
 * Add Candidate modal (header "Add Sourced Candidate"):
 *   - p-select[formcontrolname='jobOpeningId']   ← optionLabel="title", optionValue="id"
 *   - input[formcontrolname='name']
 *   - input[formcontrolname='email']
 *   - input[formcontrolname='resumeUrl']         ← optional
 *   - button "Add Candidate"                      ← disabled when candidateForm.invalid || isSubmitting
 *
 * New Job Requisition modal (header "Create Job Requisition"):
 *   - input[formcontrolname='title']
 *   - p-select[formcontrolname='departmentId']
 *   - p-select[formcontrolname='locationId']
 *   - button "Create Requisition"                 ← disabled when jobForm.invalid || isSubmitting
 *
 * Verification model (matches what HR actually does):
 *   - JOB: after submit, switch to Job Openings tab, type into the search
 *     field "Search open requisitions...", assert the new title appears in
 *     the table.
 *   - CANDIDATE: after submit, the new card appears in the APPLIED column
 *     of the Kanban board — there is NO search field for candidates, so we
 *     scan all candidate cards (they all live in the DOM, only some are
 *     scrolled out of view).
 */
public class RecruitmentDashboardPage {

    private final WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(xpath = "//button[contains(.,'Add Sourced Candidate')]")
    private WebElement addSourcedCandidateButton;

    @FindBy(xpath = "//button[contains(normalize-space(.),'New Requisition') "
                  + "or contains(normalize-space(.),'+ New')]")
    private WebElement newRequisitionButton;

    @FindBy(css = "p-dialog input[formcontrolname='name']")
    private List<WebElement> candidateModalSentinel;

    @FindBy(css = "p-dialog input[formcontrolname='title']")
    private List<WebElement> jobModalSentinel;

    @FindBy(css = "p-dialog input[formcontrolname='name']")
    private WebElement candidateNameInput;

    @FindBy(css = "p-dialog input[formcontrolname='email']")
    private WebElement candidateEmailInput;

    @FindBy(css = "p-dialog input[formcontrolname='resumeUrl']")
    private WebElement candidateResumeInput;

    @FindBy(css = "p-dialog input[formcontrolname='title']")
    private WebElement jobTitleInput;

    @FindBy(css = "p-dialog .p-error, p-dialog small.p-error, p-dialog [class*='error']")
    private List<WebElement> candidateValidationErrors;

    @FindBy(css = "input[placeholder='Search open requisitions...']")
    private WebElement jobSearchInput;

    @FindBy(css = "p-table tbody tr")
    private List<WebElement> jobOpeningRows;

    @FindBy(xpath = "//p-table//tbody//button[@title='Manage Applicants']")
    private List<WebElement> manageApplicantsButtons;

    // ── Constructor ───────────────────────────────────────────────────────────

    public RecruitmentDashboardPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Page load ─────────────────────────────────────────────────────────────

    public boolean isPageLoaded() {
        return WaitUtils.waitForH1Text(driver, "Recruitment Pipeline", 15);
    }

    // ── Tab navigation ────────────────────────────────────────────────────────

    public void openKanbanTab()      { clickTab("Kanban Board"); }
    public void openJobOpeningsTab() { clickTab("Job Openings"); }

    private void clickTab(String label) {
        WebElement btn = WaitUtils.waitForClickability(driver,
            By.xpath("//button[normalize-space()='" + label
                + "' or contains(.,'" + label + "')]"));
        WaitUtils.safeClick(driver, btn);
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.hardWait(400);
    }

    // ── Kanban board queries ──────────────────────────────────────────────────

    public boolean isStageVisible(String stage) {
        return !driver.findElements(
            By.xpath("//h3[normalize-space()='" + stage.toUpperCase() + "']")).isEmpty();
    }

    public boolean isAddCandidateVisible() {
        return !driver.findElements(
            By.xpath("//button[contains(.,'Add Sourced Candidate')]")).isEmpty();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ADD SOURCED CANDIDATE MODAL
    // ══════════════════════════════════════════════════════════════════════════

    public void clickAddSourcedCandidate() {
        WaitUtils.waitForClickability(driver,
            By.xpath("//button[contains(.,'Add Sourced Candidate')]"));
        WaitUtils.safeClick(driver, addSourcedCandidateButton);
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.hardWait(500);
    }

    public boolean isCandidateModalOpen() {
        return !candidateModalSentinel.isEmpty();
    }

    /**
     * Selects an open role in the candidate modal's job dropdown.
     * Pass "AUTO" / empty → first non-disabled option.
     */
    public void selectJobOpeningForCandidate(String jobOpening) {
        By dropdownLocator = By.cssSelector(
            "p-dialog p-select[formcontrolname='jobOpeningId']");
        try {
            if (isAuto(jobOpening)) {
                WaitUtils.selectPrimeNgOption(driver, dropdownLocator, null);
            } else {
                WaitUtils.selectPrimeNgOption(driver, dropdownLocator, jobOpening);
            }
            WaitUtils.hardWait(250);
        } catch (Exception e) {
            diag("selectJobOpeningForCandidate failed: " + e.getMessage());
        }
    }

    public void fillCandidateName(String name)  { jsType("name", name); }
    public void fillCandidateEmail(String email){ jsType("email", email); }
    public void fillResumeUrl(String url) {
        if (url == null || url.isEmpty()) return;
        jsType("resumeUrl", url);
    }

    /**
     * Submits the candidate modal using three escalating strategies (native →
     * JS click → form.requestSubmit). The component closes the modal on
     * success (no alert), so this method polls up to 6s for the modal to
     * disappear and returns true on success.
     */
    public boolean submitCandidateForm() {
        return submitDialogForm("Add Candidate", "p-dialog input[formcontrolname='name']");
    }

    public boolean isCandidateFormValidationVisible() {
        return !candidateValidationErrors.isEmpty()
            || !driver.findElements(By.cssSelector(
                "p-dialog input.ng-invalid.ng-touched, "
              + "p-dialog p-select.ng-invalid.ng-touched")).isEmpty();
    }

    /**
     * Returns the number of candidate cards visible in the APPLIED kanban
     * column whose text contains the given substring (typically the new
     * candidate's name). Cards live in the DOM regardless of scroll, so
     * findElements catches all matches.
     */
    public int countCandidateCardsInApplied(String nameOrEmail) {
        try {
            String xpath =
                  "//*[normalize-space(.)='APPLIED']"
                + "/ancestor::*[self::div][1]"   // climb to the column container
                + "//*[contains(normalize-space(.),'" + escapeXpath(nameOrEmail) + "')]";
            return driver.findElements(By.xpath(xpath)).size();
        } catch (Exception ignored) {
            return 0;
        }
    }

    /**
     * Fallback verification: any element on the kanban board containing the
     * candidate identifier (name or email). Used when the APPLIED-anchored
     * locator misses due to slightly different DOM structure.
     */
    public int countAnywhereOnKanban(String nameOrEmail) {
        try {
            return driver.findElements(By.xpath(
                "//*[contains(normalize-space(.),'"
              + escapeXpath(nameOrEmail) + "')]")).size();
        } catch (Exception ignored) {
            return 0;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CREATE JOB REQUISITION MODAL (Job Openings tab → "+ New Requisition")
    // ══════════════════════════════════════════════════════════════════════════

    public void clickNewRequisition() {
        WaitUtils.waitForClickability(driver, By.xpath(
            "//button[contains(normalize-space(.),'New Requisition') "
          + "or contains(normalize-space(.),'+ New')]"));
        WaitUtils.safeClick(driver, newRequisitionButton);
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.hardWait(500);
    }

    public boolean isJobModalOpen() {
        return !jobModalSentinel.isEmpty();
    }

    public void fillJobTitle(String title) { jsType("title", title); }

    public void selectJobDepartment(String dept) {
        By dropdownLocator = By.cssSelector(
            "p-dialog p-select[formcontrolname='departmentId']");
        try {
            if (isAuto(dept)) {
                WaitUtils.selectPrimeNgOption(driver, dropdownLocator, null);
            } else {
                WaitUtils.selectPrimeNgOption(driver, dropdownLocator, dept);
            }
            WaitUtils.hardWait(250);
        } catch (Exception e) {
            diag("selectJobDepartment failed: " + e.getMessage());
        }
    }

    public void selectJobLocation(String loc) {
        By dropdownLocator = By.cssSelector(
            "p-dialog p-select[formcontrolname='locationId']");
        try {
            if (isAuto(loc)) {
                WaitUtils.selectPrimeNgOption(driver, dropdownLocator, null);
            } else {
                WaitUtils.selectPrimeNgOption(driver, dropdownLocator, loc);
            }
            WaitUtils.hardWait(250);
        } catch (Exception e) {
            diag("selectJobLocation failed: " + e.getMessage());
        }
    }

    /** Returns true on success (modal closed within 6s). */
    public boolean submitJobForm() {
        return submitDialogForm("Create Requisition", "p-dialog input[formcontrolname='title']");
    }

    public boolean isJobFormValidationVisible() {
        return !driver.findElements(By.cssSelector(
            "p-dialog .p-error, p-dialog small.p-error, "
            + "p-dialog input.ng-invalid.ng-touched, "
            + "p-dialog p-select.ng-invalid.ng-touched")).isEmpty();
    }

    /**
     * Types {@code text} into the Job Openings search field and waits 800ms
     * for the p-table to filter. Returns the visible row count.
     */
    public int searchAndCountJobOpenings(String text) {
        try {
            WaitUtils.waitForVisibility(driver, jobSearchInput);
            jobSearchInput.clear();
            jobSearchInput.sendKeys(text);
            WaitUtils.hardWait(900);   // p-table contains-filter debounce
            return jobOpeningRows.size();
        } catch (Exception e) {
            diag("searchAndCountJobOpenings failed: " + e.getMessage());
            return 0;
        }
    }

    /** Clears the Job Openings search field. */
    public void clearJobSearch() {
        try {
            WaitUtils.waitForVisibility(driver, jobSearchInput);
            jobSearchInput.clear();
            WaitUtils.hardWait(400);
        } catch (Exception ignored) {}
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MANAGE APPLICANTS MODAL (Job Openings tab → applicant count button)
    // Used for moving candidates across pipeline stages.
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Iterates each "Manage Applicants" button in the Job Openings table,
     * opens the resulting applicants modal, and changes the stage of the
     * named candidate to {@code newStageLabel} (one of "Applied",
     * "Interviewing", "Offered", "Rejected").
     *
     * Returns true once the change is committed (the row's p-select shows
     * the new label). Closes the modal whether or not the candidate was
     * found in the current job.
     */
    public boolean moveCandidateStage(String candidateName, String newStageLabel) {
        openJobOpeningsTab();
        WaitUtils.hardWait(800);

        By manageBtns = By.xpath(
            "//p-table//tbody//button[@title='Manage Applicants']");
        List<WebElement> btns = driver.findElements(manageBtns);
        diag("moveCandidateStage: " + btns.size() + " Manage-Applicants buttons");

        for (int i = 0; i < btns.size(); i++) {
            try {
                WebElement btn = driver.findElements(manageBtns).get(i);
                WaitUtils.scrollAndClick(driver, btn);
                WaitUtils.hardWait(900);

                if (!isApplicantsModalOpen()) continue;

                if (changeCandidateStageInOpenModal(candidateName, newStageLabel)) {
                    closeApplicantsModal();
                    return true;
                }
                // Not found in this job — close and try the next one
                closeApplicantsModal();
                WaitUtils.hardWait(400);
            } catch (Exception e) {
                diag("moveCandidateStage iteration " + i + " failed: " + e.getMessage());
            }
        }
        diag("moveCandidateStage: candidate '" + candidateName + "' not found in any job");
        return false;
    }

    public boolean isApplicantsModalOpen() {
        return !driver.findElements(By.xpath(
            "//p-dialog//*[contains(text(),'Applicants:')]")).isEmpty();
    }

    /**
     * Inside an already-open Applicants modal, finds the row whose visible
     * text contains {@code candidateName}, then opens that row's stage
     * p-select and clicks the option matching {@code newStageLabel}.
     */
    private boolean changeCandidateStageInOpenModal(String candidateName, String newStageLabel) {
        // Find the row containing the candidate's name
        By rowLocator = By.xpath(
            "//p-dialog//tbody/tr["
          + "contains(normalize-space(.),'" + escapeXpath(candidateName) + "')]");
        List<WebElement> rows = driver.findElements(rowLocator);
        if (rows.isEmpty()) {
            diag("Candidate '" + candidateName + "' not in this Applicants modal");
            return false;
        }

        // Open the stage p-select inside that row
        try {
            WebElement select = rows.get(0).findElement(By.cssSelector("p-select"));
            WaitUtils.safeClick(driver, select);
            WaitUtils.hardWait(500);

            // Click the option whose label matches (e.g. "Interviewing")
            By optionLocator = By.xpath(
                  "//div[contains(@class,'p-select-overlay') "
                + "  or contains(@class,'p-dropdown-panel')]"
                + "//li[(contains(@class,'p-select-option') "
                + "  or contains(@class,'p-dropdown-item')) "
                + "  and normalize-space(.)='" + escapeXpath(newStageLabel) + "']");
            WebElement opt = WaitUtils.waitForClickability(driver, optionLocator);
            WaitUtils.safeClick(driver, opt);
            WaitUtils.hardWait(800);   // optimistic update commit + API roundtrip
            diag("Stage changed for '" + candidateName + "' → " + newStageLabel);
            return true;
        } catch (Exception e) {
            diag("Stage dropdown change failed: " + e.getMessage());
            return false;
        }
    }

    /** Closes the Applicants modal (header X / Cancel / Close button). */
    public void closeApplicantsModal() {
        try {
            List<WebElement> closes = driver.findElements(By.xpath(
                "//p-dialog//button[@aria-label='Close' "
              + "or contains(@class,'p-dialog-close-button') "
              + "or contains(@class,'p-dialog-header-icon')]"));
            if (!closes.isEmpty()) {
                WaitUtils.safeClick(driver, closes.get(0));
                WaitUtils.hardWait(400);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Counts candidate cards inside a given kanban column (APPLIED /
     * INTERVIEWING / OFFERED) whose text contains {@code candidateName}.
     */
    public int countCandidateCardsInColumn(String columnHeader, String candidateName) {
        try {
            String xpath =
                  "//*[normalize-space(.)='" + escapeXpath(columnHeader.toUpperCase()) + "']"
                + "/ancestor::*[self::div][1]"
                + "//*[contains(normalize-space(.),'" + escapeXpath(candidateName) + "')]";
            return driver.findElements(By.xpath(xpath)).size();
        } catch (Exception ignored) {
            return 0;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Shared submit logic — three escalating strategies + modal-close poll
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Shared submit for both Add Candidate and Create Requisition.
     *
     * @param btnText      e.g. "Add Candidate" / "Create Requisition"
     * @param sentinelCss  CSS of an input that is ONLY present while the
     *                     modal is open (used to detect modal close)
     * @return true when the modal closes within 6s, false otherwise.
     */
    private boolean submitDialogForm(String btnText, String sentinelCss) {
        By btnBy = By.xpath(
            "//p-dialog//button[normalize-space()='" + btnText
          + "' or contains(normalize-space(.),'" + btnText + "')]");

        // Wait up to 5s for the button to be enabled (form valid)
        long deadline = System.currentTimeMillis() + 5000;
        WebElement btn = null;
        while (System.currentTimeMillis() < deadline) {
            List<WebElement> btns = driver.findElements(btnBy);
            if (!btns.isEmpty()) {
                btn = btns.get(0);
                String disabled = btn.getAttribute("disabled");
                String cls = btn.getAttribute("class");
                boolean cssDisabled = cls != null && cls.contains("p-disabled");
                boolean attrDisabled = disabled != null && !disabled.isEmpty()
                                    && !"false".equalsIgnoreCase(disabled);
                if (!attrDisabled && !cssDisabled) {
                    diag("[" + btnText + "] enabled — disabled-attr=null, class=\"" + cls + "\"");
                    break;
                }
            }
            WaitUtils.hardWait(250);
            btn = null;
        }
        if (btn == null) {
            diag("[" + btnText + "] never enabled in 5s — form invalid");
            return false;
        }

        // Scroll into view
        try {
            ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
            WaitUtils.hardWait(200);
        } catch (Exception ignored) {}

        // Strategy A: native click
        try {
            btn.click();
            diag("[" + btnText + "] Strategy A native click executed");
        } catch (Exception e) {
            diag("[" + btnText + "] Strategy A failed: " + e.getMessage());
        }
        if (waitForModalGone(sentinelCss, 2500)) return true;

        // Strategy B: JS click
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
            diag("[" + btnText + "] Strategy B JS click executed");
        } catch (Exception e) {
            diag("[" + btnText + "] Strategy B failed: " + e.getMessage());
        }
        if (waitForModalGone(sentinelCss, 2500)) return true;

        // Strategy C: form.requestSubmit() — the W3C standard programmatic submit
        try {
            Object r = ((JavascriptExecutor) driver).executeScript(
                "var form = document.querySelector('p-dialog form');" +
                "if (!form) return 'no form';" +
                "if (typeof form.requestSubmit === 'function') {" +
                "  form.requestSubmit(); return 'requestSubmit';" +
                "}" +
                "form.dispatchEvent(new Event('submit', {bubbles:true,cancelable:true}));" +
                "return 'dispatchEvent';");
            diag("[" + btnText + "] Strategy C form.submit result: " + r);
        } catch (Exception e) {
            diag("[" + btnText + "] Strategy C failed: " + e.getMessage());
        }
        return waitForModalGone(sentinelCss, 3000);
    }

    /** Polls until the modal's sentinel selector is no longer present. */
    private boolean waitForModalGone(String sentinelCss, long maxMillis) {
        long deadline = System.currentTimeMillis() + maxMillis;
        while (System.currentTimeMillis() < deadline) {
            if (driver.findElements(By.cssSelector(sentinelCss)).isEmpty()) return true;
            WaitUtils.hardWait(250);
        }
        return driver.findElements(By.cssSelector(sentinelCss)).isEmpty();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════════

    /** Types via JS to bypass slow per-character sendKeys AND any readonly attrs. */
    private void jsType(String formControlName, String text) {
        try {
            WebElement el = WaitUtils.waitForVisibility(driver, By.cssSelector(
                "p-dialog input[formcontrolname='" + formControlName + "']"));
            WaitUtils.jsSetValue(driver, el, text);
        } catch (Exception e) {
            diag("jsType(" + formControlName + ") failed: " + e.getMessage());
        }
    }

    private static boolean isAuto(String s) {
        return s == null || s.isEmpty() || "AUTO".equalsIgnoreCase(s);
    }

    /** Escapes single quotes so values can be embedded in XPath string literals. */
    private static String escapeXpath(String s) {
        if (s == null) return "";
        return s.replace("'", "&apos;");
    }

    private void diag(String msg) {
        try { ExtentManager.getTest().info("[Recruitment] " + msg); } catch (Exception ignored) {}
        System.out.println("[Recruitment] " + msg);
    }
}
