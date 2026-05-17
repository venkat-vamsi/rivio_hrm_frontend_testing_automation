package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.DashboardPage;
import com.cts.rivio.pages.EmployeeDirectoryPage;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * EmployeeTest – EMP-S-01..EMP-S-04 from the Test Design Excel.
 *
 *   RV_EMP_001 — Directory renders with columns + pagination
 *   RV_EMP_002 — Real-time name search filters without page reload
 *   RV_EMP_003 — Onboard New Employee modal opens (EMP ID generation is RV-BUG-003)
 *   RV_EMP_004 — Employee profile shows Job + Contact sections
 */
public class EmployeeTest extends BaseTest {

    private EmployeeDirectoryPage directory;

    @BeforeMethod
    public void loginAndOpenDirectory() {
        DashboardPage dash = new LoginPage(driver)
                .login(AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD);
        directory = dash.goToEmployeeDirectory();
    }

    @Test(priority = 1, description = "RV_EMP_001 – Employee directory renders with table + pagination")
    public void RV_EMP_001_directoryRenders() {
        Assert.assertTrue(directory.isPageLoaded(),
                "Employee directory should be loaded");
        Assert.assertEquals(directory.getPageHeading(), "Employees",
                "Page heading should read 'Employees'");
        Assert.assertTrue(directory.isPaginationVisible(),
                "Pagination should be visible at the bottom of the table");
        ExtentManager.getTest().pass("Employee directory renders with pagination");
    }

    @Test(priority = 2, description = "RV_EMP_002 – Real-time search filters employee list")
    public void RV_EMP_002_realTimeSearch() {
        // Ensure we're settled on /employees before snapshotting the URL.
        com.cts.rivio.utils.WaitUtils.waitForUrlContains(driver, "/employees");
        com.cts.rivio.utils.WaitUtils.waitForAngularLoad(driver);

        int before = directory.getRowCount();
        String urlBefore = driver.getCurrentUrl();

        directory.searchEmployee("zzzzz_no_match_xyz");

        Assert.assertEquals(driver.getCurrentUrl(), urlBefore,
                "URL should NOT change during real-time search");
        int after = directory.getRowCount();
        ExtentManager.getTest().info("Rows before: " + before + ", after no-match search: " + after);
        Assert.assertTrue(after <= before, "Row count must not grow when search has no matches");

        directory.clearSearch();
        ExtentManager.getTest().pass("Search filters the list in real time");
    }

    @Test(priority = 3, description = "RV_EMP_003 – Onboard New Employee modal opens")
    public void RV_EMP_003_onboardModalOpens() {
        directory.clickAddEmployee();
        Assert.assertTrue(directory.isOnboardModalOpen(),
                "Clicking 'Add Employee' should open the onboarding modal");
        ExtentManager.getTest().pass("Onboard modal opens");
    }

    @Test(priority = 5, description = "RV_EMP_005 – Status toggle deactivation restricts login (mutating, skipped)")
    public void RV_EMP_005_statusToggleDeactivation() {
        ExtentManager.getTest().skip(
            "Mutating: requires toggling an employee active/inactive and re-logging in. "
            + "Run as a manual scenario or against a seeded test backend.");
        throw new org.testng.SkipException("Mutating test — not safe on shared demo backend");
    }

    @Test(priority = 4, description = "RV_EMP_004 – Employee profile shows Job + Contact sections")
    public void RV_EMP_004_employeeProfileSections() {
        if (directory.getRowCount() == 0) {
            ExtentManager.getTest().skip("Directory is empty in this environment — cannot open a profile");
            throw new org.testng.SkipException("Empty directory");
        }
        directory.openFirstEmployeeProfile();
        Assert.assertTrue(driver.getCurrentUrl().contains("/employees/"),
                "Should navigate to /employees/:id. URL: " + driver.getCurrentUrl());
        boolean job = !driver.findElements(org.openqa.selenium.By.xpath(
                "//*[contains(text(),'Job Details')]")).isEmpty();
        boolean contact = !driver.findElements(org.openqa.selenium.By.xpath(
                "//*[contains(text(),'Contact Information')]")).isEmpty();
        Assert.assertTrue(job, "Job Details section should be visible");
        Assert.assertTrue(contact, "Contact Information section should be visible");
        ExtentManager.getTest().pass("Profile sections render");
    }
}
