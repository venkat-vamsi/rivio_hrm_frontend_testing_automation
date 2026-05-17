package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.utils.ExtentManager;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * PerformanceManagementTest – not implemented in Rivio_Angular.
 */
public class PerformanceManagementTest extends BaseTest {

    @Test(description = "Performance Management is not implemented in the current build")
    public void performanceManagement_notImplemented() {
        ExtentManager.getTest().skip("Performance Management is not present in Rivio_Angular. "
                + "Marked as scope-pending.");
        throw new SkipException("Performance Management not implemented");
    }
}
