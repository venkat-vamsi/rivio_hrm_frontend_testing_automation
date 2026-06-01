package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.AttendancePage;
import com.cts.rivio.utils.ExcelUtils;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * AttendanceTest – Time & Attendance module.
 *
 *   RV_ATT_001       – Daily Tracking table renders
 *   RV_ATT_002       – Pencil edit icon present for unlocked records
 *   RV_ATT_003       – Absent checkbox must HIDE/disable the time fields (RV-BUG-004)
 *   RV_ATT_004       – CSV Upload modal opens
 *   RV_ATT_005       – Employee History tab renders filters
 *   RV_ATT_DD_001    – Valid manual punch — Save Record closes modal
 *   RV_ATT_DD_002    – Invalid punch — form rejects (modal stays open)
 *   att_csvBulkUpload – Valid CSV bulk upload — Process CSV reports success count ≥ 1
 *
 */
public class AttendanceTest extends BaseTest {

    @Override protected String getRole() { return ROLE_ADMIN; }

    private AttendancePage attendance;

    @BeforeMethod(alwaysRun = true)
    public void openAttendance() {
        driver.get(AppConstants.ATTENDANCE_URL);
        WaitUtils.waitForAngularLoad(driver);
        attendance = new AttendancePage(driver);
    }

    /**
     * Bug: when "Mark Employee as Absent (Full Day)" is checked the time
     * fields MUST disappear (Angular template wraps them in @if (!isAbsent())).
     */
    @Test(priority = 1, groups = {"bug", "regression", "negative"},
          description = "att_bug_absentHidesTimeFields – Absent checkbox must hide the time fields")
    public void att_bug_absentHidesTimeFields() {
        attendance.clickManualPunch();
        Assert.assertTrue(attendance.isManualPunchModalOpen(),
                "Manual Punch modal should open");

        WebElement chk = attendance.findAbsentCheckbox();
        Assert.assertNotNull(chk, "Absent checkbox not found");
        attendance.checkAbsentCheckbox();
        boolean hiddenOrDisabled = attendance.isPunchInTimeFieldDisabled();
        Assert.assertTrue(hiddenOrDisabled,
                "Punch In field must be removed or disabled when Absent is checked");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DATA-DRIVEN: Manual Punch
    // ══════════════════════════════════════════════════════════════════════════

    @DataProvider(name = "validPunchData")
    public Object[][] validPunchData() {
        return ExcelUtils.readDataExcludingHeader(
                AppConstants.ATTENDANCE_DATA_PATH, AppConstants.SHEET_VALID_PUNCH);
    }

    @DataProvider(name = "invalidPunchData")
    public Object[][] invalidPunchData() {
        return ExcelUtils.readDataExcludingHeader(
                AppConstants.ATTENDANCE_DATA_PATH, AppConstants.SHEET_INVALID_PUNCH);
    }

    /**
     * att_validManualPunch — Save Record closes the modal for valid input.
     */
    @Test(dataProvider = "validPunchData",
          priority = 20,
          groups = {"regression", "positive"},
          description = "att_validManualPunch – Save Record closes the punch modal")
    public void att_validManualPunch(
            String employee, String date, String punchIn, String punchOut) {

        ExtentManager.getTest().info(
            "[DD-Punch] employee=" + employee + " date=" + date
            + " punchIn=" + punchIn + " punchOut=" + punchOut);

        attendance.clickManualPunch();
        Assert.assertTrue(attendance.isManualPunchModalOpen(),
            "Manual Punch modal must open before filling data");

        attendance.selectEmployeeInPunchModal(employee);
        attendance.setDateInPunchModal(date);
        attendance.setPunchInTime(punchIn);
        attendance.setPunchOutTime(punchOut);

        boolean modalClosed = attendance.submitPunchForm();
        Assert.assertTrue(modalClosed,
            "att_validManualPunch: Save Record did NOT close the modal — punch was rejected. "
          + "employee=" + employee + " date=" + date + " in=" + punchIn + " out=" + punchOut);
        ExtentManager.getTest().pass("Manual punch saved");
    }

    @Test(dataProvider = "invalidPunchData",
          priority = 21,
          groups = {"regression", "negative"},
          description = "att_invalidManualPunch – Invalid manual punch is rejected")
    public void att_invalidManualPunch(
            String testCase, String employee, String date,
            String punchIn, String punchOut, String expectedError) {

        ExtentManager.getTest().info(
            "[DD-Punch-Invalid] " + testCase + " | Expected: " + expectedError);

        attendance.clickManualPunch();
        Assert.assertTrue(attendance.isManualPunchModalOpen(),
            "Manual Punch modal must open before filling invalid data");

        if (employee != null && !employee.isEmpty())
            attendance.selectEmployeeInPunchModal(employee);
        if (date != null && !date.isEmpty())
            attendance.setDateInPunchModal(date);
        if (punchIn != null && !punchIn.isEmpty())
            attendance.setPunchInTime(punchIn);
        if (punchOut != null && !punchOut.isEmpty())
            attendance.setPunchOutTime(punchOut);

        boolean modalClosed = attendance.submitPunchForm();
        // For invalid data, the form / alert path keeps the modal open
        Assert.assertFalse(modalClosed,
            "att_invalidManualPunch [" + testCase + "]: Invalid punch was accepted (modal closed). "
          + "Expected: " + expectedError);
        ExtentManager.getTest().pass("Invalid punch rejected [" + testCase + "]");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CSV Bulk Upload
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * att_csvBulkUpload — Bulk Upload Attendance for 5 employees succeeds
     * AND those records show up in Daily Tracking.
     *
     * Flow:
     *   1. Open Daily Tracking tab, capture baseline row count
     *   2. Click "CSV Upload" → modal opens
     *   3. Generate a CSV in the OS temp dir with rows for employeeIds
     *      1..5 on TODAY's date (09:00–17:00) — IDs 1-5 always exist in
     *      the demo DB per the bundled attendance_upload_template
     *   4. Send the absolute path to the hidden <input type="file">
     *   5. Click "Process CSV"
     *   6. Read the green success number from the result panel — assert ≥ 5
     *   7. Close the upload modal, return to Daily Tracking
     *   8. Assert the table now has ≥ baseline + 5 rows OR at least 5 rows
     *      whose text matches "EMP" / employee names — proves the bulk
     *      upload actually populated the daily view (the FRD requirement:
     *      "HR can scroll Daily Tracking and see the imported records").
     */
    @Test(priority = 30,
          groups = {"regression", "positive"},
          description = "att_csvBulkUpload – Bulk CSV upload (5 employees) reports success and rows appear in Daily Tracking")
    public void att_csvBulkUpload() throws Exception {
        attendance.selectDailyTrackingTab();
        WaitUtils.hardWait(800);
        int rowsBefore = attendance.getAttendanceRecordCount();
        ExtentManager.getTest().info("Daily Tracking rows before upload: " + rowsBefore);

        attendance.clickCsvUpload();
        Assert.assertTrue(attendance.isCsvUploadModalOpen(),
            "CSV Upload modal must open before uploading file");

        String csvPath = attendance.generateAttendanceCsvForToday();
        ExtentManager.getTest().info("Generated CSV (5 employees, today): " + csvPath);

        attendance.uploadCsvFile(csvPath);
        int successCount = attendance.clickProcessCsvAndGetSuccessCount();

        Assert.assertTrue(successCount >= 5,
            "att_csvBulkUpload: Process CSV reported " + successCount
          + " successful records — expected ≥ 5. The file may have been "
          + "partly rejected by backend validation.");
        ExtentManager.getTest().pass("CSV upload succeeded: " + successCount + " record(s)");

        // ── Verify on Daily Tracking ─────────────────────────────────────
        attendance.closeCsvUploadModal();
        WaitUtils.hardWait(600);
        attendance.selectDailyTrackingTab();
        WaitUtils.hardWait(1200);          // backend re-list is async

        int rowsAfter = attendance.getAttendanceRecordCount();
        int empRows   = attendance.countDailyTrackingRowsContaining("EMP");

        ExtentManager.getTest().info(
            "Daily Tracking rows after upload: " + rowsAfter + " (was " + rowsBefore + ")");
        ExtentManager.getTest().info("Rows containing 'EMP' (employee code marker): " + empRows);

        Assert.assertTrue(rowsAfter >= rowsBefore + 5 || empRows >= 5,
            "att_csvBulkUpload: Daily Tracking did NOT reflect the bulk upload. "
          + "Before=" + rowsBefore + " After=" + rowsAfter
          + " EmpRows=" + empRows + " (expected ≥5 added or ≥5 EMP-rows present).");
        ExtentManager.getTest().pass("Daily Tracking reflects bulk upload");
    }
}
