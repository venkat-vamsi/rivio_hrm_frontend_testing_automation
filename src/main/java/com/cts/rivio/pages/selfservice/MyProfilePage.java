package com.cts.rivio.pages.selfservice;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

/**
 * MyProfilePage – Self-service page where an employee views/edits their own profile.
 *
 * Route: /self-service/my-profile
 * Accessible by: Employee role (and above)
 */
public class MyProfilePage {

    private WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(css = ".page-title, h1, h2")
    private WebElement pageTitle;

    @FindBy(css = ".profile-name, h2.name, [class*='emp-name']")
    private WebElement profileName;

    @FindBy(css = ".profile-email, [class*='email-label']")
    private WebElement profileEmail;

    @FindBy(css = ".profile-phone, [class*='phone-label']")
    private WebElement profilePhone;

    @FindBy(css = ".profile-designation, [class*='designation']")
    private WebElement profileDesignation;

    @FindBy(css = ".profile-department, [class*='department']")
    private WebElement profileDepartment;

    // Profile picture
    @FindBy(css = "img.profile-photo, .avatar img, [class*='profile-image']")
    private WebElement profileImage;

    // Edit buttons
    @FindBy(css = "button.edit-profile, button[aria-label*='edit' i], [class*='edit-btn']")
    private WebElement editButton;

    // Editable fields (when in edit mode)
    @FindBy(css = "input[formcontrolname='phone'], input[placeholder*='Phone' i]")
    private WebElement phoneInput;

    @FindBy(css = "input[formcontrolname='address'], textarea[placeholder*='Address' i]")
    private WebElement addressInput;

    // Save / Update button (after editing)
    @FindBy(css = "button.save-btn, button[type='submit'].update")
    private WebElement saveButton;

    // Success message
    @FindBy(css = ".alert-success, .toast-success, [class*='success']")
    private WebElement successMessage;

    // Change password section
    @FindBy(css = "input[formcontrolname='currentPassword'], input[placeholder*='Current Password' i]")
    private WebElement currentPasswordInput;

    @FindBy(css = "input[formcontrolname='newPassword'], input[placeholder*='New Password' i]")
    private WebElement newPasswordInput;

    @FindBy(css = "button.change-password, button:contains('Update Password')")
    private WebElement changePasswordButton;

    // ── Constructor ───────────────────────────────────────────────────────────

    public MyProfilePage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public void clickEdit() {
        WaitUtils.waitForClickability(driver, editButton);
        editButton.click();
    }

    public void updatePhone(String phone) {
        WaitUtils.waitForVisibility(driver, phoneInput);
        phoneInput.clear();
        phoneInput.sendKeys(phone);
    }

    public void updateAddress(String address) {
        addressInput.clear();
        addressInput.sendKeys(address);
    }

    public void clickSave() {
        WaitUtils.waitForClickability(driver, saveButton);
        saveButton.click();
    }

    public void changePassword(String current, String newPass) {
        currentPasswordInput.clear();
        currentPasswordInput.sendKeys(current);
        newPasswordInput.clear();
        newPasswordInput.sendKeys(newPass);
        changePasswordButton.click();
    }

    // ── Verifications ─────────────────────────────────────────────────────────

    public boolean isPageLoaded() {
        WaitUtils.waitForVisibility(driver, pageTitle);
        return pageTitle.isDisplayed();
    }

    public String getName() {
        WaitUtils.waitForVisibility(driver, profileName);
        return profileName.getText().trim();
    }

    public String getEmail() {
        return profileEmail.getText().trim();
    }

    public String getDesignation() {
        return profileDesignation.getText().trim();
    }

    public String getDepartment() {
        return profileDepartment.getText().trim();
    }

    public String getSuccessMessage() {
        WaitUtils.waitForVisibility(driver, successMessage);
        return successMessage.getText().trim();
    }
}
