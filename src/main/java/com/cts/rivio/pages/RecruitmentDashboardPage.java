package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * RecruitmentDashboardPage – mirrors features/recruitment/recruitment-dashboard/recruitment-dashboard.component.html.
 *
 * Real DOM:
 *   - <h1>Recruitment Pipeline</h1>
 *   - Two tabs: "Kanban Board", "Job Openings"
 *   - Three pipeline stages: APPLIED, INTERVIEWING, OFFERED
 *   - "Add Sourced Candidate" button
 */
public class RecruitmentDashboardPage {

    private final WebDriver driver;

    public RecruitmentDashboardPage(WebDriver driver) { this.driver = driver; }

    public boolean isPageLoaded() {
        return WaitUtils.waitForH1Text(driver, "Recruitment Pipeline", 15);
    }

    public void openKanbanTab()    { clickTab("Kanban Board"); }
    public void openJobOpeningsTab() { clickTab("Job Openings"); }

    private void clickTab(String label) {
        WebElement btn = WaitUtils.waitForClickability(driver,
            By.xpath("//button[normalize-space()='" + label + "' or contains(.,'" + label + "')]"));
        WaitUtils.safeClick(driver, btn);
        WaitUtils.waitForAngularLoad(driver);
    }

    public boolean isStageVisible(String stage) {
        return !driver.findElements(
            By.xpath("//h3[normalize-space()='" + stage.toUpperCase() + "']")).isEmpty();
    }

    public int getKanbanColumnCount() {
        return driver.findElements(By.xpath("//div[@class and .//h3]")).size();
    }

    public boolean isAddCandidateVisible() {
        return !driver.findElements(By.xpath("//button[contains(.,'Add Sourced Candidate')]")).isEmpty();
    }
}
