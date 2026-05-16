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

public class DriverFactory {

    private static ThreadLocal<WebDriver> tlDriver = new ThreadLocal<>();

    private DriverFactory() {}

    public static WebDriver getDriver() {
        return tlDriver.get();
    }

    public static void initDriver(String browser) {
        WebDriver driver;

        switch (browser.toLowerCase().trim()) {

            case AppConstants.BROWSER_FIREFOX:
                WebDriverManager.firefoxdriver().setup();
                driver = new FirefoxDriver();
                break;

            case AppConstants.BROWSER_EDGE:
                WebDriverManager.edgedriver().setup();
                driver = new EdgeDriver();
                break;

            case AppConstants.BROWSER_CHROME:
            default:
                WebDriverManager.chromedriver().setup();
                ChromeOptions options = buildChromeOptions();
                driver = new ChromeDriver(options);
                break;
        }

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(AppConstants.PAGE_LOAD_WAIT));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(AppConstants.EXPLICIT_WAIT));
        driver.manage().window().maximize();

        tlDriver.set(driver);
    }

    private static ChromeOptions buildChromeOptions() {
        ChromeOptions options = new ChromeOptions();

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

        // Prevent "Save password?" and "Translate page?" dialogs
        options.addArguments("--disable-save-password-bubble");
        options.addArguments("--disable-translate");

        // Suppress Chrome's "controlled by automated test software" banner
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches",
            new String[]{"enable-automation", "enable-logging"});
        options.setExperimentalOption("useAutomationExtension", false);

        // Disable downloads dialog
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("safebrowsing.enabled", false);
        options.setExperimentalOption("prefs", prefs);

        return options;
    }

    public static void quitDriver() {
        if (tlDriver.get() != null) {
            tlDriver.get().quit();
            tlDriver.remove();
        }
    }
}
