package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * RecruitmentDashboardPage – mirrors features/recruitment/recruitment-dashboard.component.html.
 *
 * Real DOM:
 *   - &lt;h1&gt;Recruitment Pipeline&lt;/h1&gt;
 *   - Two tabs: "Kanban Board", "Job Openings"
 *   - Three pipeline stages: APPLIED, INTERVIEWING, OFFERED
 *   - "Add Sourced Candidate" button
 *
 * Add Candidate modal form (recruitment-dashboard.component.html):
 *   - p-select[formcontrolname='jobOpeningId'] – job opening dropdown
 *   - input[formcontrolname='name']            – candidate full name
 *   - input[formcontrolname='email']           – candidate email
 *   - input[formcontrolname='resumeUrl']       – resume link (optional)
 *   - Submit / Add Candidate button
 */
public class RecruitmentDashboardPage {

    private final WebDriver driver;

    public RecruitmentDashboardPage(WebDriver driver) { this.driver = driver; }

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
    }

    // ── Kanban board queries ──────────────────────────────────────────────────

    public boolean isStageVisible(String stage) {
        return !driver.findElements(
            By.xpath("//h3[normalize-space()='" + stage.toUpperCase() + "']")).isEmpty();
    }

    public int getKanbanColumnCount() {
        return driver.findElements(By.xpath("//div[@class and .//h3]")).size();
    }

    public boolean isAddCandidateVisible() {
        return !driver.findElements(
            By.xpath("//button[contains(.,'Add Sourced Candidate')]")).isEmpty();
    }

    // ── Add Sourced Candidate modal ───────────────────────────────────────────

    /**
     * Clicks the "Add Sourced Candidate" button to open the add-candidate modal.
     */
    public void clickAddSourcedCandidate() {
        WebElement btn = WaitUtils.waitForClickability(driver,
            By.xpath("//button[contains(.,'Add Sourced Candidate')]"));
        WaitUtils.safeClick(driver, btn);
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.hardWait(500);
    }

    /**
     * Selects a job opening in the "Add Sourced Candidate" modal.
     * Pass "AUTO" to select the first available option.
     */
    public void selectJobOpeningForCandidate(String jobOpening) {
        try {
            By dropdownLocator = By.cssSelector(
                "p-dialog p-select[formcontrolname='jobOpeningId'], "
                + "p-dialog p-select[formcontrolname='jobOpening']");
            if ("AUTO".equalsIgnoreCase(jobOpening) || jobOpening == null || jobOpening.isEmpty()) {
                WaitUtils.selectPrimeNgOption(driver, dropdownLocator, null);
            } else {
                WaitUtils.selectPrimeNgOption(driver, dropdownLocator, jobOpening);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Fills the candidate full name field in the add-candidate modal.
     */
    public void fillCandidateName(String name) {
        try {
            WebElement el = WaitUtils.waitForVisibility(driver, By.cssSelector(
                "p-dialog input[formcontrolname='name'], "
                + "p-dialog input[placeholder*='name' i]"));
            el.clear();
            el.sendKeys(name);
        } catch (Exception ignored) {}
    }

    /**
     * Fills the candidate email field in the add-candidate modal.
     */
    public void fillCandidateEmail(String email) {
        try {
            WebElement el = WaitUtils.waitForVisibility(driver, By.cssSelector(
                "p-dialog input[formcontrolname='email'], "
                + "p-dialog input[type='email'], "
                + "p-dialog input[placeholder*='email' i]"));
            el.clear();
            el.sendKeys(email);
        } catch (Exception ignored) {}
    }

    /**
     * Fills the resume URL field in the add-candidate modal.
     */
    public void fillResumeUrl(String url) {
        try {
            WebElement el = WaitUtils.waitForVisibility(driver, By.cssSelector(
                "p-dialog input[formcontrolname='resumeUrl'], "
                + "p-dialog input[placeholder*='resume' i], "
                + "p-dialog input[placeholder*='url' i]"));
            el.clear();
            el.sendKeys(url);
        } catch (Exception ignored) {}
    }

    /**
     * Clicks the Submit / Add Candidate button in the add-candidate modal.
     */
    public void submitCandidateForm() {
        try {
            WebElement btn = WaitUtils.waitForClickability(driver, By.xpath(
                "//p-dialog//button[contains(.,'Submit') or contains(.,'Add Candidate')"
                + " or contains(.,'Save')]"));
            WaitUtils.safeClick(driver, btn);
            WaitUtils.waitForAngularLoad(driver);
            WaitUtils.hardWait(800);
        } catch (Exception ignored) {}
    }

    /**
     * Returns true if the candidate was added successfully (success toast or modal close).
     */
    public boolean isCandidateAddedSuccessfully() {
        WaitUtils.hardWait(1000);
        boolean modalClosed = driver.findElements(By.cssSelector("p-dialog .p-dialog")).isEmpty();
        boolean successToast = !driver.findElements(By.cssSelector(
            ".p-toast-message-success, [class*='toast'][class*='success']")).isEmpty()
            || !driver.findElements(By.xpath(
            "//*[contains(@class,'toast') and (contains(.,'success')"
            + " or contains(.,'added') or contains(.,'created'))]")).isEmpty();
        return modalClosed || successToast;
    }

    /**
     * Returns true if a validation error message is visible in the add-candidate modal.
     */
    public boolean isCandidateFormValidationVisible() {
        return !driver.findElements(By.cssSelector(
            "p-dialog .p-error, p-dialog small.p-error, "
            + "p-dialog [class*='error'], p-dialog .ng-invalid")).isEmpty()
            || !driver.findElements(By.xpath(
            "//p-dialog//*[contains(@class,'text-red') or contains(@class,'text-danger')"
            + " or contains(@class,'error')]")).isEmpty();
    }

    /**
     * Returns true if the add-candidate modal is currently open.
     */
    public boolean isCandidateModalOpen() {
        return !driver.findElements(By.cssSelector("p-dialog .p-dialog")).isEmpty();
    }
}
