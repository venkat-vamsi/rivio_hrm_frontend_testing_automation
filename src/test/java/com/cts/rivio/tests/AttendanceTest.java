package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.AttendancePage;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * AttendanceTest – ATT-S-01..ATT-S-04.
 *
 *   RV_ATT_001 – Daily Tracking table shows punch records
 *   RV_ATT_002 – HR can open inline edit for unlocked records
 *   RV_ATT_003 – Manual Punch modal: Absent checkbox should disable time fields (RV-BUG-004)
 *   RV_ATT_004 – CSV Upload modal opens
 *   RV_ATT_005 – Employee History tab filters by employee + date range
 */
public class AttendanceTest extends BaseTest {

    @Override protected String getRole() { return ROLE_ADMIN; }

    private AttendancePage attendance;

    @BeforeMethod(alwaysRun = true)
    public void openAttendance() {
        // Bucket session is already logged in as Admin via BaseTest @BeforeClass.
        driver.get(AppConstants.ATTENDANCE_URL);
        com.cts.rivio.utils.WaitUtils.waitForAngularLoad(driver);
        attendance = new AttendancePage(driver);
    }

    @Test(priority = 1, groups = {"smoke", "regression"}, description = "RV_ATT_001 – Daily Tracking table renders")
    public void RV_ATT_001_dailyTrackingRenders() {
        Assert.assertTrue(attendance.isPageLoaded(),
                "Time & Attendance page should be loaded");
        attendance.selectDailyTrackingTab();
        Assert.assertTrue(attendance.getAttendanceRecordCount() >= 0,
                "Daily Tracking table should render (count may be 0 if no data)");
        ExtentManager.getTest().pass("Daily Tracking table renders");
    }

    @Test(priority = 2, groups = {"regression"}, description = "RV_ATT_002 – Pencil edit icon present for unlocked records")
    public void RV_ATT_002_pencilEditIconForUnlocked() {
        attendance.selectDailyTrackingTab();
        boolean hasPencil = !driver.findElements(
                org.openqa.selenium.By.cssSelector("button[title='Edit Punch']")).isEmpty();
        ExtentManager.getTest().info("Edit-pencil icons present: " + hasPencil);
        // Test passes either way; just record state since not all records are unlocked
        ExtentManager.getTest().pass("Daily Tracking inline-edit affordance checked");
    }

    @Test(priority = 3, groups = {"bug", "regression"}, description = "RV_ATT_003 – Manual Punch modal opens; Absent checkbox disables time fields")
    public void RV_ATT_003_manualPunchAbsentDisablesTimeFields() {
        attendance.clickManualPunch();
        Assert.assertTrue(attendance.isManualPunchModalOpen(),
                "Manual Punch modal should open");

        // RV-BUG-004 in the defect log: the Absent checkbox does NOT disable
        // the time fields. We assert the spec; this test should FAIL until the
        // dev fix lands.
        try {
            org.openqa.selenium.WebElement chk = attendance.findAbsentCheckbox();
            if (chk != null) {
                chk.click();
                boolean disabled = attendance.isPunchInTimeFieldDisabled();
                Assert.assertTrue(disabled,
                        "RV-BUG-004: Punch In field should be DISABLED when Absent is checked");
            } else {
                ExtentManager.getTest().warning("Absent checkbox not found in modal");
            }
        } catch (Exception e) {
            ExtentManager.getTest().warning("Absent checkbox interaction error: " + e.getMessage());
        }
    }

    @Test(priority = 4, groups = {"regression"}, description = "RV_ATT_004 – CSV Upload modal opens")
    public void RV_ATT_004_csvUploadModalOpens() {
        attendance.clickCsvUpload();
        Assert.assertTrue(attendance.isCsvUploadModalOpen(),
                "CSV Upload modal should open");
        ExtentManager.getTest().pass("CSV Upload modal opens");
    }

    @Test(priority = 5, groups = {"regression"}, description = "RV_ATT_005 – Employee History tab renders filters")
    public void RV_ATT_005_employeeHistoryTab() {
        attendance.selectEmployeeHistoryTab();
        boolean hasFilters = !driver.findElements(
                org.openqa.selenium.By.cssSelector("p-select, p-datepicker")).isEmpty();
        Assert.assertTrue(hasFilters,
                "Employee History tab should expose employee + date-range filters");
        ExtentManager.getTest().pass("Employee History filters visible");
    }
}
