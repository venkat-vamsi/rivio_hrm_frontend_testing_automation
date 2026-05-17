package com.cts.rivio.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.List;

/**
 * AuditLogPage – Rivio_Angular does NOT expose a dedicated audit-log page yet.
 * This stub exists so legacy SecurityAuditTest still compiles. All assertions
 * should treat audit-log absence as a known gap, not a test failure.
 */
public class AuditLogPage {

    private final WebDriver driver;

    public AuditLogPage(WebDriver driver) { this.driver = driver; }

    public boolean isPageLoaded() {
        return driver.getCurrentUrl().contains("audit");
    }

    public void searchByKeyword(String keyword) { /* no-op */ }

    public int getAuditLogRowCount() {
        List<?> rows = driver.findElements(By.cssSelector("p-table tbody tr, table tbody tr"));
        return rows.size();
    }

    public String getAuditLogRowText(int idx) {
        try { return driver.findElements(By.cssSelector("p-table tbody tr, table tbody tr"))
                .get(idx).getText(); }
        catch (Exception e) { return ""; }
    }

    public boolean isEventPresentInLog(String keyword) {
        return !driver.findElements(By.xpath("//*[contains(text(),'" + keyword + "')]")).isEmpty();
    }
}
