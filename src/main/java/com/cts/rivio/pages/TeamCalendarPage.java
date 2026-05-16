package com.cts.rivio.pages;

import com.cts.rivio.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

/**
 * TeamCalendarPage – Page Object for the Manager's Team Calendar view.
 *
 * Test Scenario : Rivio_TS_06_TeamCalendar
 * Test Cases    : Rivio_TC014
 *
 * The team calendar lets a manager see all approved team leaves,
 * apply project/team filters, and identify resource conflicts.
 */
public class TeamCalendarPage {

    private WebDriver driver;

    // ── Locators ──────────────────────────────────────────────────────────────

    @FindBy(css = ".page-title, h1, h2")
    private WebElement pageTitle;

    // Calendar grid containing leave entries
    @FindBy(css = ".team-calendar, .calendar-grid, [class*='team-cal']")
    private WebElement calendarGrid;

    // Individual calendar day cells
    @FindBy(css = ".calendar-day, [class*='cal-day'], td.day-cell")
    private List<WebElement> calendarDays;

    // Leave entry chips/badges inside calendar
    @FindBy(css = ".leave-entry, .leave-chip, [class*='leave-badge']")
    private List<WebElement> leaveEntries;

    // Conflict indicator (days where multiple people are on leave)
    @FindBy(css = ".conflict-indicator, [class*='conflict'], .resource-gap")
    private List<WebElement> conflictIndicators;

    // Team / Project filter dropdown
    @FindBy(css = "select[name='team'], select.team-filter, select[formcontrolname='team']")
    private WebElement teamFilter;

    @FindBy(css = "select[name='project'], select.project-filter, select[formcontrolname='project']")
    private WebElement projectFilter;

    // Month navigation buttons
    @FindBy(css = "button.prev-month, [aria-label='Previous month'], [class*='prev-btn']")
    private WebElement prevMonthBtn;

    @FindBy(css = "button.next-month, [aria-label='Next month'], [class*='next-btn']")
    private WebElement nextMonthBtn;

    // Calendar title showing current month/year
    @FindBy(css = ".calendar-title, [class*='cal-header'], .month-year-label")
    private WebElement calendarTitle;

    // Employee name labels in the calendar rows (for each team member)
    @FindBy(css = ".employee-row-label, [class*='emp-label'], .team-member-name")
    private List<WebElement> teamMemberLabels;

    // ── Constructor ───────────────────────────────────────────────────────────

    public TeamCalendarPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public void filterByTeam(String teamName) {
        WaitUtils.waitForVisibility(driver, teamFilter);
        new org.openqa.selenium.support.ui.Select(teamFilter).selectByVisibleText(teamName);
    }

    public void filterByProject(String projectName) {
        WaitUtils.waitForVisibility(driver, projectFilter);
        new org.openqa.selenium.support.ui.Select(projectFilter).selectByVisibleText(projectName);
    }

    public void clickPreviousMonth() {
        WaitUtils.waitForClickability(driver, prevMonthBtn);
        prevMonthBtn.click();
    }

    public void clickNextMonth() {
        WaitUtils.waitForClickability(driver, nextMonthBtn);
        nextMonthBtn.click();
    }

    // ── Verifications ─────────────────────────────────────────────────────────

    public boolean isPageLoaded() {
        WaitUtils.waitForVisibility(driver, pageTitle);
        return pageTitle.isDisplayed();
    }

    public boolean isCalendarGridVisible() {
        try {
            WaitUtils.waitForVisibility(driver, calendarGrid);
            return calendarGrid.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public int getLeaveEntryCount() {
        return leaveEntries.size();
    }

    public int getConflictIndicatorCount() {
        return conflictIndicators.size();
    }

    public int getTeamMemberCount() {
        return teamMemberLabels.size();
    }

    public String getCalendarTitle() {
        try {
            WaitUtils.waitForVisibility(driver, calendarTitle);
            return calendarTitle.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Returns true if a specific employee's name appears in the team calendar rows.
     */
    public boolean isEmployeeVisibleInCalendar(String employeeName) {
        return teamMemberLabels.stream()
                .anyMatch(el -> el.getText().trim().equalsIgnoreCase(employeeName));
    }
}
