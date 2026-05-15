package com.ganesh.hisabkitabpro.domain.ocr

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** P0 — capture-save must never run without an explicit customer id. */
class OCRBillProcessorSaveGuardTest {

    @Test
    fun captureSave_allowedOnlyWithPositiveCustomerId() {
        assertFalse(OCRBillProcessor.canCaptureSaveToLedger(0L))
        assertFalse(OCRBillProcessor.canCaptureSaveToLedger(-1L))
        assertTrue(OCRBillProcessor.canCaptureSaveToLedger(1L))
    }
}
