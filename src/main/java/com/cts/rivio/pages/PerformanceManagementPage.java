package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

/**
 * PerformanceManagementPage – Page Object for the Performance Management module.
 *
 * Test Scenarios : Rivio_TS_11_PerformanceManagement
 * Test Cases     : Rivio_TC023 – Manager sets goals/KPIs for employee
 *                  Rivio_TC024 – Complete review cycle (self-review → manager rating → HR finalise)
 *
 * Sub-sections:
 *   1. Goal Setting      – Manager assigns KPIs/goals
 *   2. Self Review       – Employee submits self-assessment
 *   3. Manager Review    – Manager rates and gives feedback
 *   4. HR Finalisation   – HR archives the completed review
 */
public class PerformanceManagementPage {

    private WebDriver driver;

    // ── Locators – Page & Navigation ──────────────────────────────────────────

    @FindBy(css = ".page-title, h1, h2")
    private WebElement pageTitle;

    // Sub-tab navigation
    @FindBy(css = "[role='tab'], .tab-item, .nav-tab, .performance-tab")
    private List<WebElement> tabs;

    // ── Locators – Goal Setting (Manager view) ────────────────────────────────

    // List of direct reports for goal assignment
    @FindBy(css = ".employee-list .employee-item, [class*='reportee-row']")
    private List<WebElement> employeeList;

    // Goal title input
    @FindBy(css = "input[formcontrolname='goalTitle'], input[placeholder*='Goal' i]")
    private WebElement goalTitleInput;

    // KPI / target value input
    @FindBy(css = "input[formcontrolname='targetValue'], input[placeholder*='Target' i]")
    private WebElement targetValueInput;

    // Goal description / details textarea
    @FindBy(css = "textarea[formcontrolname='description'], textarea[placeholder*='Description' i]")
    private WebElement goalDescriptionInput;

    // Save Goal button
    @FindBy(css = "button.save-goal, button[type='submit'], .modal .btn-primary")
    private WebElement saveGoalButton;

    // Goals list for a selected employee
    @FindBy(css = ".goal-card, .goal-row, [class*='goal-item']")
    private List<WebElement> goalRows;

    // ── Locators – Self Review (Employee view) ────────────────────────────────

    @FindBy(css = "textarea[formcontrolname='selfReview'], textarea[placeholder*='self review' i]")
    private WebElement selfReviewTextarea;

    @FindBy(css = "button.submit-review, button[class*='submit-self']")
    private WebElement submitSelfReviewButton;

    // Review status label ("Pending Self Review", "Pending Manager Review", etc.)
    @FindBy(css = ".review-status, [class*='review-status'], .status-badge")
    private WebElement reviewStatusLabel;

    // ── Locators – Manager Review ─────────────────────────────────────────────

    // Rating input (1-5 stars or numeric)
    @FindBy(css = "input[formcontrolname='rating'], .star-rating input, select[name='rating']")
    private WebElement ratingInput;

    // Feedback textarea
    @FindBy(css = "textarea[formcontrolname='feedback'], textarea[placeholder*='Feedback' i]")
    private WebElement feedbackTextarea;

    @FindBy(css = "button.submit-manager-review, button[class*='submit-rating']")
    private WebElement submitManagerReviewButton;

    // ── Locators – HR Finalisation ────────────────────────────────────────────

    // Pending reviews list (HR view)
    @FindBy(css = ".pending-review-row, [class*='finalise-row'], table.reviews tbody tr")
    private List<WebElement> pendingReviewRows;

    @FindBy(css = "button.finalise, button[class*='finalise-review'], button:contains('Finalise')")
    private WebElement finaliseButton;

    // Success message
    @FindBy(css = ".toast-success, .alert-success, [class*='success']")
    private WebElement successMessage;

    // ── Constructor ───────────────────────────────────────────────────────────

    public PerformanceManagementPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    public void clickTab(String tabName) {
        for (WebElement tab : tabs) {
            if (tab.getText().trim().equalsIgnoreCase(tabName)) {
                WaitUtils.waitForClickability(driver, tab);
                tab.click();
                return;
            }
        }
    }

    // ── Goal Setting actions ──────────────────────────────────────────────────

    public void selectEmployee(int index) {
        WaitUtils.waitForClickability(driver, employeeList.get(index));
        employeeList.get(index).click();
    }

    public void enterGoalTitle(String title) {
        WaitUtils.waitForVisibility(driver, goalTitleInput);
        goalTitleInput.clear();
        goalTitleInput.sendKeys(title);
    }

    public void enterTargetValue(String value) {
        targetValueInput.clear();
        targetValueInput.sendKeys(value);
    }

    public void enterGoalDescription(String desc) {
        goalDescriptionInput.clear();
        goalDescriptionInput.sendKeys(desc);
    }

    public void clickSaveGoal() {
        WaitUtils.waitForClickability(driver, saveGoalButton);
        saveGoalButton.click();
    }

    // ── Self-Review actions ───────────────────────────────────────────────────

    public void enterSelfReview(String text) {
        WaitUtils.waitForVisibility(driver, selfReviewTextarea);
        selfReviewTextarea.clear();
        selfReviewTextarea.sendKeys(text);
    }

    public void submitSelfReview() {
        WaitUtils.waitForClickability(driver, submitSelfReviewButton);
        submitSelfReviewButton.click();
    }

    // ── Manager Review actions ────────────────────────────────────────────────

    public void enterRating(String rating) {
        WaitUtils.waitForVisibility(driver, ratingInput);
        // Handle both <input type="number"> and <select>
        try {
            new Select(ratingInput).selectByVisibleText(rating);
        } catch (Exception e) {
            ratingInput.clear();
            ratingInput.sendKeys(rating);
        }
    }

    public void enterFeedback(String feedback) {
        WaitUtils.waitForVisibility(driver, feedbackTextarea);
        feedbackTextarea.clear();
        feedbackTextarea.sendKeys(feedback);
    }

    public void submitManagerReview() {
        WaitUtils.waitForClickability(driver, submitManagerReviewButton);
        submitManagerReviewButton.click();
    }

    // ── HR Finalisation actions ───────────────────────────────────────────────

    public void finaliseReview(int rowIndex) {
        WebElement row = pendingReviewRows.get(rowIndex);
        WebElement btn = row.findElement(
            By.cssSelector("button.finalise, [class*='finalise']"));
        WaitUtils.waitForClickability(driver, btn);
        btn.click();
    }

    // ── Verifications ─────────────────────────────────────────────────────────

    public boolean isPageLoaded() {
        WaitUtils.waitForVisibility(driver, pageTitle);
        return pageTitle.isDisplayed();
    }

    public int getGoalCount() {
        return goalRows.size();
    }

    public String getReviewStatus() {
        try {
            WaitUtils.waitForVisibility(driver, reviewStatusLabel);
            return reviewStatusLabel.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public int getPendingReviewCount() {
        return pendingReviewRows.size();
    }

    public String getSuccessMessage() {
        WaitUtils.waitForVisibility(driver, successMessage);
        return successMessage.getText().trim();
    }
}
