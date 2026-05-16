package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

/**
 * DocumentManagementPage – Page Object for Profile & Documents → Document Management.
 *
 * Test Scenarios : Rivio_TS_08_DocumentManagement
 * Test Cases     : Rivio_TC017 (Upload + verification tracking)
 *                  Rivio_TC018 (Expiry reminders for time-bound documents)
 *
 * Demonstrates:
 *   – File upload via <input type="file"> using sendKeys (absolute file path)
 *   – Reading expiry reminder badges / alerts
 */
public class DocumentManagementPage {

    private WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(css = ".page-title, h1, h2")
    private WebElement pageTitle;

    // Upload button or link that opens file picker
    @FindBy(css = "button.upload-doc, button[class*='upload'], a.upload, [class*='upload-btn']")
    private WebElement uploadButton;

    // Hidden file input element — sendKeys(filePath) to it directly
    @FindBy(css = "input[type='file']")
    private WebElement fileInput;

    // Document name input (label/title for the document)
    @FindBy(css = "input[formcontrolname='documentName'], input[placeholder*='Document Name' i]")
    private WebElement documentNameInput;

    // Document type dropdown
    @FindBy(css = "select[formcontrolname='documentType'], select[name='documentType']")
    private WebElement documentTypeSelect;

    // Expiry date input
    @FindBy(css = "input[formcontrolname='expiryDate'], input[type='date'][placeholder*='expiry' i]")
    private WebElement expiryDateInput;

    // Submit / Save upload button
    @FindBy(css = "button[type='submit'].save-doc, .modal button.btn-primary, button.confirm-upload")
    private WebElement saveDocumentButton;

    // List of uploaded documents
    @FindBy(css = ".document-row, table tbody tr, .doc-item, [class*='document-card']")
    private List<WebElement> documentRows;

    // Verification status badges ("Pending Verification", "Verified", "Rejected")
    @FindBy(css = ".verification-status, [class*='verify-status'], .doc-status-badge")
    private List<WebElement> verificationStatusBadges;

    // Expiry reminder badge / alert
    @FindBy(css = ".expiry-reminder, [class*='expiry-alert'], .doc-expiry-warning")
    private List<WebElement> expiryReminders;

    // "Update Status" / "Verify" button per document (HR view)
    @FindBy(css = "button.verify-doc, button[aria-label='Verify'], [class*='verify-btn']")
    private List<WebElement> verifyButtons;

    // Success toast
    @FindBy(css = ".toast-success, .alert-success, [class*='success-toast']")
    private WebElement successToast;

    // ── Constructor ───────────────────────────────────────────────────────────

    public DocumentManagementPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public void clickUpload() {
        WaitUtils.waitForClickability(driver, uploadButton);
        uploadButton.click();
    }

    /**
     * Uploads a file by sending the absolute file path to the hidden <input type="file">.
     * No file dialog interaction needed — Selenium handles it directly.
     *
     * @param absoluteFilePath e.g. "C:\\TestFiles\\id_proof.pdf"
     */
    public void uploadFile(String absoluteFilePath) {
        WaitUtils.waitForVisibility(driver, fileInput);
        fileInput.sendKeys(absoluteFilePath);
    }

    public void enterDocumentName(String name) {
        WaitUtils.waitForVisibility(driver, documentNameInput);
        documentNameInput.clear();
        documentNameInput.sendKeys(name);
    }

    public void selectDocumentType(String type) {
        new org.openqa.selenium.support.ui.Select(documentTypeSelect).selectByVisibleText(type);
    }

    public void enterExpiryDate(String date) {
        expiryDateInput.clear();
        expiryDateInput.sendKeys(date);
    }

    public void clickSaveDocument() {
        WaitUtils.waitForClickability(driver, saveDocumentButton);
        saveDocumentButton.click();
    }

    /**
     * Full upload flow: click upload → fill form → submit.
     */
    public void uploadDocument(String filePath, String docName, String docType, String expiryDate) {
        clickUpload();
        uploadFile(filePath);
        enterDocumentName(docName);
        selectDocumentType(docType);
        if (expiryDate != null && !expiryDate.isEmpty()) {
            enterExpiryDate(expiryDate);
        }
        clickSaveDocument();
    }

    /**
     * Click the Verify button for a specific document row (HR action).
     */
    public void verifyDocument(int rowIndex) {
        WaitUtils.waitForClickability(driver, verifyButtons.get(rowIndex));
        verifyButtons.get(rowIndex).click();
    }

    // ── Verifications ─────────────────────────────────────────────────────────

    public boolean isPageLoaded() {
        WaitUtils.waitForVisibility(driver, pageTitle);
        return pageTitle.isDisplayed();
    }

    public int getDocumentCount() {
        return documentRows.size();
    }

    public String getVerificationStatusAtRow(int rowIndex) {
        return verificationStatusBadges.get(rowIndex).getText().trim();
    }

    public int getExpiryReminderCount() {
        return expiryReminders.size();
    }

    public boolean isExpiryReminderVisible() {
        return !expiryReminders.isEmpty();
    }

    public String getSuccessToastMessage() {
        WaitUtils.waitForVisibility(driver, successToast);
        return successToast.getText().trim();
    }

    /**
     * Checks if any document in the list has the status "Pending Verification".
     */
    public boolean hasPendingVerificationDocument() {
        return verificationStatusBadges.stream()
                .anyMatch(el -> el.getText().toLowerCase().contains("pending"));
    }
}
