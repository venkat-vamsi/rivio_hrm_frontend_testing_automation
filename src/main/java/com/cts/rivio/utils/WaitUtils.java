package com.cts.rivio.utils;

import com.cts.rivio.constants.AppConstants;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

public class WaitUtils {

    private WaitUtils() {}

    private static WebDriverWait getWait(WebDriver driver) {
        return new WebDriverWait(driver,
                Duration.ofSeconds(AppConstants.EXPLICIT_WAIT),
                Duration.ofMillis(AppConstants.POLLING_INTERVAL));
    }

    private static WebDriverWait getWait(WebDriver driver, int seconds) {
        return new WebDriverWait(driver,
                Duration.ofSeconds(seconds),
                Duration.ofMillis(AppConstants.POLLING_INTERVAL));
    }

    public static WebElement waitForVisibility(WebDriver driver, By locator) {
        return getWait(driver).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForVisibility(WebDriver driver, WebElement element) {
        return getWait(driver).until(ExpectedConditions.visibilityOf(element));
    }

    public static WebElement waitForClickability(WebDriver driver, By locator) {
        return getWait(driver).until(ExpectedConditions.elementToBeClickable(locator));
    }

    public static WebElement waitForClickability(WebDriver driver, WebElement element) {
        return getWait(driver).until(ExpectedConditions.elementToBeClickable(element));
    }

    public static boolean waitForInvisibility(WebDriver driver, By locator) {
        return getWait(driver).until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public static boolean waitForUrlContains(WebDriver driver, String urlFragment) {
        return getWait(driver).until(ExpectedConditions.urlContains(urlFragment));
    }

    public static boolean waitForUrlNotContains(WebDriver driver, String urlFragment) {
        return getWait(driver).until(d -> !d.getCurrentUrl().contains(urlFragment));
    }

    public static Alert waitForAlert(WebDriver driver) {
        return getWait(driver).until(ExpectedConditions.alertIsPresent());
    }

    public static List<WebElement> waitForAllElements(WebDriver driver, By locator) {
        return getWait(driver).until(
            ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    public static boolean waitForTextInElement(WebDriver driver, WebElement element, String text) {
        return getWait(driver).until(
            ExpectedConditions.textToBePresentInElement(element, text));
    }

    public static <T> T fluentWait(WebDriver driver, Function<WebDriver, T> condition,
                                   int timeoutSeconds, int pollMillis) {
        FluentWait<WebDriver> wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeoutSeconds))
                .pollingEvery(Duration.ofMillis(pollMillis))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);
        return wait.until(condition);
    }

    // ── Angular-specific wait ──────────────────────────────────────────────────

    /**
     * Waits for Angular to finish rendering.
     * Works for Angular 2+ apps. Falls back to document.readyState for non-Angular pages.
     */
    public static void waitForAngularLoad(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        // First wait for document ready
        waitForPageLoad(driver);
        // Then wait for Angular testability
        try {
            new WebDriverWait(driver, Duration.ofSeconds(AppConstants.ANGULAR_WAIT),
                    Duration.ofMillis(300))
                .until(d -> {
                    try {
                        Object result = js.executeScript(
                            "try {" +
                            "  var ta = window.getAllAngularTestabilities();" +
                            "  return ta.every(function(t){ return t.isStable(); });" +
                            "} catch(e) { return true; }"
                        );
                        return Boolean.TRUE.equals(result);
                    } catch (Exception e) {
                        return true;
                    }
                });
        } catch (Exception ignored) {}
    }

    /**
     * Waits for document.readyState === 'complete'.
     */
    public static void waitForPageLoad(WebDriver driver) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(AppConstants.PAGE_LOAD_WAIT),
                    Duration.ofMillis(500))
                .until(d -> {
                    try {
                        return "complete".equals(
                            ((JavascriptExecutor) d).executeScript("return document.readyState"));
                    } catch (Exception e) {
                        return false;
                    }
                });
        } catch (Exception ignored) {}
    }

    // ── Popup / overlay dismissal ─────────────────────────────────────────────

    /**
     * Dismisses any overlays that would block test interaction:
     * JS alerts, cookie banners, Angular CDK overlays, PrimeNG dialogs, tour tips.
     */
    public static void dismissPopupsIfPresent(WebDriver driver) {
        // 1. JS alert
        try {
            Alert alert = new WebDriverWait(driver, Duration.ofSeconds(2))
                .until(ExpectedConditions.alertIsPresent());
            System.out.println("[Popup] Dismissed JS alert: " + alert.getText());
            alert.dismiss();
        } catch (Exception ignored) {}

        // 2. Cookie consent banner
        dismissElementIfPresent(driver,
            By.cssSelector("button[class*='accept'], button[id*='accept-cookie'], " +
                           "[class*='cookie'] button, [id*='cookie-banner'] button"));

        // 3. PrimeNG dialog close button
        dismissElementIfPresent(driver,
            By.cssSelector(".p-dialog-header-close, .p-dialog .p-dialog-header button"));

        // 4. Angular CDK overlay backdrop (blocks clicks)
        dismissElementIfPresent(driver,
            By.cssSelector(".cdk-overlay-backdrop"));

        // 5. Generic modal dismiss buttons (tour tips, onboarding overlays)
        dismissElementIfPresent(driver,
            By.cssSelector(".shepherd-cancel-icon, .tour-close, [class*='dismiss'], " +
                           "[aria-label='Close'], [aria-label='close']"));
    }

    private static void dismissElementIfPresent(WebDriver driver, By locator) {
        try {
            List<WebElement> els = driver.findElements(locator);
            for (WebElement el : els) {
                if (el.isDisplayed()) {
                    try {
                        el.click();
                        System.out.println("[Popup] Dismissed: " + locator);
                    } catch (Exception e) {
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
                    }
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    // ── JavaScript helpers ────────────────────────────────────────────────────

    /**
     * Clicks an element using JavaScript — bypasses overlay/visibility issues.
     */
    public static void jsClick(WebDriver driver, WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    public static void jsClick(WebDriver driver, By locator) {
        WebElement el = waitForVisibility(driver, locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
    }

    /**
     * Sets value on an input using JavaScript and fires input + change events.
     * Needed for Angular date pickers and PrimeNG inputs that intercept keystrokes.
     */
    public static void jsSetValue(WebDriver driver, WebElement element, String value) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(
            "arguments[0].value = arguments[1];" +
            "arguments[0].dispatchEvent(new Event('input', {bubbles:true}));" +
            "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
            element, value
        );
    }

    /**
     * Scroll element into view before clicking — avoids ElementClickInterceptedException.
     */
    public static void scrollAndClick(WebDriver driver, WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({block:'center'});", element);
        hardWait(300);
        try {
            waitForClickability(driver, element).click();
        } catch (Exception e) {
            jsClick(driver, element);
        }
    }

    /**
     * Safe click: tries regular click first, falls back to JS click.
     */
    public static void safeClick(WebDriver driver, WebElement element) {
        try {
            waitForClickability(driver, element).click();
        } catch (ElementClickInterceptedException | TimeoutException e) {
            jsClick(driver, element);
        }
    }

    // ── PrimeNG dropdown helper ───────────────────────────────────────────────

    /**
     * Selects an option in a PrimeNG p-dropdown or p-select component.
     * PrimeNG uses custom HTML — not a native <select>, so Selenium's Select class won't work.
     *
     * Strategy: click the dropdown trigger to open the panel, then click the matching item.
     */
    public static void selectPrimeNgOption(WebDriver driver, WebElement dropdownContainer, String optionText) {
        // Open the dropdown
        safeClick(driver, dropdownContainer);
        hardWait(400);

        // null / empty / "AUTO" sentinel → pick the first non-disabled item.
        // We must NOT fall through to the text-match branch because building
        // an XPath with "normalize-space(.)='null'" matches nothing and the
        // dropdown sits open with no selection (causing the next form field
        // to be unreachable — which is what made onboarding tests "stick").
        if (optionText == null || optionText.isEmpty() || "AUTO".equalsIgnoreCase(optionText)) {
            By firstOption = By.xpath(
                "(//div[contains(@class,'p-select-overlay') or contains(@class,'p-dropdown-panel') "
              + "    or contains(@class,'p-overlay')]"
              + "  //li[(contains(@class,'p-select-option') or contains(@class,'p-dropdown-item')) "
              + "      and not(contains(@class,'p-disabled'))])[1]"
            );
            try {
                WebElement item = getWait(driver, 10).until(
                    ExpectedConditions.elementToBeClickable(firstOption));
                item.click();
                hardWait(250);
                return;
            } catch (Exception e) {
                // Last-ditch close — Escape so the panel doesn't block the next field
                try { dropdownContainer.sendKeys(Keys.ESCAPE); } catch (Exception ignored) {}
                throw new RuntimeException("AUTO selection failed: no clickable option found in dropdown panel", e);
            }
        }

        // Wait for the dropdown panel and click matching item
        By itemLocator = By.xpath(
            "//li[contains(@class,'p-dropdown-item') and normalize-space(.)='" + optionText + "'] | " +
            "//li[contains(@class,'p-select-option') and normalize-space(.)='" + optionText + "'] | " +
            "//p-dropdownitem//li[normalize-space(.)='" + optionText + "']"
        );
        try {
            WebElement item = getWait(driver, 10).until(
                ExpectedConditions.elementToBeClickable(itemLocator));
            item.click();
        } catch (Exception e) {
            // Fallback: find visible option text anywhere in the overlay
            By fallback = By.xpath(
                "//*[contains(@class,'p-overlay') or contains(@class,'p-dropdown-panel')"
              + "    or contains(@class,'p-select-overlay')]"
              + "//*[normalize-space(text())='" + optionText + "']"
            );
            waitForClickability(driver, fallback).click();
        }
    }

    /**
     * Selects a PrimeNG dropdown option by locating the dropdown wrapper first.
     */
    public static void selectPrimeNgOption(WebDriver driver, By dropdownLocator, String optionText) {
        WebElement dropdown = waitForClickability(driver, dropdownLocator);
        selectPrimeNgOption(driver, dropdown, optionText);
    }

    // ── Hard wait ─────────────────────────────────────────────────────────────

    public static void hardWait(int millis) {
        try { Thread.sleep(millis); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Returns true if the given locator becomes present within `timeoutSeconds`,
     * false otherwise. Does NOT throw. Use this in `isPageLoaded()` style checks
     * to give Angular time to paint before the test asserts.
     */
    public static boolean waitForPresence(WebDriver driver, By locator, int timeoutSeconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds),
                    Duration.ofMillis(AppConstants.POLLING_INTERVAL))
                .until(ExpectedConditions.presenceOfElementLocated(locator));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Convenience: wait up to N seconds for the heading text to appear. */
    public static boolean waitForH1Text(WebDriver driver, String headingText, int timeoutSeconds) {
        return waitForPresence(driver,
            By.xpath("//h1[normalize-space()='" + headingText + "'] | //h1[contains(.,'" + headingText + "')]"),
            timeoutSeconds);
    }

    /**
     * Polls the browser URL until it stays the same for `stabilityMs` consecutive
     * milliseconds, OR `maxSeconds` elapses. Returns the final URL.
     *
     * This is CRITICAL for Angular roleGuard tests: when a forbidden URL is
     * navigated to, Angular renders the initial route then the guard's
     * router.parseUrl() redirect fires asynchronously. Reading the URL
     * immediately captures the pre-redirect address; reading after stability
     * captures the post-redirect (correct) address.
     */
    public static String waitForUrlToBeStable(WebDriver driver, int maxSeconds, long stabilityMs) {
        long deadline = System.currentTimeMillis() + maxSeconds * 1000L;
        String lastUrl = "";
        long lastChangeAt = System.currentTimeMillis();
        try { lastUrl = driver.getCurrentUrl(); } catch (Exception ignored) {}

        while (System.currentTimeMillis() < deadline) {
            try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            String now;
            try { now = driver.getCurrentUrl(); } catch (Exception e) { continue; }
            if (!now.equals(lastUrl)) {
                lastUrl = now;
                lastChangeAt = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - lastChangeAt >= stabilityMs) {
                return now;
            }
        }
        return lastUrl;
    }

    /** Default: wait up to 12s for URL to stay stable for 1200ms.
     *  Bumped from 8s/800ms after roleGuard redirects sometimes ran longer
     *  than initial 800ms window (causing false "you reached forbidden URL" fails). */
    public static String waitForUrlToBeStable(WebDriver driver) {
        return waitForUrlToBeStable(driver, 12, 1200);
    }
}
