package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.constants.AppConstants;
import com.cts.rivio.pages.DashboardPage;
import com.cts.rivio.utils.ExtentManager;
import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * DashboardTest — Admin Overview Dashboard.
 *
 * Naming pattern: {@code dash_<scenario>}.
 *
 *   dash_kpiCardsNavigate       – KPI cards route to their feature pages
 *   dash_bug_askRivioCollapsed  – Sidebar collapse breaks Ask Rivi label
 */
public class DashboardTest extends BaseTest {

    @Override protected String getRole() { return ROLE_ADMIN; }

    private DashboardPage dashboard;

    @BeforeMethod(alwaysRun = true)
    public void openDashboard() {
        driver.get(AppConstants.DASHBOARD_URL);
        WaitUtils.waitForUrlContains(driver, "/dashboard");
        WaitUtils.waitForH1Text(driver, "Admin Overview", 15);
        WaitUtils.waitForAngularLoad(driver);
        dashboard = new DashboardPage(driver);
    }

    @Test(priority = 1, groups = {"smoke", "regression", "positive"},
          description = "dash_kpiCardsNavigate – KPI cards route to /attendance, /leave, /payroll")
    public void dash_kpiCardsNavigate() {
        // Present Today → /attendance
        dashboard.clickKpiCard("Present Today");
        String url = WaitUtils.waitForUrlToBeStable(driver);
        Assert.assertTrue(url.contains("/attendance"),
                "Present Today should navigate to /attendance. URL: " + url);

        // On Leave Today → /leave
        driver.get(AppConstants.DASHBOARD_URL);
        WaitUtils.waitForUrlToBeStable(driver);
        dashboard = new DashboardPage(driver);
        dashboard.clickKpiCard("On Leave Today");
        url = WaitUtils.waitForUrlToBeStable(driver);
        Assert.assertTrue(url.contains("/leave"),
                "On Leave Today should navigate to /leave. URL: " + url);

        // Active Pay Cycles → /payroll
        driver.get(AppConstants.DASHBOARD_URL);
        WaitUtils.waitForUrlToBeStable(driver);
        dashboard = new DashboardPage(driver);
        dashboard.clickKpiCard("Active Pay Cycles");
        url = WaitUtils.waitForUrlToBeStable(driver);
        Assert.assertTrue(url.contains("/payroll"),
                "Active Pay Cycles should navigate to /payroll. URL: " + url);

        ExtentManager.getTest().pass("All 3 KPI cards navigate correctly");
    }

    /**
     * Bug: when the sidebar is collapsed to its narrow rail (~80px), the
     * Ask Rivi gradient button's inner "Ask Rivi" label is NOT given the
     * {@code opacity-0} class that every other sidebar item gets — so the
     * text squashes/clips inside the narrow rail instead of disappearing.
     */
    @Test(priority = 2, groups = {"bug", "regression", "negative"},
          description = "dash_bug_askRivioCollapsed – Ask Rivi label must hide when sidebar collapses")
    public void dash_bug_askRivioCollapsed() {
        WebElement collapseBtn = null;
        try {
            collapseBtn = driver.findElement(By.xpath(
                "//aside//button[.//i[contains(@class,'pi-angle-left')]] | "
              + "//aside//button[contains(@class,'sidebar-toggle')]"));
        } catch (Exception ignored) {}
        if (collapseBtn != null) {
            WaitUtils.scrollAndClick(driver, collapseBtn);
            WaitUtils.hardWait(600);
        }

        int asideWidth = 0;
        try {
            asideWidth = driver.findElement(By.cssSelector("aside")).getSize().getWidth();
        } catch (Exception ignored) {}
        ExtentManager.getTest().info("Sidebar width after collapse: " + asideWidth + "px");

        List<WebElement> labels = driver.findElements(By.xpath(
            "//aside//a[contains(@href,'/ask-rivi') or contains(@ng-reflect-router-link,'/ask-rivi')]"
          + "//span[contains(normalize-space(.),'Ask Rivi')]"));
        Assert.assertFalse(labels.isEmpty(),
                "Ask Rivio button not found in the sidebar");
        WebElement label = labels.get(0);

        int labelWidth = label.getSize().getWidth();
        int labelHeight = label.getSize().getHeight();
        String labelClass = label.getAttribute("class");

        boolean labelProperlyHidden =
               (labelClass != null && labelClass.contains("opacity-0"))
            || labelWidth == 0
            || labelHeight == 0
            || !label.isDisplayed();

        Assert.assertTrue(labelProperlyHidden,
                "Sidebar collapsed (width ≈ " + asideWidth + "px) but Ask Rivi label "
              + "is still " + labelWidth + "x" + labelHeight + " inside the narrow rail. "
              + "It must get the same opacity-0 treatment as other nav items.");
        ExtentManager.getTest().pass("Ask Rivio label correctly collapses with the sidebar");
    }
}
