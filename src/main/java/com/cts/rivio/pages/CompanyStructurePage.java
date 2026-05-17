package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * CompanyStructurePage – mirrors features/company/company-structure/company-structure.component.html.
 *
 * Six tabs in left sub-panel: Departments, Roles & Titles, Office Locations,
 * Work Days, Public Holidays, Leave Types.
 */
public class CompanyStructurePage {

    private final WebDriver driver;

    public CompanyStructurePage(WebDriver driver) { this.driver = driver; }

    public boolean isPageLoaded() {
        return WaitUtils.waitForH1Text(driver, "Organization Structure", 15);
    }

    public boolean isSubSectionVisible(String label) {
        return !driver.findElements(By.xpath(
            "//button[normalize-space()='" + label + "' or contains(normalize-space(.),'" + label + "')]")).isEmpty();
    }

    public int countSubSections() {
        String[] labels = {"Departments", "Roles & Titles", "Office Locations",
                           "Work Days", "Public Holidays", "Leave Types"};
        int count = 0;
        for (String l : labels) if (isSubSectionVisible(l)) count++;
        return count;
    }

    public void clickSubSection(String label) {
        WebElement btn = WaitUtils.waitForClickability(driver, By.xpath(
            "//button[contains(normalize-space(.),'" + label + "')]"));
        WaitUtils.safeClick(driver, btn);
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.hardWait(800);  // Right-panel content takes a beat to paint
    }

    /**
     * Waits up to 15s for Work Days content. The Angular component lazy-loads
     * the day toggles; we look for a toggle widget OR a weekday name as proof
     * the panel has rendered.
     */
    public boolean waitForWorkDayTogglesToRender() {
        return WaitUtils.waitForPresence(driver,
            By.xpath(
                "//*[contains(@class,'p-toggleswitch') or contains(@class,'p-inputswitch') "
              + "or self::p-toggleswitch or self::p-inputswitch] | "
              + "//*[normalize-space()='Monday' or normalize-space()='Tuesday' "
              + "or normalize-space()='Wednesday' or normalize-space()='Thursday' "
              + "or normalize-space()='Friday' or normalize-space()='Saturday' "
              + "or normalize-space()='Sunday']"),
            15);
    }

    // Legacy compat
    public boolean isDepartmentsTabVisible() { return isSubSectionVisible("Departments"); }
    public boolean isLocationsTabVisible()    { return isSubSectionVisible("Office Locations"); }
    public boolean isDesignationsTabVisible() { return isSubSectionVisible("Roles & Titles"); }
}
