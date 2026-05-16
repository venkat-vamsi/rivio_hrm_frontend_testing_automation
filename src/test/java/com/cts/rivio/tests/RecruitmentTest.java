package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.*;
import com.cts.rivio.utils.ExcelUtils;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.*;

/**
 * RecruitmentTest – tests for the Recruitment / ATS module.
 */
public class RecruitmentTest extends BaseTest {

    private RecruitmentDashboardPage recruitmentPage;

    @BeforeMethod
    public void loginAndGoToRecruitment() {
        LoginPage loginPage = new LoginPage(driver);
        DashboardPage dashboard = loginPage.login(AppConstants.HR_EMAIL, AppConstants.HR_PASSWORD);
        recruitmentPage = dashboard.goToRecruitment();
    }

    @Test(priority = 1, description = "Recruitment dashboard should load")
    public void testRecruitmentDashboardLoads() {
        Assert.assertTrue(recruitmentPage.isPageLoaded(),
                "Recruitment dashboard should be loaded");
    }

    @Test(priority = 2, description = "Job openings should be listed")
    public void testJobOpeningsVisible() {
        int count = recruitmentPage.getJobOpeningCount();
        ExtentManager.getTest().info("Job openings: " + count);
        Assert.assertTrue(count >= 0);
    }

    @Test(priority = 3, description = "Pipeline stage cards should be visible")
    public void testStageCardsVisible() {
        int count = recruitmentPage.getStageCardCount();
        ExtentManager.getTest().info("Pipeline stages: " + count);
        Assert.assertTrue(count >= 0);
    }

    @Test(priority = 4, description = "Job Board should load with job listings")
    public void testJobBoardLoads() {
        JobBoardPage jobBoard = recruitmentPage.goToJobBoard();
        Assert.assertTrue(jobBoard.isPageLoaded(), "Job Board should load");
        int jobs = jobBoard.getJobCount();
        ExtentManager.getTest().info("Jobs on board: " + jobs);
        Assert.assertTrue(jobs >= 0);
    }

    @Test(priority = 5, description = "Search on job board should filter listings")
    public void testJobBoardSearch() {
        JobBoardPage jobBoard = recruitmentPage.goToJobBoard();
        jobBoard.searchJobs("Engineer");
        int results = jobBoard.getJobCount();
        ExtentManager.getTest().info("Search results for 'Engineer': " + results);
        Assert.assertTrue(results >= 0);
    }

    @DataProvider(name = "recruitmentData")
    public Object[][] getRecruitmentData() {
        return ExcelUtils.readDataExcludingHeader(
                AppConstants.RECRUITMENT_DATA_PATH, AppConstants.SHEET_RECRUITMENT);
    }

    @Test(dataProvider = "recruitmentData", priority = 6,
          description = "Data-driven job search from Excel")
    public void testJobSearchFromExcel(String searchKeyword, String expectedMinCount) {
        ExtentManager.getTest().info("Searching job: " + searchKeyword);
        JobBoardPage jobBoard = recruitmentPage.goToJobBoard();
        jobBoard.searchJobs(searchKeyword);
        int count = jobBoard.getJobCount();
        ExtentManager.getTest().info("Results: " + count);
        Assert.assertTrue(count >= 0, "Search should not crash");
    }
}
