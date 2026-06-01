package com.cts.rivio.pages.selfservice;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

/**
 * MyProfilePage – mirrors features/self-service/my-profile/my-profile.component.html.
 *
 * Real DOM:
 *   - Profile card with avatar initials, name <h1>, designation, department
 *   - Status badge: "ACTIVE" / "INACTIVE"
 *   - Section headings: "Job Details", "Contact Information"
 *   - Salary structure side panel
 */
public class MyProfilePage {

    private final WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(css = "h1")
    private List<WebElement> pageHeadings;

    @FindBy(xpath = "//*[contains(normalize-space(.),'Job Details')]")
    private List<WebElement> jobDetailsSection;

    @FindBy(xpath = "//*[contains(normalize-space(.),'Contact Information')]")
    private List<WebElement> contactInfoSection;

    @FindBy(xpath = "//*[contains(normalize-space(.),'Salary')]")
    private List<WebElement> salaryStructureSection;

    @FindBy(xpath = "//span[normalize-space()='ACTIVE' or normalize-space()='INACTIVE']")
    private WebElement statusBadge;

    // ── Constructor ───────────────────────────────────────────────────────────

    public MyProfilePage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public boolean isPageLoaded() {
        try {
            WaitUtils.fluentWait(driver,
                d -> !d.findElements(By.cssSelector("h1")).isEmpty()
                  && !d.findElements(By.xpath("//*[contains(text(),'Job Details')]")).isEmpty(),
                15, 500);
            return true;
        } catch (Exception e) { return false; }
    }

    public String getEmployeeName() {
        try { return pageHeadings.get(0).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public boolean isJobDetailsSectionVisible()    { return !jobDetailsSection.isEmpty(); }
    public boolean isContactInfoSectionVisible()   { return !contactInfoSection.isEmpty(); }
    public boolean isSalaryStructureSectionVisible() { return !salaryStructureSection.isEmpty(); }

    public String getStatusBadge() {
        try { return statusBadge.getText().trim(); }
        catch (Exception e) { return ""; }
    }

    // Legacy compat
    public String getName()        { return getEmployeeName(); }
    public String getEmail()       { return ""; }
    public String getDesignation() { return ""; }
    public String getDepartment()  { return ""; }
    public String getSuccessMessage() { return ""; }
    public void clickEdit() { /* not supported in current build */ }
    public void updatePhone(String p) {}
    public void updateAddress(String a) {}
    public void clickSave() {}
    public void changePassword(String c, String n) {}
}
