package com.cts.rivio.pages.selfservice;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * MyAttendancePage – mirrors features/self-service/my-attendance/my-attendance.component.html.
 *
 * Real DOM:
 *   - <h1>My Attendance Log</h1>
 *   - 5 KPI cards: Required Work Days, Days Present, Days Absent, Approved Leaves, Attendance Score
 *   - Month/Year selectors (p-select)
 *   - Daily log p-table
 */
public class MyAttendancePage {

    private final WebDriver driver;

    public MyAttendancePage(WebDriver driver) { this.driver = driver; }

    public boolean isPageLoaded() {
        // First wait for the h1, then wait briefly for the KPI cards to render
        boolean headerOk = com.cts.rivio.utils.WaitUtils.waitForH1Text(driver, "My Attendance Log", 15);
        if (!headerOk) return false;
        // KPI labels render after the h1 — wait for at least one of them
        return com.cts.rivio.utils.WaitUtils.waitForPresence(driver,
            By.xpath("//p[contains(.,'Required Work Days') or contains(.,'Days Present')]"), 8);
    }

    public int getSummaryStatCount() {
        return driver.findElements(By.xpath(
            "//p[contains(@class,'uppercase') and (contains(.,'Required') or contains(.,'Present') "
            + "or contains(.,'Absent') or contains(.,'Approved Leaves') or contains(.,'Score'))]"
        )).size();
    }

    public boolean isAttendanceScoreVisible() {
        return !driver.findElements(By.xpath("//*[contains(text(),'Attendance Score') or contains(text(),'Score')]")).isEmpty();
    }

    public String getAttendanceScoreText() {
        try {
            // The score card has gradient slate-800 to slate-950; capture its visible value
            WebElement card = driver.findElement(By.xpath(
                "//*[contains(text(),'Attendance Score')]/ancestor::*[contains(@class,'rounded-2xl')][1]"));
            return card.getText().trim();
        } catch (Exception e) { return ""; }
    }

    public int getCalendarCellCount() {
        return driver.findElements(By.cssSelector("p-table tbody tr")).size();
    }

    public boolean isMonthYearSelectorVisible() {
        return !driver.findElements(By.cssSelector("p-select")).isEmpty();
    }
}
