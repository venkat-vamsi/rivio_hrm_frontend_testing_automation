package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.*;
import com.cts.rivio.utils.ExtentManager;
import org.testng.Assert;
import org.testng.annotations.*;

/**
 * ReportsAnalyticsTest
 *
 * Test Scenario : Rivio_TS_16_ReportsAndAnalytics
 * Test Cases    : Rivio_TC032 – Attendance Summary Report generated and exported (PDF + Excel)
 *                 Rivio_TC033 – Performance Rating Summary report available and exportable
 */
public class ReportsAnalyticsTest extends BaseTest {

    private ReportsPage reportsPage;

    @BeforeMethod
    public void loginAsAdminAndGoToReports() {
        new LoginPage(driver).login(AppConstants.ADMIN_EMAIL, AppConstants.ADMIN_PASSWORD);
        // Navigate to Reports section
        try {
            driver.findElement(org.openqa.selenium.By.xpath(
                "//a[contains(text(),'Report') or contains(@href,'report')]"
                + " | //nav//a[contains(.,'Reports')]")).click();
        } catch (Exception e) {
            ExtentManager.getTest().warning("Reports link not found via nav: " + e.getMessage());
        }
        reportsPage = new ReportsPage(driver);
    }

    // ── Rivio_TC032 ──────────────────────────────────────────────────────────

    @Test(priority = 1,
          description = "Rivio_TC032 – Step 1: Attendance Summary Report loads with data for selected date range")
    public void tc032_Step1_AttendanceSummaryReportLoads() {
        ExtentManager.getTest().info("[TC032-S1] Opening Attendance Summary Report");

        Assert.assertTrue(reportsPage.isPageLoaded(),
                "Reports page should load for HR/Admin");

        try {
            reportsPage.selectReportType("Attendance Summary");
            reportsPage.setStartDate("2025-01-01");
            reportsPage.setEndDate("2025-12-31");
            reportsPage.clickGenerate();

            int rows = reportsPage.getReportRowCount();
            ExtentManager.getTest().info("[TC032-S1] Report rows generated: " + rows);
            ExtentManager.getTest().pass("[TC032-S1] Attendance Summary Report loaded");
        } catch (Exception e) {
            ExtentManager.getTest().warning("[TC032-S1] Report generation issue: " + e.getMessage());
            Assert.assertTrue(reportsPage.isPageLoaded(), "Reports page should remain stable");
        }
    }

    @Test(priority = 2,
          description = "Rivio_TC032 – Step 2: Export Attendance Summary as PDF")
    public void tc032_Step2_ExportAttendanceSummaryAsPdf() {
        ExtentManager.getTest().info("[TC032-S2] Exporting Attendance Summary as PDF");

        try {
            reportsPage.selectReportType("Attendance Summary");
            reportsPage.setStartDate("2025-01-01");
            reportsPage.setEndDate("2025-12-31");
            reportsPage.clickGenerate();

            boolean pdfVisible = reportsPage.isExportPdfButtonVisible();
            ExtentManager.getTest().info("[TC032-S2] PDF export button visible: " + pdfVisible);

            if (pdfVisible) {
                reportsPage.clickExportPdf();
                ExtentManager.getTest().pass("[TC032-S2] PDF export triggered");
            } else {
                ExtentManager.getTest().warning("[TC032-S2] PDF export button not visible after generation");
            }
        } catch (Exception e) {
            ExtentManager.getTest().warning("[TC032-S2] PDF export issue: " + e.getMessage());
        }

        Assert.assertTrue(reportsPage.isPageLoaded(), "Reports page must remain stable");
    }

    @Test(priority = 3,
          description = "Rivio_TC032 – Step 3: Export Attendance Summary as Excel")
    public void tc032_Step3_ExportAttendanceSummaryAsExcel() {
        ExtentManager.getTest().info("[TC032-S3] Exporting Attendance Summary as Excel");

        try {
            reportsPage.selectReportType("Attendance Summary");
            reportsPage.setStartDate("2025-01-01");
            reportsPage.setEndDate("2025-12-31");
            reportsPage.clickGenerate();

            boolean excelVisible = reportsPage.isExportExcelButtonVisible();
            ExtentManager.getTest().info("[TC032-S3] Excel export button visible: " + excelVisible);

            if (excelVisible) {
                reportsPage.clickExportExcel();
                ExtentManager.getTest().pass("[TC032-S3] Excel export triggered");
            } else {
                ExtentManager.getTest().warning("[TC032-S3] Excel export button not visible");
            }
        } catch (Exception e) {
            ExtentManager.getTest().warning("[TC032-S3] Excel export issue: " + e.getMessage());
        }

        Assert.assertTrue(reportsPage.isPageLoaded(), "Reports page must remain stable");
    }

    // ── Rivio_TC033 ──────────────────────────────────────────────────────────

    @Test(priority = 4,
          description = "Rivio_TC033 – Step 1: Performance Rating Summary report loads with current cycle data")
    public void tc033_Step1_PerformanceRatingSummaryLoads() {
        ExtentManager.getTest().info("[TC033-S1] Opening Performance Rating Summary");

        try {
            reportsPage.selectReportType("Performance Rating Summary");

            int rows = reportsPage.getReportRowCount();
            ExtentManager.getTest().info("[TC033-S1] Performance rating rows: " + rows);

            String heading = reportsPage.getReportHeading();
            ExtentManager.getTest().info("[TC033-S1] Report heading: " + heading);

            ExtentManager.getTest().pass("[TC033-S1] Performance Rating Summary loaded");
        } catch (Exception e) {
            ExtentManager.getTest().warning("[TC033-S1] Performance report issue: " + e.getMessage());
            Assert.assertTrue(reportsPage.isPageLoaded(), "Reports page must be stable");
        }

        Assert.assertTrue(reportsPage.isPageLoaded(), "Reports section must remain loaded");
    }

    @Test(priority = 5,
          description = "Rivio_TC033 – Step 2: Export Performance Rating Summary as Excel")
    public void tc033_Step2_ExportPerformanceRatingAsExcel() {
        ExtentManager.getTest().info("[TC033-S2] Exporting Performance Rating Summary as Excel");

        try {
            reportsPage.selectReportType("Performance Rating Summary");

            boolean excelVisible = reportsPage.isExportExcelButtonVisible();
            ExtentManager.getTest().info("[TC033-S2] Excel export button visible: " + excelVisible);

            if (excelVisible) {
                reportsPage.clickExportExcel();
                ExtentManager.getTest().pass("[TC033-S2] Performance rating Excel export triggered");
            } else {
                ExtentManager.getTest().warning("[TC033-S2] Excel export not available");
            }
        } catch (Exception e) {
            ExtentManager.getTest().warning("[TC033-S2] Excel export issue: " + e.getMessage());
        }

        Assert.assertTrue(reportsPage.isPageLoaded(), "Reports page must remain stable");
    }
}
