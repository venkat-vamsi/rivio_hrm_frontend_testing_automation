package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class LoginPage {

    private WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────
    // Using formcontrolname (Angular stable attribute) as primary, type as fallback

    @FindBy(css = "input[formcontrolname='email'], input[type='email'], input[name='email']")
    private WebElement emailInput;

    @FindBy(css = "input[formcontrolname='password'], input[type='password'], input[name='password']")
    private WebElement passwordInput;

    // Angular submit button — type='submit' is the most reliable cross-framework selector
    @FindBy(css = "button[type='submit']")
    private WebElement loginButton;

    // Error message shown for invalid credentials
    @FindBy(css = ".error-message, .alert-danger, [class*='error'], p.text-red-500, " +
                  ".p-message-error, .p-toast-message-error")
    private WebElement errorMessage;

    @FindBy(xpath = "//a[contains(text(),'Forgot') or contains(text(),'forgot') or " +
                    "contains(text(),'Reset') or contains(text(),'reset')]")
    private WebElement forgotPasswordLink;

    @FindBy(css = ".spinner, .loading, [class*='spinner'], mat-spinner, .p-progress-spinner")
    private WebElement loadingSpinner;

    // ── Constructor ───────────────────────────────────────────────────────────

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public void enterEmail(String email) {
        WaitUtils.waitForVisibility(driver, emailInput);
        // Use jsSetValue to reliably trigger Angular's form binding
        WaitUtils.jsSetValue(driver, emailInput, email);
        emailInput.sendKeys("");  // nudge Angular change detection
    }

    public void enterPassword(String password) {
        WaitUtils.waitForVisibility(driver, passwordInput);
        WaitUtils.jsSetValue(driver, passwordInput, password);
        passwordInput.sendKeys("");
    }

    public void clickLoginButton() {
        // Dismiss any popup that might be covering the button
        WaitUtils.dismissPopupsIfPresent(driver);
        WaitUtils.safeClick(driver, loginButton);
    }

    /** Full login: fills credentials, submits, waits for Angular to leave auth page. */
    public DashboardPage login(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        clickLoginButton();

        // Wait for URL to leave the auth/login route
        try {
            WaitUtils.waitForUrlNotContains(driver, "auth");
        } catch (Exception ignored) {}

        WaitUtils.waitForAngularLoad(driver);
        WaitUtils.dismissPopupsIfPresent(driver);
        return new DashboardPage(driver);
    }

    /** Login and stay on login page to assert error messages (negative tests). */
    public LoginPage loginExpectingFailure(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        clickLoginButton();
        WaitUtils.hardWait(1500);  // let validation render
        return this;
    }

    public void clickForgotPassword() {
        WaitUtils.waitForClickability(driver, forgotPasswordLink);
        WaitUtils.safeClick(driver, forgotPasswordLink);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getErrorMessage() {
        WaitUtils.waitForVisibility(driver, errorMessage);
        return errorMessage.getText().trim();
    }

    public boolean isErrorDisplayed() {
        try {
            return errorMessage.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public boolean isLoginPageDisplayed() {
        try {
            WaitUtils.waitForVisibility(driver, emailInput);
            return emailInput.isDisplayed() && passwordInput.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getPageTitle() {
        return driver.getTitle();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public DashboardPage loginUsingEnterKey(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        passwordInput.sendKeys(Keys.ENTER);
        try { WaitUtils.waitForUrlNotContains(driver, "auth"); } catch (Exception ignored) {}
        WaitUtils.waitForAngularLoad(driver);
        return new DashboardPage(driver);
    }
}
