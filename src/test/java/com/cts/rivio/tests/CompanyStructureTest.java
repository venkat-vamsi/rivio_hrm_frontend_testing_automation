package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.CompanyStructurePage;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * CompanyStructureTest – CFG-S-01..CFG-S-04.
 *
 *   RV_CFG_001 – Six sub-sections visible and clickable
 *   RV_CFG_002 – Work Days panel renders day cards
 *   RV_CFG_004 – Leave Types panel renders a table
 *
 * Per Rivio_Angular-main/app.routes.ts only Super Admin has /company access,
 * so login uses ADMIN_EMAIL ("Super Admin" role). Navigation is direct via
 * driver.get(...) to avoid sidebar timing flakes from cascading skips.
 */
public class CompanyStructureTest extends BaseTest {

    @Override protected String getRole() { return ROLE_ADMIN; }

    private CompanyStructurePage company;

    @BeforeMethod(alwaysRun = true)
    public void openCompany() {
        // Bucket session is already logged in as Admin via BaseTest @BeforeClass.
        driver.get(AppConstants.COMPANY_URL);
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.waitForUrlToBeStable(driver);
        company = new CompanyStructurePage(driver);
        Assert.assertTrue(company.isPageLoaded(),
                "Organization Structure page must load for Super Admin");
    }

    @Test(priority = 1, groups = {"smoke", "regression"}, description = "RV_CFG_001 – Company Config exposes six sub-sections")
    public void RV_CFG_001_sixSubSectionsVisible() {
        int found = company.countSubSections();
        Assert.assertEquals(found, 6,
                "Expected 6 sub-sections (Departments, Roles & Titles, Office Locations, "
                + "Work Days, Public Holidays, Leave Types). Found: " + found);
        ExtentManager.getTest().pass("All six sub-sections visible");
    }

    @Test(priority = 2, groups = {"regression"}, description = "RV_CFG_001 – Each sub-section is clickable")
    public void RV_CFG_001_subSectionsClickable() {
        String[] labels = {"Departments", "Roles & Titles", "Office Locations",
                           "Work Days", "Public Holidays", "Leave Types"};
        for (String l : labels) {
            company.clickSubSection(l);
            ExtentManager.getTest().info("Sub-section opened: " + l);
        }
        ExtentManager.getTest().pass("All sub-sections clickable");
    }

    @Test(priority = 3, groups = {"regression"}, description = "RV_CFG_002 – Work Days panel renders day cards")
    public void RV_CFG_002_workDaysPanelRenders() {
        company.clickSubSection("Work Days");
        Assert.assertTrue(company.waitForWorkDayTogglesToRender(),
                "Work Days panel should render within 15s");
        int weekdayCards = 0;
        for (String d : new String[]{"Monday","Tuesday","Wednesday","Thursday","Friday"}) {
            if (company.isDayPresent(d)) weekdayCards++;
        }
        Assert.assertTrue(weekdayCards >= 3,
                "Work Days panel should expose at least 3 weekday cards. Got: " + weekdayCards);
        ExtentManager.getTest().pass("Work Days panel rendered (" + weekdayCards + " weekday cards)");
    }

    @Test(priority = 4, groups = {"regression"}, description = "RV_CFG_004 – Leave Type allotment panel renders")
    public void RV_CFG_004_leaveTypeAllotmentPanel() {
        company.clickSubSection("Leave Types");
        int tables = driver.findElements(
            org.openqa.selenium.By.cssSelector("p-table, table")).size();
        Assert.assertTrue(tables >= 1,
                "Leave Types sub-section should render a table");
        ExtentManager.getTest().pass("Leave Types panel renders");
    }
}
