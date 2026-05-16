package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

public class CompanyStructurePage {

    private WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(css = ".page-title, h1, h2")
    private WebElement pageTitle;

    // Sub-tabs: covers PrimeNG p-tabView, Bootstrap nav-tabs, custom Angular tabs
    @FindBy(css = ".p-tabview-nav li, [role='tab'], .nav-tabs .nav-item, " +
                  ".mat-tab-label, [class*='tab-header'] li, [class*='sub-tab']")
    private List<WebElement> subTabs;

    // Department rows — table or card style
    @FindBy(css = ".department-card, [class*='dept-row'], table tbody tr, " +
                  "[class*='department-row'], p-table tbody tr")
    private List<WebElement> departmentRows;

    // "Add Department" button — XPath replaces invalid :contains() CSS
    @FindBy(xpath = "//button[contains(normalize-space(),'Add Department') or " +
                    "contains(normalize-space(),'New Department') or " +
                    "contains(normalize-space(),'Add Dept')]")
    private WebElement addDepartmentButton;

    // Department name input in add/edit form
    @FindBy(css = "input[formcontrolname='name'], input[formcontrolname='departmentName'], " +
                  "input[placeholder*='Department Name' i], input[placeholder*='Name' i]")
    private WebElement departmentNameInput;

    // Description field
    @FindBy(css = "textarea[formcontrolname='description'], input[formcontrolname='description'], " +
                  "textarea[placeholder*='Description' i]")
    private WebElement descriptionInput;

    // Manager dropdown (PrimeNG)
    @FindBy(css = "p-dropdown[formcontrolname='manager'], p-dropdown[formcontrolname='managerId'], " +
                  "select[formcontrolname='manager']")
    private WebElement managerDropdown;

    // Form save button
    @FindBy(css = "button[type='submit'], .modal button.btn-primary, " +
                  "[class*='save-btn'], [class*='submit-btn']")
    private WebElement saveButton;

    // Form cancel button
    @FindBy(css = "button.cancel, .modal button.btn-secondary, " +
                  "[class*='cancel-btn'], button[type='button'][class*='secondary']")
    private WebElement cancelButton;

    // Delete confirmation modal
    @FindBy(css = ".delete-confirm-modal, [role='alertdialog'], .p-confirm-dialog, " +
                  ".p-dialog[class*='confirm']")
    private WebElement deleteConfirmModal;

    @FindBy(css = ".delete-confirm-modal .confirm, [role='alertdialog'] button.confirm, " +
                  ".p-confirm-dialog-accept, .p-dialog .p-button-danger")
    private WebElement confirmDeleteButton;

    // Success toast
    @FindBy(css = ".toast-success, .alert-success, [class*='success-toast'], " +
                  ".p-toast-message-success")
    private WebElement successToast;

    // Org chart
    @FindBy(css = ".org-chart .node, [class*='org-node'], .tree-node, p-organizationchart .p-organizationchart-node")
    private List<WebElement> orgChartNodes;

    // ── Constructor ───────────────────────────────────────────────────────────

    public CompanyStructurePage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    /**
     * Clicks a sub-tab by name. Tries exact match first, then partial match.
     * Re-fetches tab list to avoid stale elements.
     */
    public void clickSubTab(String tabName) {
        // Re-fetch tabs fresh from DOM
        List<WebElement> tabs = driver.findElements(By.cssSelector(
            ".p-tabview-nav li, [role='tab'], .nav-tabs .nav-item a, .nav-tabs .nav-link, " +
            ".mat-tab-label, [class*='tab-header'] li, [class*='sub-tab'], " +
            "[class*='tab-item'], ul.tabs li"));

        for (WebElement tab : tabs) {
            try {
                String text = tab.getText().trim();
                if (text.equalsIgnoreCase(tabName) || text.contains(tabName)) {
                    WaitUtils.scrollAndClick(driver, tab);
                    WaitUtils.waitForAngularLoad(driver);
                    return;
                }
            } catch (StaleElementReferenceException ignored) {}
        }

        // Fallback: XPath text search
        try {
            WebElement tab = WaitUtils.waitForClickability(driver,
                By.xpath("//*[@role='tab' and contains(normalize-space(),'" + tabName + "')] | " +
                         "//li[contains(@class,'tab') and contains(normalize-space(),'" + tabName + "')] | " +
                         "//a[contains(@class,'tab') and contains(normalize-space(),'" + tabName + "')]"));
            WaitUtils.safeClick(driver, tab);
            WaitUtils.waitForAngularLoad(driver);
        } catch (Exception e) {
            throw new NoSuchElementException("Sub-tab not found: " + tabName);
        }
    }

    public void clickAddDepartment() {
        WaitUtils.dismissPopupsIfPresent(driver);
        WaitUtils.waitForClickability(driver, addDepartmentButton);
        WaitUtils.safeClick(driver, addDepartmentButton);
        WaitUtils.waitForAngularLoad(driver);
    }

    public void enterDepartmentName(String name) {
        WaitUtils.waitForVisibility(driver, departmentNameInput);
        departmentNameInput.clear();
        departmentNameInput.sendKeys(name);
    }

    public void enterDescription(String description) {
        try {
            WaitUtils.waitForVisibility(driver, descriptionInput);
            descriptionInput.clear();
            descriptionInput.sendKeys(description);
        } catch (Exception ignored) {}
    }

    public void clickSave() {
        WaitUtils.waitForClickability(driver, saveButton);
        WaitUtils.safeClick(driver, saveButton);
        WaitUtils.waitForAngularLoad(driver);
    }

    public void clickCancel() {
        WaitUtils.waitForClickability(driver, cancelButton);
        WaitUtils.safeClick(driver, cancelButton);
    }

    public void deleteDepartment(int rowIndex) {
        // Re-fetch rows to avoid stale reference
        List<WebElement> rows = driver.findElements(
            By.cssSelector("table tbody tr, [class*='department-row'], [class*='dept-row']"));
        WebElement row = rows.get(rowIndex);

        // Find the delete button in the row
        WebElement deleteBtn = row.findElement(By.xpath(
            ".//button[contains(@aria-label,'delete') or contains(@aria-label,'Delete') or " +
            "contains(normalize-space(),'Delete') or contains(@class,'delete') or " +
            "contains(@class,'remove')] | " +
            ".//a[contains(@class,'delete') or contains(normalize-space(),'Delete')]"));
        WaitUtils.safeClick(driver, deleteBtn);

        // Handle JS alert or modal confirmation
        try {
            Alert alert = WaitUtils.waitForAlert(driver);
            System.out.println("[Alert] Delete confirmation: " + alert.getText());
            alert.accept();
        } catch (Exception e) {
            try {
                WaitUtils.waitForVisibility(driver, deleteConfirmModal);
                WaitUtils.safeClick(driver, confirmDeleteButton);
            } catch (Exception ignored) {}
        }
        WaitUtils.waitForAngularLoad(driver);
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

    public int getDepartmentCount() {
        return driver.findElements(
            By.cssSelector("table tbody tr, [class*='department-row']")).size();
    }

    public String getSuccessToastMessage() {
        WaitUtils.waitForVisibility(driver, successToast);
        return successToast.getText().trim();
    }

    public int getOrgChartNodeCount() {
        return orgChartNodes.size();
    }
}
