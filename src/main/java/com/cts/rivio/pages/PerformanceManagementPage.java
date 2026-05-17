package com.cts.rivio.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * PerformanceManagementPage – stub.
 * The Rivio Angular app does not implement a Performance Management module.
 * Tests targeting it should be treated as known-gap.
 */
public class PerformanceManagementPage {

    private final WebDriver driver;

    public PerformanceManagementPage(WebDriver driver) { this.driver = driver; }

    public boolean isPageLoaded() {
        return driver.getCurrentUrl().contains("performance")
            || !driver.findElements(By.xpath("//*[contains(text(),'Performance')]")).isEmpty();
    }

    public boolean isGoalSettingInterfaceVisible() { return false; }
}
