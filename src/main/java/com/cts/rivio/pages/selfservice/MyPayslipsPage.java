package com.cts.rivio.pages.selfservice;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

public class MyPayslipsPage {

    private WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(css = ".page-title, h1, h2")
    private WebElement pageTitle;

    // Month filter — PrimeNG p-dropdown wrapper (NOT a native <select>)
    @FindBy(css = "p-dropdown[formcontrolname='month'], p-dropdown[formcontrolname='selectedMonth'], " +
                  "p-select[formcontrolname='month'], " +
                  "[class*='month-filter'] .p-dropdown, [class*='month-select'] .p-dropdown, " +
                  "select[name='month'], select.month-select")
    private WebElement monthFilter;

    // Year filter
    @FindBy(css = "p-dropdown[formcontrolname='year'], p-dropdown[formcontrolname='selectedYear'], " +
                  "p-select[formcontrolname='year'], " +
                  "[class*='year-filter'] .p-dropdown, [class*='year-select'] .p-dropdown, " +
                  "select[name='year'], select.year-select")
    private WebElement yearFilter;

    @FindBy(css = "table tbody tr, .payslip-item, [class*='payslip-row'], " +
                  "[class*='payslip-card'], p-table tbody tr")
    private List<WebElement> payslipRows;

    @FindBy(css = "button.download, a.download-payslip, [class*='download-btn'], " +
                  "button[aria-label*='download' i], a[class*='download']")
    private List<WebElement> downloadButtons;

    @FindBy(css = "button.view-payslip, a[class*='view'], [class*='preview-btn'], " +
                  "button[aria-label*='view' i], button[class*='view']")
    private List<WebElement> viewButtons;

    @FindBy(css = ".net-pay, [class*='net-amount'], td.amount, [class*='net-pay']")
    private List<WebElement> netPayLabels;

    @FindBy(css = ".latest-payslip, [class*='recent-payslip'], [class*='current-payslip']")
    private WebElement latestPayslipCard;

    @FindBy(css = ".payslip-modal, .modal.payslip-view, [class*='payslip-dialog'], " +
                  ".p-dialog[class*='payslip']")
    private WebElement payslipModal;

    @FindBy(css = ".payslip-modal .close, .modal .close-btn, button[aria-label='close'], " +
                  ".p-dialog-header-close, [class*='modal-close']")
    private WebElement closeModalButton;

    // ── Constructor ───────────────────────────────────────────────────────────

    public MyPayslipsPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    /**
     * Filters payslips by month. Handles both PrimeNG dropdown and native select.
     */
    public void filterByMonth(String month) {
        // Try PrimeNG dropdown
        try {
            WaitUtils.selectPrimeNgOption(driver, monthFilter, month);
            WaitUtils.waitForAngularLoad(driver);
            return;
        } catch (Exception ignored) {}
        // Fallback: native select
        try {
            new org.openqa.selenium.support.ui.Select(monthFilter).selectByVisibleText(month);
        } catch (Exception e) {
            System.err.println("[MyPayslipsPage] Could not filter by month: " + e.getMessage());
        }
    }

    public void filterByYear(String year) {
        try {
            WaitUtils.selectPrimeNgOption(driver, yearFilter, year);
            WaitUtils.waitForAngularLoad(driver);
            return;
        } catch (Exception ignored) {}
        try {
            new org.openqa.selenium.support.ui.Select(yearFilter).selectByVisibleText(year);
        } catch (Exception e) {
            System.err.println("[MyPayslipsPage] Could not filter by year: " + e.getMessage());
        }
    }

    public void clickDownload(int rowIndex) {
        // Re-fetch to avoid stale
        List<WebElement> btns = driver.findElements(
            By.cssSelector("button.download, a.download-payslip, [class*='download-btn']"));
        WaitUtils.waitForClickability(driver, btns.get(rowIndex));
        WaitUtils.safeClick(driver, btns.get(rowIndex));
    }

    public void clickView(int rowIndex) {
        List<WebElement> btns = driver.findElements(
            By.cssSelector("button.view-payslip, a[class*='view'], [class*='preview-btn']"));
        WaitUtils.waitForClickability(driver, btns.get(rowIndex));
        WaitUtils.safeClick(driver, btns.get(rowIndex));
        WaitUtils.waitForAngularLoad(driver);
    }

    public void closePayslipModal() {
        WaitUtils.waitForClickability(driver, closeModalButton);
        WaitUtils.safeClick(driver, closeModalButton);
    }

    // ── Verifications ─────────────────────────────────────────────────────────

    public boolean isPageLoaded() {
        try {
            WaitUtils.waitForVisibility(driver, pageTitle);
            return pageTitle.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public int getPayslipCount() {
        return driver.findElements(
            By.cssSelector("table tbody tr, .payslip-item, [class*='payslip-row']")).size();
    }

    public String getNetPayAtRow(int rowIndex) {
        return netPayLabels.get(rowIndex).getText().trim();
    }

    public boolean isPayslipModalVisible() {
        try {
            WaitUtils.waitForVisibility(driver, payslipModal);
            return payslipModal.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
}
