package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * SidebarPage – wraps core/layout/sidebar/sidebar.component.html.
 *
 * Sidebar entries are <a [routerLink]="...">; Angular renders these with an
 * href attribute exactly equal to the route. Self-service items live under a
 * collapsible group.
 *
 * IMPORTANT: We use exact-match selectors (`href='/attendance'`), NOT ends-with
 * (`href$='/attendance'`), because `/self-service/attendance` would falsely
 * satisfy the ends-with match.
 */
public class SidebarPage {

    private final WebDriver driver;

    public SidebarPage(WebDriver driver) { this.driver = driver; }

    /** Exact-match locator — avoids /self-service/X matching /X. */
    private By itemByRoute(String route) {
        return By.cssSelector(
            "aside a[href='" + route + "'], "
          + "aside a[ng-reflect-router-link='" + route + "'], "
          + "aside a[routerlink='" + route + "']");
    }

    /** Default: 5s wait. Use when expecting the item to appear. */
    public boolean isItemVisible(String route) {
        return WaitUtils.waitForPresence(driver, itemByRoute(route), 5);
    }

    /** Fast check (no wait) — use when expecting the item to NOT be there. */
    public boolean isItemImmediatelyVisible(String route) {
        return !driver.findElements(itemByRoute(route)).isEmpty();
    }

    public void clickItem(String route) {
        WebElement el = WaitUtils.waitForClickability(driver, itemByRoute(route));
        WaitUtils.safeClick(driver, el);
        WaitUtils.waitForAngularLoad(driver);
    }

    public void openSelfServiceGroup() {
        try {
            WebElement btn = driver.findElement(By.xpath(
                "//aside//button[.//span[normalize-space()='Self Service']]"));
            if (!isItemImmediatelyVisible("/self-service/profile")) {
                WaitUtils.safeClick(driver, btn);
                WaitUtils.hardWait(400);
            }
        } catch (Exception ignored) {}
    }

    public List<WebElement> getVisibleNavLinks() {
        return driver.findElements(By.cssSelector("aside nav a[routerlink], aside nav a[href]"));
    }

    public boolean isAskRiviVisible() {
        return WaitUtils.waitForPresence(driver,
            By.cssSelector("aside a[href='/ask-rivi'], "
                + "aside a[ng-reflect-router-link='/ask-rivi'], "
                + "aside a[routerlink='/ask-rivi']"), 5);
    }

    /** Waits for the sidebar's main nav to render at least one entry. */
    public void waitForSidebarToRender() {
        WaitUtils.waitForPresence(driver, By.cssSelector("aside nav a"), 10);
    }
}
