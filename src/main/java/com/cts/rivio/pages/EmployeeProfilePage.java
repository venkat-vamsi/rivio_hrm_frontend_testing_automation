package com.cts.rivio.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

/**
 * EmployeeProfilePage – admin-side employee profile at /employees/:id.
 * Shares most of the structure with self-service MyProfilePage.
 */
public class EmployeeProfilePage {

    private final WebDriver driver;

    public EmployeeProfilePage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    public boolean isPageLoaded() {
        return driver.getCurrentUrl().matches(".*/employees/\\d+.*")
            && !driver.findElements(By.cssSelector("h1")).isEmpty();
    }

    public String getEmployeeName() {
        try { return driver.findElement(By.cssSelector("h1")).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public boolean isJobDetailsSectionVisible() {
        return !driver.findElements(By.xpath("//*[contains(text(),'Job Details')]")).isEmpty();
    }

    public boolean isContactInfoSectionVisible() {
        return !driver.findElements(By.xpath("//*[contains(text(),'Contact Information')]")).isEmpty();
    }
}
