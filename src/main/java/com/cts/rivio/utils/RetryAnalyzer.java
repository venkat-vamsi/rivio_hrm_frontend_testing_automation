package com.cts.rivio.utils;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * RetryAnalyzer – retries a failed test up to MAX_RETRY times.
 *
 * Usage: Add to any @Test annotation:
 *   @Test(retryAnalyzer = RetryAnalyzer.class)
 *
 * How it works:
 *   TestNG calls retry() after each failure.
 *   If retry() returns true, TestNG re-runs the test.
 *   If it returns false (retries exhausted), the test is marked FAIL.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private int retryCount = 0;
    private static final int MAX_RETRY = 2; // retry up to 2 extra times

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < MAX_RETRY) {
            retryCount++;
            System.out.println("[Retry] Retrying test: "
                    + result.getName() + " (attempt " + retryCount + "/" + MAX_RETRY + ")");
            return true;
        }
        return false;
    }
}
