package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

/**
 * HeaderPage – Page Object for the top navigation header bar.
 *
 * The header typically contains:
 *   – Logo
 *   – Page/module title
 *   – User profile / avatar
 *   – Notification bell
 *   – Logout
 *   – AI assistant (Ask Rivi chatbot) button
 */
public class HeaderPage {

    private WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(css = "header, .top-bar, .navbar, [class*='header']")
    private WebElement headerContainer;

    @FindBy(css = "header .logo, .navbar-brand, img[alt*='logo' i], img[alt*='rivio' i]")
    private WebElement logo;

    @FindBy(css = "header .page-title, header h1, header h2, .breadcrumb")
    private WebElement pageTitle;

    @FindBy(css = ".user-menu, .profile-dropdown, [class*='user-info']")
    private WebElement userMenu;

    @FindBy(css = ".notification-icon, [aria-label*='notification' i], button.bell")
    private WebElement notificationIcon;

    // Logged-in user's display name
    @FindBy(css = ".user-name, .logged-in-user, [class*='username']")
    private WebElement loggedInUserName;

    // Logout – may be inside a dropdown
    @FindBy(xpath = "//*[@id='user-menu']//a[contains(text(),'Logout')]"
                  + " | //button[contains(text(),'Logout')]"
                  + " | //li[contains(text(),'Logout')]")
    private WebElement logoutOption;

    // AI Chatbot / Ask Rivi button
    @FindBy(css = ".ask-rivi, [class*='chatbot'], button[aria-label*='rivi' i]")
    private WebElement askRiviButton;

    // ── Constructor ───────────────────────────────────────────────────────────

    public HeaderPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public void clickUserMenu() {
        WaitUtils.waitForClickability(driver, userMenu);
        userMenu.click();
    }

    public LoginPage logout() {
        clickUserMenu();
        WaitUtils.waitForClickability(driver, logoutOption);
        logoutOption.click();
        return new LoginPage(driver);
    }

    public void clickNotifications() {
        WaitUtils.waitForClickability(driver, notificationIcon);
        notificationIcon.click();
    }

    public void clickAskRivi() {
        WaitUtils.waitForClickability(driver, askRiviButton);
        askRiviButton.click();
    }

    public void clickLogo() {
        WaitUtils.waitForClickability(driver, logo);
        logo.click();
    }

    // ── Verifications ─────────────────────────────────────────────────────────

    public boolean isHeaderDisplayed() {
        try {
            return headerContainer.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public String getLoggedInUserName() {
        WaitUtils.waitForVisibility(driver, loggedInUserName);
        return loggedInUserName.getText().trim();
    }

    public String getPageTitle() {
        try {
            WaitUtils.waitForVisibility(driver, pageTitle);
            return pageTitle.getText().trim();
        } catch (Exception e) {
            return driver.getTitle();
        }
    }

    public boolean isLogoDisplayed() {
        try {
            return logo.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }
}
