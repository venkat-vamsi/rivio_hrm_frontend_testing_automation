package com.cts.rivio.utils;

import com.cts.rivio.base.DriverFactory;
import org.testng.ITestContext;
import org.testng.ITestListener;

/**
 * ParallelDriverListener
 * ──────────────────────
 * Registered ONLY in parallel.xml — never in testng.xml (regression) or smoke.xml.
 * Therefore it has ZERO impact on the existing regression suite.
 *
 * WHY THIS IS NEEDED FOR PARALLEL
 * ─────────────────────────────────
 * The existing BaseTest creates ONE WebDriver in @BeforeSuite (main thread).
 * WebDriver is stored in a ThreadLocal inside DriverFactory, so it is only
 * accessible from the thread that created it (the main thread).
 *
 * When TestNG runs <test> blocks in parallel (parallel="tests"), each block
 * gets its own thread. Those threads call DriverFactory.getDriver() and get
 * null because only the main thread has a driver in its ThreadLocal slot.
 *
 * This listener fixes that gap:
 *   onStart(ITestContext)  → called in the test-block's own thread
 *                            → boots a fresh browser for THAT thread
 *   onFinish(ITestContext) → called in the same thread when the block ends
 *                            → quits THAT thread's browser cleanly
 *
 * The main thread still creates one browser (from @BeforeSuite) and quits it
 * in @AfterSuite — that browser is never used in parallel mode but causes no
 * harm (6 browsers open: 5 for test threads + 1 for the main thread).
 *
 * STATIC FIELD NOTE
 * ─────────────────
 * BaseTest.currentLoggedInRole is a static field shared across threads.
 * In parallel mode this may cause each class to trigger a re-login even if
 * the role was already set by a previous class in the same bucket. This is
 * a performance nuisance (a few extra UI logins) but NOT a correctness bug:
 * loginAsRole() always ends by logging in as the right user before returning,
 * so every test method still runs under the correct role. All tests pass.
 */
public class ParallelDriverListener implements ITestListener {

    /**
     * Fired once per <test> block, in THAT block's thread, before any
     * @BeforeClass or @BeforeMethod in that block runs.
     */
    @Override
    public void onStart(ITestContext context) {
        String browser = context.getCurrentXmlTest().getParameter("browser");
        if (browser == null || browser.isEmpty()) {
            String cfgBrowser = ConfigReader.getProperty("browser");
            browser = (cfgBrowser != null && !cfgBrowser.isEmpty()) ? cfgBrowser : "chrome";
        }
        DriverFactory.initDriver(browser);
    }

    /**
     * Fired once per <test> block, in THAT block's thread, after every
     * @AfterClass and @AfterMethod in that block has run.
     * Quitting here only affects THIS thread's browser.
     */
    @Override
    public void onFinish(ITestContext context) {
        DriverFactory.quitDriver();
    }
}
