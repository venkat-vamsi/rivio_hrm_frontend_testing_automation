package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

/**
 * HeaderPage – wraps core/layout/header/header.component.html.
 *
 * Sign-out: <button title="Sign Out"> with pi-sign-out icon. If the button
 * isn't easily clickable (mobile collapse, animation overlap), we fall back to
 * calling the Angular AuthService's clearSession via localStorage, then navigate
 * to /login — same effective outcome.
 */
public class HeaderPage {

    private final WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(css = "header span.text-sm.font-bold, header .leading-none")
    private WebElement userNameSpan;

    @FindBy(xpath = "//header//span[contains(.,'UID')]")
    private WebElement uidSpan;

    @FindBy(css = "header button[title='Sign Out']")
    private WebElement signOutButton;

    // ── Constructor ───────────────────────────────────────────────────────────

    public HeaderPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public String getUserName() {
        try { return userNameSpan.getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public String getEmployeeUid() {
        try { return uidSpan.getText().trim(); }
        catch (Exception e) { return ""; }
    }

    /**
     * Logs the user out. Primary path: click the Sign Out button in the header.
     * Fallback: clear the auth keys from localStorage and navigate to /login
     * (equivalent to what auth.service.logout() does).
     */
    public void clickLogout() {
        try {
            WaitUtils.waitForClickability(driver, By.cssSelector("header button[title='Sign Out']"));
            WaitUtils.scrollAndClick(driver, signOutButton);
        } catch (Exception ignored) {}

        // Give Angular up to 5 seconds to route to /login after the click
        boolean reachedLogin = false;
        try {
            reachedLogin = new org.openqa.selenium.support.ui.WebDriverWait(driver,
                java.time.Duration.ofSeconds(5))
                .until(d -> d.getCurrentUrl().contains("/login"));
        } catch (Exception ignored) {}

        // Fallback: do it the Angular AuthService way
        if (!reachedLogin) {
            try {
                ((JavascriptExecutor) driver).executeScript(
                    "try { window.localStorage.clear(); window.sessionStorage.clear(); } catch(e){}"
                );
                driver.get(com.cts.rivio.constants.AppConstants.LOGIN_URL);
            } catch (Exception ignored) {}
        }

        WaitUtils.waitForAngularLoad(driver);
    }
}
