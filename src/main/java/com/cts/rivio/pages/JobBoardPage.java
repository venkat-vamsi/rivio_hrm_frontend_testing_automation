package com.cts.rivio.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * JobBoardPage – alias for the "Job Openings" tab inside the Recruitment dashboard.
 * (The Angular app does not expose a separate /ats/job-board route; "Job Openings"
 * is a tab inside /ats.)
 */
public class JobBoardPage {

    private final WebDriver driver;

    public JobBoardPage(WebDriver driver) { this.driver = driver; }

    public boolean isPageLoaded() {
        return driver.getCurrentUrl().contains("/ats");
    }

    public int getJobOpeningsCount() {
        return driver.findElements(By.cssSelector("p-table tbody tr")).size();
    }
}
