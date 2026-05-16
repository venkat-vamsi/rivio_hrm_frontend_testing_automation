package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

/**
 * RecruitmentDashboardPage – Page Object for the Recruitment / ATS module.
 *
 * The ATS (Applicant Tracking System) lets HR post jobs, track candidates
 * through stages (Applied → Screened → Interviewed → Hired/Rejected).
 */
public class RecruitmentDashboardPage {

    private WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(css = ".page-title, h1, h2")
    private WebElement pageTitle;

    // Job opening cards / rows
    @FindBy(css = ".job-card, .job-opening-card, [class*='job-row'], table tbody tr")
    private List<WebElement> jobOpeningCards;

    // "Post New Job" / "Add Job Opening" button
    @FindBy(css = "button.add-job, button[class*='post-job'], "
                + "button:contains('Post'), button:contains('Add Job')")
    private WebElement addJobButton;

    // Stage / pipeline overview (Kanban columns or stat counts)
    @FindBy(css = ".stage-card, .pipeline-stage, [class*='stage-count']")
    private List<WebElement> stageCards;

    // Candidate count per stage badges
    @FindBy(css = ".candidate-count, .badge, [class*='count-badge']")
    private List<WebElement> candidateCounts;

    // Search input for candidates or jobs
    @FindBy(css = "input.search, input[placeholder*='Search' i]")
    private WebElement searchInput;

    // View candidates button per job
    @FindBy(css = "button.view-candidates, a[href*='candidates'], [class*='candidates-btn']")
    private List<WebElement> viewCandidateButtons;

    // Status filter
    @FindBy(css = "select.status-filter, select[name='status']")
    private WebElement statusFilter;

    // ── Constructor ───────────────────────────────────────────────────────────

    public RecruitmentDashboardPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public void clickAddJob() {
        WaitUtils.waitForClickability(driver, addJobButton);
        addJobButton.click();
    }

    public void searchJobs(String keyword) {
        WaitUtils.waitForVisibility(driver, searchInput);
        searchInput.clear();
        searchInput.sendKeys(keyword);
    }

    public void clickViewCandidates(int jobIndex) {
        WaitUtils.waitForClickability(driver, viewCandidateButtons.get(jobIndex));
        viewCandidateButtons.get(jobIndex).click();
    }

    public JobBoardPage goToJobBoard() {
        driver.findElement(
            By.xpath("//a[contains(text(),'Job Board') or contains(@href,'job-board')]")).click();
        return new JobBoardPage(driver);
    }

    // ── Verifications ─────────────────────────────────────────────────────────

    public boolean isPageLoaded() {
        WaitUtils.waitForVisibility(driver, pageTitle);
        return pageTitle.isDisplayed();
    }

    public int getJobOpeningCount() {
        return jobOpeningCards.size();
    }

    public int getStageCardCount() {
        return stageCards.size();
    }

    public String getStageCardText(int index) {
        return stageCards.get(index).getText().trim();
    }
}
