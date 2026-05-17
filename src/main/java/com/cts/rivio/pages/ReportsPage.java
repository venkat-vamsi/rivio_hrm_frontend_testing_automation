package com.cts.rivio.pages;

import org.openqa.selenium.WebDriver;

/**
 * ReportsPage – stub. The Rivio Angular app does not have a dedicated reports module;
 * reporting visuals live inside the admin dashboard (charts).
 */
public class ReportsPage {

    private final WebDriver driver;

    public ReportsPage(WebDriver driver) { this.driver = driver; }

    public boolean isPageLoaded() { return false; }
    public int getReportCount() { return 0; }
}
