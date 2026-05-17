package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.utils.ExtentManager;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * TeamCalendarTest – Team Calendar feature not implemented in Rivio_Angular.
 */
public class TeamCalendarTest extends BaseTest {

    @Test(description = "Team Calendar is not implemented in the current build")
    public void teamCalendar_notImplemented() {
        ExtentManager.getTest().skip("Team Calendar UI not present in Rivio_Angular.");
        throw new SkipException("Team Calendar not implemented");
    }
}
