package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

/**
 * SidebarPage – Page Object for the left-side navigation sidebar.
 *
 * The sidebar is present on all authenticated pages, so other Page Objects
 * can hold an instance of SidebarPage for navigation without duplicating locators.
 *
 * XPath concepts demonstrated here:
 *   //nav                     – any <nav> element anywhere
 *   //nav//a[text()='X']      – anchor inside nav with exact text 'X'
 *   contains(@class,'active') – class contains string 'active'
 *   following-sibling         – sibling element after current node
 */
public class SidebarPage {

    private WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(css = "nav.sidebar, aside.sidebar, .side-nav, [class*='sidebar']")
    private WebElement sidebarContainer;

    // All navigation links inside sidebar
    @FindBy(css = "nav a, .sidebar a, .side-nav a, [class*='nav-item'] a")
    private List<WebElement> navLinks;

    // Individual nav items
    @FindBy(xpath = "//nav//a[contains(., 'Dashboard')]")
    private WebElement dashboardLink;

    @FindBy(xpath = "//nav//a[contains(., 'Employees') or contains(.,'Employee Directory')]")
    private WebElement employeesLink;

    @FindBy(xpath = "//nav//a[contains(., 'Leave')]")
    private WebElement leaveLink;

    @FindBy(xpath = "//nav//a[contains(., 'Attendance')]")
    private WebElement attendanceLink;

    @FindBy(xpath = "//nav//a[contains(., 'Payroll')]")
    private WebElement payrollLink;

    @FindBy(xpath = "//nav//a[contains(., 'Recruitment')]")
    private WebElement recruitmentLink;

    @FindBy(xpath = "//nav//a[contains(., 'Company') or contains(.,'Structure')]")
    private WebElement companyLink;

    @FindBy(xpath = "//nav//a[contains(., 'Job Board') or contains(.,'ATS')]")
    private WebElement jobBoardLink;

    // Self-service section (employee view)
    @FindBy(xpath = "//nav//a[contains(., 'My Profile')]")
    private WebElement myProfileLink;

    @FindBy(xpath = "//nav//a[contains(., 'My Leaves') or contains(.,'My Leave')]")
    private WebElement myLeavesLink;

    @FindBy(xpath = "//nav//a[contains(., 'My Payslip')]")
    private WebElement myPayslipsLink;

    @FindBy(xpath = "//nav//a[contains(., 'My Attendance')]")
    private WebElement myAttendanceLink;

    // Active link highlight
    @FindBy(css = ".nav-item.active a, .nav-link.active, [class*='active'] a")
    private WebElement activeLink;

    // Collapse / expand toggle for sidebar sections
    @FindBy(css = ".sidebar-section-toggle, [class*='collapse-btn']")
    private List<WebElement> sectionToggles;

    // ── Constructor ───────────────────────────────────────────────────────────

    public SidebarPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Navigation actions ────────────────────────────────────────────────────

    public DashboardPage clickDashboard() {
        WaitUtils.waitForClickability(driver, dashboardLink);
        dashboardLink.click();
        return new DashboardPage(driver);
    }

    public EmployeeDirectoryPage clickEmployees() {
        WaitUtils.waitForClickability(driver, employeesLink);
        employeesLink.click();
        return new EmployeeDirectoryPage(driver);
    }

    public LeaveDashboardPage clickLeave() {
        WaitUtils.waitForClickability(driver, leaveLink);
        leaveLink.click();
        return new LeaveDashboardPage(driver);
    }

    public AttendancePage clickAttendance() {
        WaitUtils.waitForClickability(driver, attendanceLink);
        attendanceLink.click();
        return new AttendancePage(driver);
    }

    public PayrollDashboardPage clickPayroll() {
        WaitUtils.waitForClickability(driver, payrollLink);
        payrollLink.click();
        return new PayrollDashboardPage(driver);
    }

    public RecruitmentDashboardPage clickRecruitment() {
        WaitUtils.waitForClickability(driver, recruitmentLink);
        recruitmentLink.click();
        return new RecruitmentDashboardPage(driver);
    }

    public CompanyStructurePage clickCompany() {
        WaitUtils.waitForClickability(driver, companyLink);
        companyLink.click();
        return new CompanyStructurePage(driver);
    }

    public JobBoardPage clickJobBoard() {
        WaitUtils.waitForClickability(driver, jobBoardLink);
        jobBoardLink.click();
        return new JobBoardPage(driver);
    }

    // ── Verifications ─────────────────────────────────────────────────────────

    public boolean isSidebarVisible() {
        try {
            return sidebarContainer.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public boolean isLinkPresent(String linkText) {
        return navLinks.stream()
                .anyMatch(el -> el.getText().trim().equalsIgnoreCase(linkText));
    }

    public String getActiveLinkText() {
        try {
            return activeLink.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public int getTotalNavLinks() {
        return navLinks.size();
    }
}
