package com.cts.rivio.pages.selfservice;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

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

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(xpath = "//p[contains(@class,'uppercase') and (contains(.,'Required') or contains(.,'Present') "
                  + "or contains(.,'Absent') or contains(.,'Approved Leaves') or contains(.,'Score'))]")
    private List<WebElement> kpiLabels;

    @FindBy(xpath = "//*[contains(text(),'Attendance Score') or contains(text(),'Score')]")
    private List<WebElement> attendanceScoreElements;

    @FindBy(css = "p-table tbody tr")
    private List<WebElement> calendarRows;

    @FindBy(css = "p-select")
    private List<WebElement> monthYearSelectors;

    // ── Constructor ───────────────────────────────────────────────────────────

    public MyAttendancePage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public boolean isPageLoaded() {
        boolean headerOk = WaitUtils.waitForH1Text(driver, "My Attendance Log", 15);
        if (!headerOk) return false;
        return WaitUtils.waitForPresence(driver,
            By.xpath("//p[contains(.,'Required Work Days') or contains(.,'Days Present')]"), 8);
    }

    public int getSummaryStatCount() {
        return kpiLabels.size();
    }

    public boolean isAttendanceScoreVisible() {
        return !attendanceScoreElements.isEmpty();
    }

    public String getAttendanceScoreText() {
        try {
            // Score card uses dynamic ancestor lookup — kept as inline By
            WebElement card = driver.findElement(By.xpath(
                "//*[contains(text(),'Attendance Score')]/ancestor::*[contains(@class,'rounded-2xl')][1]"));
            return card.getText().trim();
        } catch (Exception e) { return ""; }
    }

    public int getCalendarCellCount() {
        return calendarRows.size();
    }

    public boolean isMonthYearSelectorVisible() {
        return !monthYearSelectors.isEmpty();
    }
}
