package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * CompanyStructurePage – mirrors features/company/company-structure/company-structure.component.html
 * in Rivio_Angular-main.
 *
 * Real DOM:
 *   - <h1>Organization Structure</h1>
 *   - Left side: 6 sub-section buttons (Departments, Roles & Titles, Office
 *     Locations, Work Days, Public Holidays, Leave Types). The setTab() signal
 *     swaps the right-side panel.
 *   - Work Days panel renders @for (day of workDays()) — each day shows a
 *     custom toggle button. ON state adds class `from-emerald-400`; OFF state
 *     uses `bg-slate-300/50` on the same button. (No PrimeNG toggleswitch.)
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
        WaitUtils.hardWait(600);  // right-panel signal-driven re-render
    }

    /**
     * Waits for Work Days panel content. Per Rivio_Angular-main, the panel
     * renders a card per day with the day name + a custom toggle button.
     * No PrimeNG toggleswitch is used — detect by day-name text.
     */
    public boolean waitForWorkDayTogglesToRender() {
        return WaitUtils.waitForPresence(driver,
            By.xpath(
                "//*[normalize-space()='Monday' or normalize-space()='Tuesday' "
              + "or normalize-space()='Wednesday' or normalize-space()='Thursday' "
              + "or normalize-space()='Friday' or normalize-space()='Saturday' "
              + "or normalize-space()='Sunday']"),
            15);
    }

    /**
     * Returns true if the given day's toggle button is in the ON state.
     * The toggle's "on" colour class is `from-emerald-400`; "off" uses `slate-300`.
     */
    public boolean isDayWorking(String dayName) {
        try {
            By byToggle = By.xpath(
                "//*[normalize-space()='" + dayName + "']/following::button[1]");
            WebElement btn = driver.findElement(byToggle);
            String cls = btn.getAttribute("class");
            if (cls == null) return false;
            return cls.contains("from-emerald") || cls.contains("emerald-400");
        } catch (Exception e) {
            return false;
        }
    }

    /** Returns true if a day card exists for the given day name. */
    public boolean isDayPresent(String dayName) {
        return !driver.findElements(By.xpath(
            "//*[normalize-space()='" + dayName + "']")).isEmpty();
    }

    // Legacy compat
    public boolean isDepartmentsTabVisible() { return isSubSectionVisible("Departments"); }
    public boolean isLocationsTabVisible()    { return isSubSectionVisible("Office Locations"); }
    public boolean isDesignationsTabVisible() { return isSubSectionVisible("Roles & Titles"); }
}
