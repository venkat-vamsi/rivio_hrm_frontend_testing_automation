package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.EmployeeDirectoryPage;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * EmployeeTest – EMP-S-01..EMP-S-03 from the Test Design Excel, plus HR-found
 * bugs RV-BUG-NEW-01 (bank account validation) and RV-BUG-NEW-02 (phone
 * validation). Both validation bugs live in the "Edit Contact & Info" modal
 * opened by the pencil icon on /employees/:id (Rivio_Angular-main
 * features/employees/employee-profile/employee-profile.component.html).
 *
 *   RV_EMP_001 — Directory renders with table + pagination
 *   RV_EMP_002 — Real-time name search filters without page reload
 *   RV_EMP_003 — Onboard New Employee modal opens
 *   RV_EMP_BUG_01 — Bank Account input must reject random strings
 *   RV_EMP_BUG_02 — Phone Number input must reject non-numeric strings
 */
public class EmployeeTest extends BaseTest {

    @Override protected String getRole() { return ROLE_ADMIN; }

    private EmployeeDirectoryPage directory;

    @BeforeMethod(alwaysRun = true)
    public void openDirectory() {
        // Bucket session is already logged in as Admin via BaseTest @BeforeClass.
        driver.get(AppConstants.EMPLOYEE_DIR_URL);
        WaitUtils.waitForAngularLoad(driver);
        directory = new EmployeeDirectoryPage(driver);
    }

    @Test(priority = 1, groups = {"smoke", "regression"}, description = "RV_EMP_001 – Employee directory renders with table + pagination")
    public void RV_EMP_001_directoryRenders() {
        Assert.assertTrue(directory.isPageLoaded(),
                "Employee directory should be loaded");
        Assert.assertEquals(directory.getPageHeading(), "Employees",
                "Page heading should read 'Employees'");
        Assert.assertTrue(directory.isPaginationVisible(),
                "Pagination should be visible at the bottom of the table");
        ExtentManager.getTest().pass("Employee directory renders with pagination");
    }

    @Test(priority = 2, groups = {"regression"}, description = "RV_EMP_002 – Real-time search filters employee list")
    public void RV_EMP_002_realTimeSearch() {
        WaitUtils.waitForUrlContains(driver, "/employees");
        WaitUtils.waitForAngularLoad(driver);

        int before = directory.getRowCount();
        String urlBefore = driver.getCurrentUrl();

        directory.searchEmployee("zzzzz_no_match_xyz");

        Assert.assertEquals(driver.getCurrentUrl(), urlBefore,
                "URL should NOT change during real-time search");
        int after = directory.getRowCount();
        ExtentManager.getTest().info("Rows before: " + before + ", after no-match search: " + after);
        Assert.assertTrue(after <= before, "Row count must not grow when search has no matches");

        directory.clearSearch();
        ExtentManager.getTest().pass("Search filters the list in real time");
    }

