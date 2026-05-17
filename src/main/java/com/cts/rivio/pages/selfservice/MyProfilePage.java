package com.cts.rivio.pages.selfservice;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

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

    public MyProfilePage(WebDriver driver) { this.driver = driver; }

    public boolean isPageLoaded() {
        // While loading the page shows the spinner; once data is in, profile content renders.
        try {
            WaitUtils.fluentWait(driver,
                d -> !d.findElements(By.cssSelector("h1")).isEmpty()
                  && !d.findElements(By.xpath("//*[contains(text(),'Job Details')]")).isEmpty(),
                15, 500);
            return true;
        } catch (Exception e) { return false; }
    }

    public String getEmployeeName() {
        try { return driver.findElement(By.cssSelector("h1")).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public boolean isJobDetailsSectionVisible() {
        return !driver.findElements(By.xpath("//*[contains(normalize-space(.),'Job Details')]")).isEmpty();
    }

    public boolean isContactInfoSectionVisible() {
        return !driver.findElements(By.xpath("//*[contains(normalize-space(.),'Contact Information')]")).isEmpty();
    }

    public boolean isSalaryStructureSectionVisible() {
        return !driver.findElements(By.xpath("//*[contains(normalize-space(.),'Salary')]")).isEmpty();
    }

    public String getStatusBadge() {
        try {
            WebElement el = driver.findElement(By.xpath(
                "//span[normalize-space()='ACTIVE' or normalize-space()='INACTIVE']"));
            return el.getText().trim();
        } catch (Exception e) { return ""; }
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
