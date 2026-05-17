package com.cts.rivio.tests;

import com.cts.rivio.base.BaseTest;
import com.cts.rivio.utils.ExtentManager;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * DocumentManagementTest – the Rivio Angular app does not expose a Document
 * Management module (see app.routes.ts). These tests are intentionally skipped
 * until the feature exists. Listed in the regression suite so the report
 * preserves the original TC traceability.
 */
public class DocumentManagementTest extends BaseTest {

    @Test(description = "Document Management is not implemented in the current build")
    public void documentManagement_notImplemented() {
        ExtentManager.getTest().skip("Document Management module is not present in Rivio_Angular. "
                + "Marked as scope-pending — open a feature request before re-enabling.");
        throw new SkipException("Document Management module not implemented in Rivio_Angular");
    }
}
