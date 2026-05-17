package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * AIPolicyAssistantPage – "Ask Rivi" AI feature at /ask-rivi.
 * Available to all non-Employee roles.
 */
public class AIPolicyAssistantPage {

    private final WebDriver driver;

    public AIPolicyAssistantPage(WebDriver driver) { this.driver = driver; }

    public boolean isPageLoaded() {
        return driver.getCurrentUrl().contains("/ask-rivi")
            && !driver.findElements(By.cssSelector("textarea, input[type='text']")).isEmpty();
    }

    public void askQuestion(String text) {
        WebElement input = WaitUtils.waitForVisibility(driver,
            By.cssSelector("textarea, input[type='text']"));
        input.sendKeys(text);
    }

    public boolean hasResponseArea() {
        return !driver.findElements(By.cssSelector("[class*='message'], [class*='response'], [class*='answer']")).isEmpty();
    }
}
