package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.pages.selfservice.MyAttendancePage;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * MyAttendanceTest – ATT-S-05 (My Attendance Log).
 *
 *   RV_ATT_006 – Monthly KPI summary (Required, Present, Absent, Approved Leaves, Score)
 *                NOTE: RV-BUG-010 — Attendance Score always reads 0%.
 */
public class MyAttendanceTest extends BaseTest {

    @Test(priority = 1, description = "RV_ATT_006 – My Attendance log renders KPI cards")
    public void RV_ATT_006_myAttendanceKpis() {
        new LoginPage(driver).login(AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD);
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
}
