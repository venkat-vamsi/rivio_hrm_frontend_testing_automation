package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

/**
 * AIPolicyAssistantPage – Page Object for the AI Policy Assistant (Ask Rivi / Policy Q&A).
 *
 * Test Scenarios : Rivio_TS_12_AIPolicyAssistant
 * Test Cases     : Rivio_TC025 – AI returns relevant answers for valid HR policy queries
 *                  Rivio_TC026 – HR/Admin uploads new policy → queryable by employees
 *
 * Route: accessible from Quick Menu → Policy Q&A
 */
public class AIPolicyAssistantPage {

    private WebDriver driver;

    // ── Locators – Chat Interface ─────────────────────────────────────────────

    @FindBy(css = ".page-title, h1, h2, .chat-title")
    private WebElement pageTitle;

    // Query / message input
    @FindBy(css = "input.query-input, textarea.chat-input, input[placeholder*='Ask' i], "
                + "input[placeholder*='question' i], textarea[placeholder*='Type' i]")
    private WebElement queryInput;

    // Send / Ask button
    @FindBy(css = "button.send-btn, button[type='submit'].ask, button[aria-label='Send']")
    private WebElement sendButton;

    // Chat messages container
    @FindBy(css = ".chat-messages, .conversation, [class*='chat-container']")
    private WebElement chatContainer;

    // Individual AI response messages
    @FindBy(css = ".ai-message, .bot-message, [class*='assistant-msg'], .response-bubble")
    private List<WebElement> aiResponses;

    // User sent messages
    @FindBy(css = ".user-message, [class*='user-msg'], .query-bubble")
    private List<WebElement> userMessages;

    // Loading / typing indicator
    @FindBy(css = ".typing-indicator, .loading-dots, [class*='thinking']")
    private WebElement typingIndicator;

    // ── Locators – Policy Management (HR/Admin) ───────────────────────────────

    @FindBy(css = "button.upload-policy, a[class*='upload-policy'], [class*='policy-upload-btn']")
    private WebElement uploadPolicyButton;

    // File input for policy upload
    @FindBy(css = "input[type='file']")
    private WebElement policyFileInput;

    // Policy name / title
    @FindBy(css = "input[formcontrolname='policyName'], input[placeholder*='Policy Name' i]")
    private WebElement policyNameInput;

    // Policy list
    @FindBy(css = ".policy-card, .policy-row, [class*='policy-item']")
    private List<WebElement> policyList;

    // Upload confirm button
    @FindBy(css = "button.confirm-upload, .modal .btn-primary, button[type='submit'].upload")
    private WebElement confirmUploadButton;

    // Success toast
    @FindBy(css = ".toast-success, .alert-success, [class*='success']")
    private WebElement successToast;

    // ── Constructor ───────────────────────────────────────────────────────────

    public AIPolicyAssistantPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Chat Actions ──────────────────────────────────────────────────────────

    public void typeQuery(String query) {
        WaitUtils.waitForVisibility(driver, queryInput);
        queryInput.clear();
        queryInput.sendKeys(query);
    }

    public void clickSend() {
        WaitUtils.waitForClickability(driver, sendButton);
        sendButton.click();
    }

    /**
     * Types a query and sends it; waits for AI response to appear.
     */
    public void askQuestion(String query) {
        typeQuery(query);
        clickSend();
        // Wait for typing indicator to disappear (AI is "thinking")
        try {
            WaitUtils.waitForInvisibility(driver,
                By.cssSelector(".typing-indicator, .loading-dots"));
        } catch (Exception ignored) {}
    }

    // ── Policy Upload Actions (HR) ────────────────────────────────────────────

    public void clickUploadPolicy() {
        WaitUtils.waitForClickability(driver, uploadPolicyButton);
        uploadPolicyButton.click();
    }

    public void uploadPolicyFile(String absoluteFilePath) {
        WaitUtils.waitForVisibility(driver, policyFileInput);
        policyFileInput.sendKeys(absoluteFilePath);
    }

    public void enterPolicyName(String name) {
        WaitUtils.waitForVisibility(driver, policyNameInput);
        policyNameInput.clear();
        policyNameInput.sendKeys(name);
    }

    public void confirmUpload() {
        WaitUtils.waitForClickability(driver, confirmUploadButton);
        confirmUploadButton.click();
    }

    // ── Verifications ─────────────────────────────────────────────────────────

    public boolean isPageLoaded() {
        WaitUtils.waitForVisibility(driver, pageTitle);
        return pageTitle.isDisplayed();
    }

    public boolean isChatInterfaceVisible() {
        try {
            WaitUtils.waitForVisibility(driver, chatContainer);
            return chatContainer.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public int getAiResponseCount() {
        return aiResponses.size();
    }

    public String getLatestAiResponse() {
        if (aiResponses.isEmpty()) return "";
        return aiResponses.get(aiResponses.size() - 1).getText().trim();
    }

    public boolean isResponseNonEmpty() {
        String response = getLatestAiResponse();
        return response != null && !response.isEmpty();
    }

    public int getPolicyCount() {
        return policyList.size();
    }

    public String getSuccessToastMessage() {
        WaitUtils.waitForVisibility(driver, successToast);
        return successToast.getText().trim();
    }

    /**
     * Sends a query using the Enter key (tests keyboard interaction).
     */
    public void askQuestionViaEnter(String query) {
        typeQuery(query);
        queryInput.sendKeys(Keys.ENTER);
        try {
            WaitUtils.waitForInvisibility(driver,
                By.cssSelector(".typing-indicator, .loading-dots"));
        } catch (Exception ignored) {}
    }
}
