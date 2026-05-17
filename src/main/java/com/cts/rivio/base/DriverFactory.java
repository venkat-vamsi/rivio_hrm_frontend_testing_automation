package com.cts.rivio.base;

import com.cts.rivio.constants.AppConstants;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DriverFactory – manages WebDriver lifecycle (one per thread via ThreadLocal).
 *
 * IMPORTANT: WebDriverManager.setup() is called ONCE per JVM (not per test),
 * because parallel threads racing on the chromedriver download file lock was
 * causing entire test classes to be marked as skipped.
 */
public class DriverFactory {

    private static final ThreadLocal<WebDriver> tlDriver = new ThreadLocal<>();
    private static final AtomicBoolean wdmInitialized = new AtomicBoolean(false);
    private static final Object wdmLock = new Object();

    private DriverFactory() {}

    public static WebDriver getDriver() { return tlDriver.get(); }

    /** Sets up the matching driver binary exactly once per JVM. */
    private static void ensureWebDriverManagerSetup(String browser) {
        if (wdmInitialized.get()) return;
        synchronized (wdmLock) {
            if (wdmInitialized.get()) return;
            switch (browser.toLowerCase().trim()) {
                case AppConstants.BROWSER_FIREFOX: WebDriverManager.firefoxdriver().setup(); break;
                case AppConstants.BROWSER_EDGE:    WebDriverManager.edgedriver().setup();    break;
                case AppConstants.BROWSER_CHROME:
                default:                            WebDriverManager.chromedriver().setup();  break;
            }
            wdmInitialized.set(true);
            System.out.println("[DriverFactory] WebDriverManager setup completed for " + browser);
        }
    }

    public static void initDriver(String browser) {
        ensureWebDriverManagerSetup(browser);

        // Retry the actual driver creation up to 3 times. Chrome occasionally
        // fails to bind a debug port on a busy machine — a fresh attempt usually
        // succeeds. Without retry, the @BeforeMethod fails and TestNG cascade-
        // skips the rest of the class.
        Exception last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                WebDriver driver = createDriver(browser);
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
                driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(AppConstants.PAGE_LOAD_WAIT));
                driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(AppConstants.EXPLICIT_WAIT));
                driver.manage().window().maximize();
                tlDriver.set(driver);
                return;
            } catch (Exception e) {
                last = e;
                System.err.println("[DriverFactory] Driver init attempt " + attempt + "/3 failed: " + e.getMessage());
                try { Thread.sleep(1500L * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        throw new RuntimeException("Could not create WebDriver after 3 attempts", last);
    }

    private static WebDriver createDriver(String browser) {
        switch (browser.toLowerCase().trim()) {
            case AppConstants.BROWSER_FIREFOX: return new FirefoxDriver();
            case AppConstants.BROWSER_EDGE:    return new EdgeDriver();
            case AppConstants.BROWSER_CHROME:
            default:                            return new ChromeDriver(buildChromeOptions());
        }
    }

    private static ChromeOptions buildChromeOptions() {
        ChromeOptions options = new ChromeOptions();

        // Headless support: enable with -Dheadless=true or in config.properties.
        String headless = System.getProperty("headless",
                com.cts.rivio.utils.ConfigReader.getProperty("headless", "false"));
        if ("true".equalsIgnoreCase(headless)) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1920,1080");
        }

        // Stability
        options.addArguments("--start-maximized");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--remote-allow-origins=*");

        // Suppress browser UI popups that interrupt tests
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--disable-infobars");
        options.addArguments("--disable-extensions");
        options.addArguments("--no-first-run");
        options.addArguments("--no-default-browser-check");

        // Suppress Chrome's password-leak / password-check / autofill bubbles
        options.addArguments("--disable-features=PasswordCheck,PasswordLeakDetection,AutofillServerCommunication,InterestFeedContentSuggestions");

        // Prevent "Save password?" and "Translate page?" dialogs
        options.addArguments("--disable-save-password-bubble");
        options.addArguments("--disable-translate");

        // Suppress Chrome's "controlled by automated test software" banner
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches",
            new String[]{"enable-automation", "enable-logging"});
        options.setExperimentalOption("useAutomationExtension", false);

        // Disable downloads dialog + password leak detection in prefs
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.password_manager_leak_detection", false);
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("safebrowsing.enabled", false);
        options.setExperimentalOption("prefs", prefs);

        return options;
    }

    public static void quitDriver() {
        if (tlDriver.get() != null) {
            try { tlDriver.get().quit(); } catch (Exception ignored) {}
            tlDriver.remove();
        }
    }
}
