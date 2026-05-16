package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

/**
 * EmployeeProfilePage – Page Object for the individual employee profile/detail page.
 *
 * This page typically shows:
 *   – Personal info (name, email, phone, DOB)
 *   – Job info (designation, department, join date, employment type)
 *   – Documents / file uploads
 *   – Edit / Update actions
 */
public class EmployeeProfilePage {

    private WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(css = ".employee-name, h1.name, .profile-name")
    private WebElement employeeName;

    @FindBy(css = ".employee-email, [class*='email']")
    private WebElement employeeEmail;

    @FindBy(css = ".employee-phone, [class*='phone']")
    private WebElement employeePhone;

    @FindBy(css = ".employee-designation, [class*='designation'], [class*='role']")
    private WebElement designation;

    @FindBy(css = ".employee-department, [class*='department']")
    private WebElement department;

    @FindBy(css = ".employee-join-date, [class*='join-date'], [class*='joinDate']")
    private WebElement joinDate;

    @FindBy(css = ".employment-type, [class*='employment-type']")
    private WebElement employmentType;

    // Profile picture / avatar
    @FindBy(css = "img.profile-avatar, .avatar img, [class*='profile-pic']")
    private WebElement profileAvatar;

    // Tab navigation (Personal Info, Job Info, Documents, etc.)
    @FindBy(css = ".profile-tabs li, .tab-nav a, [role='tab']")
    private List<WebElement> profileTabs;

    // Edit profile button
    @FindBy(css = "button.edit-profile, button[aria-label*='edit'], [class*='edit-btn']")
    private WebElement editButton;

    // Back to directory link
    @FindBy(css = "a[routerlink*='employees'], button.back-btn, [class*='back']")
    private WebElement backButton;

    // Status badge (Active / Inactive)
    @FindBy(css = ".status-badge, [class*='status'], .employee-status")
    private WebElement statusBadge;

    // ── Constructor ───────────────────────────────────────────────────────────

    public EmployeeProfilePage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public void clickTab(String tabName) {
        for (WebElement tab : profileTabs) {
            if (tab.getText().trim().equalsIgnoreCase(tabName)) {
                WaitUtils.waitForClickability(driver, tab);
                tab.click();
                return;
            }
        }
        throw new NoSuchElementException("Tab not found: " + tabName);
    }

    public void clickEdit() {
        WaitUtils.waitForClickability(driver, editButton);
        editButton.click();
    }

    public EmployeeDirectoryPage goBack() {
        WaitUtils.waitForClickability(driver, backButton);
        backButton.click();
        return new EmployeeDirectoryPage(driver);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getEmployeeName() {
        WaitUtils.waitForVisibility(driver, employeeName);
        return employeeName.getText().trim();
    }

    public String getEmail() {
        WaitUtils.waitForVisibility(driver, employeeEmail);
        return employeeEmail.getText().trim();
    }

    public String getPhone() {
        try { return employeePhone.getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public String getDesignation() {
        try { return designation.getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public String getDepartment() {
        try { return department.getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public String getJoinDate() {
        try { return joinDate.getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public String getStatus() {
        try { return statusBadge.getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public boolean isProfilePageLoaded() {
        try {
            WaitUtils.waitForVisibility(driver, employeeName);
            return employeeName.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public int getTabCount() {
        return profileTabs.size();
    }
}
