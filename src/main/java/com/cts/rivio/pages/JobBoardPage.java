package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * JobBoardPage – Page Object for the public-facing Job Board (/ats/job-board).
 *
 * The job board shows open positions that candidates can view and apply to.
 * This page is accessible even without login in some configurations.
 */
public class JobBoardPage {

    private WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(css = ".page-title, h1, h2")
    private WebElement pageTitle;

    // Job listing cards
    @FindBy(css = ".job-card, [class*='job-listing'], .job-item")
    private List<WebElement> jobCards;

    // Job titles within cards
    @FindBy(css = ".job-card .job-title, [class*='job-title'], h3.title")
    private List<WebElement> jobTitles;

    // Department badges on job cards
    @FindBy(css = ".job-card .department, [class*='job-dept']")
    private List<WebElement> jobDepartments;

    // Employment type badges (Full-time, Part-time, Contract)
    @FindBy(css = ".job-card .emp-type, [class*='employment-type'], .badge")
    private List<WebElement> employmentTypeBadges;

    // Search jobs input
    @FindBy(css = "input[placeholder*='Search' i], input.job-search")
    private WebElement searchInput;

    // Department filter dropdown
    @FindBy(css = "select.dept-filter, select[name='department']")
    private WebElement departmentFilter;

    // "Apply Now" buttons
    @FindBy(css = "button.apply-btn, button:contains('Apply'), a[class*='apply']")
    private List<WebElement> applyButtons;

    // "View Details" buttons
    @FindBy(css = "button.view-details, a[class*='details'], [class*='view-btn']")
    private List<WebElement> viewDetailsButtons;

    // No jobs found message
    @FindBy(css = ".no-jobs, .empty-state, [class*='no-results']")
    private WebElement noJobsMessage;

    // ── Constructor ───────────────────────────────────────────────────────────

    public JobBoardPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public void searchJobs(String keyword) {
        WaitUtils.waitForVisibility(driver, searchInput);
        searchInput.clear();
        searchInput.sendKeys(keyword);
    }

    public void clickApply(int jobIndex) {
        WaitUtils.waitForClickability(driver, applyButtons.get(jobIndex));
        applyButtons.get(jobIndex).click();
    }

    public void clickViewDetails(int jobIndex) {
        WaitUtils.waitForClickability(driver, viewDetailsButtons.get(jobIndex));
        viewDetailsButtons.get(jobIndex).click();
    }

    // ── Verifications ─────────────────────────────────────────────────────────

    public boolean isPageLoaded() {
        WaitUtils.waitForVisibility(driver, pageTitle);
        return pageTitle.isDisplayed();
    }

    public int getJobCount() {
        return jobCards.size();
    }

    public List<String> getAllJobTitles() {
        return jobTitles.stream().map(el -> el.getText().trim()).collect(Collectors.toList());
    }

    public boolean isJobPresent(String title) {
        return getAllJobTitles().stream().anyMatch(t -> t.equalsIgnoreCase(title));
    }

    public boolean isNoJobsMessageDisplayed() {
        try {
            return noJobsMessage.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }
}