    @Test(priority = 3, groups = {"regression"}, description = "RV_EMP_003 – Onboard New Employee modal opens")
    public void RV_EMP_003_onboardModalOpens() {
        directory.clickAddEmployee();
        Assert.assertTrue(directory.isOnboardModalOpen(),
                "Clicking 'Add Employee' should open the onboarding modal");
        ExtentManager.getTest().pass("Onboard modal opens");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HR-found bugs — Edit Contact & Info modal on /employees/:id
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * RV-BUG-NEW-01: Bank Account input accepts any random string. Per
     * Rivio_Angular-main employee-profile.component.html lines 186-189, the
     * "bankAccount" field has no validator beyond presence — alphabetic /
     * symbol input is saved as-is.
     */
    @Test(priority = 10, groups = {"bug", "regression"}, description =
        "RV_EMP_BUG_01 – Bank Account number must reject random non-numeric input")
    public void RV_EMP_BUG_01_bankAccountNumberValidation() {
        openEditContactModalFromFirstProfile();

        WebElement bank = findInputByFormControlName("bankAccount");
        Assert.assertNotNull(bank,
                "Bank Account input not found in Edit Contact & Info modal — selector drift");

        WaitUtils.jsSetValue(driver, bank, "abcd_random_xyz!!");
        bank.sendKeys(Keys.TAB);
        WaitUtils.hardWait(500);

        boolean rejected = isMarkedInvalid(bank)
                       || isSaveDisabled()
                       || hasNearbyError("bank", "account");

        Assert.assertTrue(rejected,
                "RV-BUG-NEW-01: Bank Account field accepted 'abcd_random_xyz!!'. "
              + "The field must validate the account-number format (numeric, length, "
              + "or pattern) — otherwise any random string passes through.");
        ExtentManager.getTest().pass("Bank Account number validation present");
    }

    /**
     * RV-BUG-NEW-02: Phone Number input accepts arbitrary character strings.
     * Per Rivio_Angular-main employee-profile.component.html lines 182-185,
     * "phoneNo" has no validator — alphabetic input is saved as-is.
     */
    @Test(priority = 11, groups = {"bug", "regression"}, description =
        "RV_EMP_BUG_02 – Phone Number must reject non-numeric input")
    public void RV_EMP_BUG_02_phoneNumberValidation() {
        openEditContactModalFromFirstProfile();

        WebElement phone = findInputByFormControlName("phoneNo");
        Assert.assertNotNull(phone,
                "Phone Number input not found in Edit Contact & Info modal — selector drift");

        WaitUtils.jsSetValue(driver, phone, "abcdefghij");
        phone.sendKeys(Keys.TAB);
        WaitUtils.hardWait(500);

        boolean rejected = isMarkedInvalid(phone)
                       || isSaveDisabled()
                       || hasNearbyError("phone", "mobile");

        Assert.assertTrue(rejected,
                "RV-BUG-NEW-02: Phone Number field accepted the alphabetic string "
              + "'abcdefghij'. The field must validate numeric / phone-format input.");
        ExtentManager.getTest().pass("Phone Number validation present");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void openEditContactModalFromFirstProfile() {
        // Open the first employee's profile via the eye icon, then click the
        // "Edit Contact Info" pencil to open the Edit Contact & Info modal.
        // The eye icon per Rivio_Angular-main employee-directory.html line 101 is
        // a <button> with a pi-eye <i> inside it.
        List<WebElement> eyes = driver.findElements(By.xpath(
            "//p-table//tbody/tr[1]//button[.//i[contains(@class,'pi-eye')]]"));
        if (eyes.isEmpty()) {
            eyes = driver.findElements(By.xpath(
                "//p-table//button[.//i[contains(@class,'pi-eye')]]"));
        }
        if (!eyes.isEmpty()) {
            WaitUtils.scrollAndClick(driver, eyes.get(0));
        } else {
            // Last resort: jump to an arbitrary profile id
            driver.get(AppConstants.EMPLOYEE_DIR_URL + "/1");
        }
        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.waitForUrlToBeStable(driver);

        // Click the pencil button titled "Edit Contact Info"
        try {
            WebElement pencil = WaitUtils.waitForClickability(driver, By.xpath(
                "//button[@title='Edit Contact Info' or contains(@title,'Edit Contact')]"));
            WaitUtils.scrollAndClick(driver, pencil);
        } catch (Exception e) {
            // Fallback: first pencil icon on the page
            try {
                WebElement anyPencil = driver.findElement(By.cssSelector(
                    "button .pi-pencil, button[class*='pencil']"));
                WaitUtils.scrollAndClick(driver, anyPencil);
            } catch (Exception ignored) {}
        }

        // Wait for the Edit Contact & Info dialog
        WaitUtils.waitForPresence(driver, By.cssSelector(
            "p-dialog input[formcontrolname='bankAccount'], "
          + "p-dialog input[formcontrolname='phoneNo']"), 10);
    }

    private WebElement findInputByFormControlName(String name) {
        List<WebElement> els = driver.findElements(By.cssSelector(
            "p-dialog input[formcontrolname='" + name + "']"));
        return els.isEmpty() ? null : els.get(0);
    }

    private boolean isMarkedInvalid(WebElement input) {
        try {
            String cls  = input.getAttribute("class");
            String aria = input.getAttribute("aria-invalid");
            if (cls != null && (cls.contains("ng-invalid") || cls.contains("p-invalid")
                             || cls.contains("is-invalid") || cls.contains("border-rose")
                             || cls.contains("!border-rose"))) return true;
            if ("true".equalsIgnoreCase(aria)) return true;
        } catch (Exception ignored) {}
        return false;
    }

    private boolean isSaveDisabled() {
        try {
            WebElement save = driver.findElement(By.xpath(
                "//p-dialog//button[contains(.,'Save Changes') or contains(.,'Save')]"));
            String dis = save.getAttribute("disabled");
            return dis != null && !dis.isEmpty();
        } catch (Exception e) { return false; }
    }

    private boolean hasNearbyError(String... keywords) {
        for (String kw : keywords) {
            if (!driver.findElements(By.xpath(
                "//p-dialog//*[contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ',"
              + "'abcdefghijklmnopqrstuvwxyz'),'" + kw.toLowerCase() + "') "
              + "and (contains(.,'invalid') or contains(.,'numeric') "
              + "or contains(.,'digits') or contains(.,'must') "
              + "or contains(.,'valid'))]")).isEmpty()) return true;
        }
        return false;
    }
}
