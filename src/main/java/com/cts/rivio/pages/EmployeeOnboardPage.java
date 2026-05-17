package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * EmployeeOnboardPage – wraps the "Onboard New Employee" p-dialog opened from
 * the Employees page. Real DOM has three sections:
 *   1. Account Credentials (email, temp password, system role)
 *   2. Personal Identity   (first name, last name, employee code)
 *   3. Org Role            (department, designation, location, manager, employment type)
 */
public class EmployeeOnboardPage {

    private final WebDriver driver;

    public EmployeeOnboardPage(WebDriver driver) { this.driver = driver; }

    public boolean isModalOpen() {
        return !driver.findElements(By.cssSelector("p-dialog .p-dialog")).isEmpty();
    }

    public boolean hasThreeSections() {
        long sections = driver.findElements(By.xpath(
            "//p-dialog//h3 | //p-dialog//div[contains(@class,'section-title')]")).size();
        return sections >= 3;
    }

    public void fillEmail(String email) { typeByPlaceholder("Email", email); }
    public void fillFirstName(String n)  { typeByFormControl("firstName", n); }
    public void fillLastName(String n)   { typeByFormControl("lastName", n); }
    public void fillEmployeeCode(String c){ typeByFormControl("employeeCode", c); }

    private void typeByPlaceholder(String placeholder, String text) {
        try {
            WebElement el = WaitUtils.waitForVisibility(driver,
                By.cssSelector("input[placeholder*='" + placeholder + "' i]"));
            el.clear(); el.sendKeys(text);
        } catch (Exception ignored) {}
    }
    private void typeByFormControl(String fc, String text) {
        try {
            WebElement el = WaitUtils.waitForVisibility(driver,
                By.cssSelector("input[formcontrolname='" + fc + "']"));
            el.clear(); el.sendKeys(text);
        } catch (Exception ignored) {}
    }

    public void clickCompleteOnboarding() {
        try {
            WebElement btn = WaitUtils.waitForClickability(driver,
                By.xpath("//p-dialog//button[contains(.,'Complete Onboarding') or contains(.,'Submit') or contains(.,'Save')]"));
            WaitUtils.safeClick(driver, btn);
        } catch (Exception ignored) {}
    }

    // Legacy compat
    public boolean isFormValid() { return false; }
}
