package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.CompanyStructurePage;
import com.cts.rivio.pages.DashboardPage;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * CompanyStructureTest – CFG-S-01..CFG-S-04.
 *
 *   RV_CFG_001 – Six sub-sections accessible only to HR Admin
 *   RV_CFG_002 – Work Days toggle affects attendance rules
 *   RV_CFG_003 – Public holidays excluded from leave count (RV-BUG-008)
 *   RV_CFG_004 – Leave Type allotment changes apply at next cycle
 *
 * Only Super Admin has /company route access (per app.routes.ts).
 */
public class CompanyStructureTest extends BaseTest {

    private CompanyStructurePage company;

    @BeforeMethod
    public void loginAsAdminAndOpenCompany() {
        DashboardPage dash = new LoginPage(driver)
                .login(AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD);
        company = dash.goToCompany();
    }

    @Test(priority = 1, description = "RV_CFG_001 – Company Config exposes six sub-sections")
    public void RV_CFG_001_sixSubSectionsVisible() {
        Assert.assertTrue(company.isPageLoaded(),
                "Organization Structure page should be loaded");
        int found = company.countSubSections();
        Assert.assertEquals(found, 6,
                "Expected 6 sub-sections (Departments, Roles & Titles, Office Locations, "
                + "Work Days, Public Holidays, Leave Types). Found: " + found);
        ExtentManager.getTest().pass("All six sub-sections visible");
    }

    @Test(priority = 2, description = "RV_CFG_001 – Each sub-section is clickable")
    public void RV_CFG_001_subSectionsClickable() {
        String[] labels = {"Departments", "Roles & Titles", "Office Locations",
                           "Work Days", "Public Holidays", "Leave Types"};
        for (String l : labels) {
            company.clickSubSection(l);
            ExtentManager.getTest().info("Sub-section opened: " + l);
        }
        ExtentManager.getTest().pass("All sub-sections clickable");
    }

    @Test(priority = 3, description = "RV_CFG_002 – Work Days panel renders day toggles")
    public void RV_CFG_002_workDaysPanelRenders() {
        company.clickSubSection("Work Days");
        // Toggles + weekday labels render after a tick — wait explicitly.
        Assert.assertTrue(company.waitForWorkDayTogglesToRender(),
                "Work Days panel should render within 15s");
        // Either weekday names or toggle controls should be present
        int proof = driver.findElements(org.openqa.selenium.By.xpath(
            "//*[contains(@class,'p-toggleswitch') or contains(@class,'p-inputswitch') "
          + "or self::p-toggleswitch or contains(@class,'switch')] | "
          + "//*[normalize-space()='Monday' or normalize-space()='Tuesday' "
          + "or normalize-space()='Wednesday' or normalize-space()='Thursday' "
          + "or normalize-space()='Friday']")).size();
        Assert.assertTrue(proof >= 3,
                "Work Days panel should expose weekday toggles/labels. Got: " + proof);
        ExtentManager.getTest().pass("Work Days panel rendered (proof elements: " + proof + ")");
    }

    @Test(priority = 4, description = "RV_CFG_003 – Public holidays exclusion from leave count (mutating, skipped)")
    public void RV_CFG_003_holidaysExcludedFromLeave() {
        ExtentManager.getTest().skip(
            "Mutating: requires configuring a holiday, then applying + approving a leave spanning it. "
            + "Defect log RV-BUG-008 says the exclusion is NOT applied — run manually.");
        throw new org.testng.SkipException("Mutating test — not safe on shared demo backend");
    }

    @Test(priority = 5, description = "RV_CFG_004 – Leave Type allotment panel renders")
    public void RV_CFG_004_leaveTypeAllotmentPanel() {
        company.clickSubSection("Leave Types");
        // The Leave Types table should render with at least a header row
        int tables = driver.findElements(
            org.openqa.selenium.By.cssSelector("p-table, table")).size();
        Assert.assertTrue(tables >= 1,
                "Leave Types sub-section should render a table");
        ExtentManager.getTest().pass("Leave Types panel renders");
    }
}
