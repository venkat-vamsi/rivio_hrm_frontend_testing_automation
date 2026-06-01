package com.cts.rivio.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

/**
 * EmployeeProfilePage – admin-side employee profile at /employees/:id.
 * Shares most of the structure with self-service MyProfilePage.
 */
public class EmployeeProfilePage {

    private final WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(css = "h1")
    private List<WebElement> pageHeadings;

    @FindBy(xpath = "//*[contains(text(),'Job Details')]")
    private List<WebElement> jobDetailsSection;

    @FindBy(xpath = "//*[contains(text(),'Contact Information')]")
    private List<WebElement> contactInfoSection;

    // ── Constructor ───────────────────────────────────────────────────────────

    public EmployeeProfilePage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public boolean isPageLoaded() {
        return driver.getCurrentUrl().matches(".*/employees/\\d+.*")
            && !pageHeadings.isEmpty();
    }

    public String getEmployeeName() {
        try { return pageHeadings.get(0).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public boolean isJobDetailsSectionVisible() {
        return !jobDetailsSection.isEmpty();
    }

    public boolean isContactInfoSectionVisible() {
        return !contactInfoSection.isEmpty();
    }
}
