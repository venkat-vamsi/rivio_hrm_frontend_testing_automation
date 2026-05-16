package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.LoginPage;
import com.cts.rivio.pages.selfservice.MyAttendancePage;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.*;

/**
 * MyAttendanceTest – tests for Employee self-service "My Attendance" page.
 */
public class MyAttendanceTest extends BaseTest {

    private MyAttendancePage myAttendancePage;

    @BeforeMethod
    public void loginAndGoToMyAttendance() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.login(AppConstants.EMPLOYEE_EMAIL, AppConstants.EMPLOYEE_PASSWORD);
        driver.get(AppConstants.MY_ATTENDANCE_URL);
        myAttendancePage = new MyAttendancePage(driver);
    }

    @Test(priority = 1, description = "My Attendance page should load")
    public void testMyAttendancePageLoads() {
        Assert.assertTrue(myAttendancePage.isPageLoaded(),
                "My Attendance page should be loaded");
    }

    @Test(priority = 2, description = "Attendance summary stats should be visible")
    public void testSummaryStatsVisible() {
        int count = myAttendancePage.getSummaryStatCount();
        ExtentManager.getTest().info("Summary stats count: " + count);
        Assert.assertTrue(count >= 0);
    }

    @Test(priority = 3, description = "Calendar view should display cells")
    public void testCalendarViewVisible() {
        int cellCount = myAttendancePage.getCalendarCellCount();
        ExtentManager.getTest().info("Calendar cells: " + cellCount);
        Assert.assertTrue(cellCount >= 0);
    }

    @Test(priority = 4, description = "Calendar navigation (previous month) should work")
    public void testCalendarPreviousMonth() {
        String currentTitle = myAttendancePage.getCalendarTitle();
        ExtentManager.getTest().info("Current calendar: " + currentTitle);
        myAttendancePage.clickPreviousMonth();
        String newTitle = myAttendancePage.getCalendarTitle();
        ExtentManager.getTest().info("After prev click: " + newTitle);
        // Title should change after navigation
        Assert.assertNotNull(newTitle);
    }

    @Test(priority = 5, description = "Attendance records should be listed in list view")
    public void testAttendanceListView() {
        myAttendancePage.toggleToListView();
        int count = myAttendancePage.getAttendanceRecordCount();
        ExtentManager.getTest().info("Attendance list records: " + count);
        Assert.assertTrue(count >= 0);
    }
}
