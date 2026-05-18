package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.selfservice.MyAttendancePage;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * MyAttendanceTest – ATT-S-05 (My Attendance Log) and HR-found Bug 6
 * (weekends counted as Required Work Days).
 *
 *   RV_ATT_006 – Monthly KPI summary (Required, Present, Absent, Approved Leaves, Score)
 *                NOTE: RV-BUG-010 — Attendance Score always reads 0%.
 *   RV_ATT_BUG_06 – "Required Work Days" KPI must exclude Saturday & Sunday.
 */
public class MyAttendanceTest extends BaseTest {

    @Override protected String getRole() { return ROLE_EMPLOYEE; }

    @Test(priority = 1, description = "RV_ATT_006 – My Attendance log renders KPI cards")
    public void RV_ATT_006_myAttendanceKpis() {
        driver.get(AppConstants.MY_ATTENDANCE_URL);

        MyAttendancePage page = new MyAttendancePage(driver);
        Assert.assertTrue(page.isPageLoaded(),
                "My Attendance Log page should be loaded");
        int count = page.getSummaryStatCount();
        Assert.assertTrue(count >= 4,
                "Expected at least 4 KPI cards (Required, Present, Absent, Approved Leaves). Got: " + count);
        Assert.assertTrue(page.isMonthYearSelectorVisible(),
                "Month/Year selectors should be visible");
        ExtentManager.getTest().info("Attendance score card text: " + page.getAttendanceScoreText());
        ExtentManager.getTest().pass("My Attendance Log KPIs render");
    }

    /**
     * RV-BUG-NEW-06: On /self-service/attendance, the "Required Work Days"
     * KPI card displays the number of working days in the current month. Per
     * Rivio_Angular-main my-attendance.component.ts:
     *   totalWorkingDays = computed(() => this.monthlyLog().filter(log =>
     *       log.isWorkingDay && !log.isHoliday).length);
     * where isWorkingDay is derived from the backend /work-days response —
     * a day's `isWorkingDay:false` flag adds it to nonWorkingDays.
     *
     * Bug: the live backend currently returns Saturday and Sunday with
     * isWorkingDay:true, so they get counted as required work days. For any
     * calendar month the upper-bound of valid working days is (days in month
     * − weekend days). When the displayed value exceeds that, weekends are
     * being included.
     */
    @Test(priority = 2, description =
        "RV_ATT_BUG_06 – Required Work Days KPI must exclude weekends")
    public void RV_ATT_BUG_06_requiredWorkDaysExcludesWeekends() {
        driver.get(AppConstants.MY_ATTENDANCE_URL);
        WaitUtils.waitForAngularLoad(driver);

        MyAttendancePage page = new MyAttendancePage(driver);
        Assert.assertTrue(page.isPageLoaded(),
                "My Attendance Log page must load before checking the KPI");

        // Locate the Required Work Days card and read its numeric value.
        WebElement valueEl = WaitUtils.waitForVisibility(driver, By.xpath(
            "//*[normalize-space()='Required Work Days']"
          + "/following-sibling::*[1]"));
        String text = valueEl.getText().trim();
        int displayed = parseFirstInt(text);
        ExtentManager.getTest().info("Required Work Days displayed: " + text + " (parsed=" + displayed + ")");

        // Compute the upper-bound of working days for the current month — days
        // in month minus Saturdays + Sundays. Holidays would reduce this
        // further; we only assert the weekend exclusion here.
        LocalDate today    = LocalDate.now();
        LocalDate firstDay = today.withDayOfMonth(1);
        LocalDate lastDay  = today.withDayOfMonth(today.lengthOfMonth());

        int weekdays = 0;
        for (LocalDate d = firstDay; !d.isAfter(lastDay); d = d.plusDays(1)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) weekdays++;
        }
        ExtentManager.getTest().info(
            "Calendar month " + today.getMonth() + " " + today.getYear()
          + " — total days=" + today.lengthOfMonth() + ", weekdays=" + weekdays);

        Assert.assertTrue(displayed > 0 && displayed <= weekdays,
                "RV-BUG-NEW-06: My Attendance reports " + displayed + " 'Required Work "
              + "Days' for " + today.getMonth() + " " + today.getYear() + ", but the "
              + "month only has " + weekdays + " weekdays. Weekends are being counted "
              + "as required work days — Saturday and Sunday must be excluded.");
        ExtentManager.getTest().pass(
            "Required Work Days (" + displayed + ") correctly excludes weekends");
    }

    private int parseFirstInt(String s) {
        if (s == null) return -1;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(s);
        return m.find() ? Integer.parseInt(m.group()) : -1;
    }
}
