package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;

/**
 * EmployeeOnboardPage – Page Object for the employee onboarding / add-employee form.
 *
 * Demonstrates:
 *   – Filling multi-section forms
 *   – Select dropdowns (native HTML <select>)
 *   – File input (document upload)
 *   – Form submit and success/error message verification
 */
public class EmployeeOnboardPage {

    private WebDriver driver;

    // ── Locators – Personal Information ──────────────────────────────────────

    @FindBy(css = "input[formcontrolname='firstName'], input[placeholder*='First Name' i]")
    private WebElement firstNameInput;

    @FindBy(css = "input[formcontrolname='lastName'], input[placeholder*='Last Name' i]")
    private WebElement lastNameInput;

    @FindBy(css = "input[formcontrolname='email'], input[type='email']")
    private WebElement emailInput;

    @FindBy(css = "input[formcontrolname='phone'], input[type='tel']")
    private WebElement phoneInput;

    @FindBy(css = "input[formcontrolname='dob'], input[type='date'][placeholder*='DOB' i]")
    private WebElement dobInput;

    @FindBy(css = "select[formcontrolname='gender'], select[name='gender']")
    private WebElement genderDropdown;

    // ── Locators – Job Information ─────────────────────────────────────────

    @FindBy(css = "select[formcontrolname='department'], select[name='department']")
    private WebElement departmentDropdown;

    @FindBy(css = "select[formcontrolname='designation'], select[name='designation']")
    private WebElement designationDropdown;

    @FindBy(css = "select[formcontrolname='employmentType'], select[name='employmentType']")
    private WebElement employmentTypeDropdown;

    @FindBy(css = "input[formcontrolname='joinDate'], input[placeholder*='Join Date' i]")
    private WebElement joinDateInput;

    @FindBy(css = "select[formcontrolname='location'], select[name='location']")
    private WebElement locationDropdown;

    @FindBy(css = "input[formcontrolname='salary'], input[placeholder*='salary' i]")
    private WebElement salaryInput;

    // ── Locators – Action Buttons ─────────────────────────────────────────

    @FindBy(css = "button[type='submit'], button.submit-btn, button.save-btn")
    private WebElement submitButton;

    @FindBy(css = "button.cancel-btn, button[routerlink], a.cancel")
    private WebElement cancelButton;

    // Success toast / message
    @FindBy(css = ".toast-success, .success-message, [class*='success'], .alert-success")
    private WebElement successMessage;

    // Validation error messages
    @FindBy(css = ".validation-error, .mat-error, .error-text, [class*='invalid-feedback']")
    private java.util.List<WebElement> validationErrors;

    // ── Constructor ───────────────────────────────────────────────────────────

    public EmployeeOnboardPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public void enterFirstName(String name) {
        WaitUtils.waitForVisibility(driver, firstNameInput);
        firstNameInput.clear();
        firstNameInput.sendKeys(name);
    }

    public void enterLastName(String name) {
        lastNameInput.clear();
        lastNameInput.sendKeys(name);
    }

    public void enterEmail(String email) {
        emailInput.clear();
        emailInput.sendKeys(email);
    }

    public void enterPhone(String phone) {
        phoneInput.clear();
        phoneInput.sendKeys(phone);
    }

    public void enterDob(String dob) {
        dobInput.clear();
        dobInput.sendKeys(dob);
    }

    public void selectGender(String gender) {
        new Select(genderDropdown).selectByVisibleText(gender);
    }

    public void selectDepartment(String dept) {
        WaitUtils.waitForVisibility(driver, departmentDropdown);
        new Select(departmentDropdown).selectByVisibleText(dept);
    }

    public void selectDesignation(String designation) {
        new Select(designationDropdown).selectByVisibleText(designation);
    }

    public void selectEmploymentType(String type) {
        new Select(employmentTypeDropdown).selectByVisibleText(type);
    }

    public void enterJoinDate(String date) {
        joinDateInput.clear();
        joinDateInput.sendKeys(date);
    }

    public void selectLocation(String location) {
        new Select(locationDropdown).selectByVisibleText(location);
    }

    public void enterSalary(String salary) {
        salaryInput.clear();
        salaryInput.sendKeys(salary);
    }

    /** Fills the entire onboarding form in one call. */
    public void fillOnboardForm(String firstName, String lastName, String email,
                                 String phone, String dob, String gender,
                                 String dept, String designation, String empType,
                                 String joinDate, String location, String salary) {
        enterFirstName(firstName);
        enterLastName(lastName);
        enterEmail(email);
        enterPhone(phone);
        enterDob(dob);
        selectGender(gender);
        selectDepartment(dept);
        selectDesignation(designation);
        selectEmploymentType(empType);
        enterJoinDate(joinDate);
        selectLocation(location);
        enterSalary(salary);
    }

    public void clickSubmit() {
        WaitUtils.waitForClickability(driver, submitButton);
        submitButton.click();
    }

    public void clickCancel() {
        WaitUtils.waitForClickability(driver, cancelButton);
        cancelButton.click();
    }

    // ── Verifications ─────────────────────────────────────────────────────────

    public String getSuccessMessage() {
        WaitUtils.waitForVisibility(driver, successMessage);
        return successMessage.getText().trim();
    }

    public boolean isSuccessMessageDisplayed() {
        try {
            WaitUtils.waitForVisibility(driver, successMessage);
            return successMessage.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public int getValidationErrorCount() {
        return validationErrors.size();
    }

    public boolean hasValidationErrors() {
        return !validationErrors.isEmpty();
    }
}
