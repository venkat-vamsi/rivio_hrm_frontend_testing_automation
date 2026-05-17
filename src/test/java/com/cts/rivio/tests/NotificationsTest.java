package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.utils.ExtentManager;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * NotificationsTest – the Rivio Angular header has no notification bell.
 * Skipped until the feature is added to the frontend.
 */
public class NotificationsTest extends BaseTest {

    @Test(description = "Notifications panel is not implemented in the current build")
    public void notifications_notImplemented() {
        ExtentManager.getTest().skip("Notifications/alerts UI not present in Rivio_Angular header.");
        throw new SkipException("Notifications not implemented");
    }
}
