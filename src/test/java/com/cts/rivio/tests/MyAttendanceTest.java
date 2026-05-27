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
 * MyAttendanceTest — Employee Self-Service → My Attendance.
 *
 * Naming pattern: {@code ssp_attendance_<scenario>}.
 *
 *   ssp_attendance_bug_workDaysWeekends – "Required Work Days" KPI must exclude
 *                                          Saturday and Sunday (FRD §2.5 bug)
 */
public class MyAttendanceTest extends BaseTest {

    @Override protected String getRole() { return ROLE_EMPLOYEE; }

    /**
     * Bug: the "Required Work Days" KPI on /self-service/attendance counts
     * weekends as working days. The displayed count must not exceed the
     * weekday count for the current month.
     */
    @Test(priority = 1, groups = {"bug", "regression", "negative"},
          description = "ssp_attendance_bug_workDaysWeekends – Required Work Days KPI must exclude weekends")
    public void ssp_attendance_bug_workDaysWeekends() {
        driver.get(AppConstants.MY_ATTENDANCE_URL);
        WaitUtils.waitForAngularLoad(driver);

        MyAttendancePage page = new MyAttendancePage(driver);
        Assert.assertTrue(page.isPageLoaded(),
                "My Attendance Log page must load before checking the KPI");

        WebElement valueEl = WaitUtils.waitForVisibility(driver, By.xpath(
            "//*[normalize-space()='Required Work Days']/following-sibling::*[1]"));
        String text = valueEl.getText().trim();
        int displayed = parseFirstInt(text);
        ExtentManager.getTest().info("Required Work Days displayed: " + text + " (parsed=" + displayed + ")");

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
                "My Attendance reports " + displayed + " 'Required Work Days' for "
              + today.getMonth() + " " + today.getYear() + ", but the month only has "
              + weekdays + " weekdays. Weekends are being counted as required work days.");
        ExtentManager.getTest().pass(
            "Required Work Days (" + displayed + ") correctly excludes weekends");
    }

    private int parseFirstInt(String s) {
        if (s == null) return -1;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(s);
        return m.find() ? Integer.parseInt(m.group()) : -1;
    }
}
