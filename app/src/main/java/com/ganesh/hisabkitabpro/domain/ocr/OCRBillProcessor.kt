package com.ganesh.hisabkitabpro.domain.ocr

import android.graphics.Bitmap
import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import com.ganesh.hisabkitabpro.domain.repository.CustomerRepository
import com.ganesh.hisabkitabpro.domain.repository.TransactionRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ManualBillScan(
    val amountRupees: Double,
    val vendorHint: String,
    val lineItemsSummary: String = "",
    /** Wave 7 — UI may warn when [BillAmountConfidence.LOW]. */
    val amountConfidence: BillAmountConfidence = BillAmountConfidence.HIGH,
)

/** Result of live-frame OCR when [customerId] is already known (ledger scan) — avoids wrong party assignment. */
sealed class AutoBillOcrResult {
    data object NoAmount : AutoBillOcrResult()
    data object CustomerMissing : AutoBillOcrResult()
    data class Saved(val userMessage: String) : AutoBillOcrResult()
    /** P1 — amount read but not auto-posted; user must capture again or enter manually. */
    data class LowConfidence(val userMessage: String) : AutoBillOcrResult()
    data object Failed : AutoBillOcrResult()
}

@Singleton
class OCRBillProcessor @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val customerRepository: CustomerRepository
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Wave 2 — ML Kit client is not safe for concurrent [com.google.mlkit.vision.text.TextRecognizer.process] calls. */
    private val textRecognitionMutex = Mutex()

    private suspend fun recognizeText(image: InputImage): Text =
        textRecognitionMutex.withLock { recognizer.process(image).await() }

    /**
     * Reads amount + vendor from a bill photo without saving — for pre-filling the keypad screen.
     */
    /**
     * Live camera frames: uses **fixed** [customerId] (from customer ledger) + amount from bill text only.
     * Does not assign party from OCR name — safer than [processAndSaveBill] for auto-capture.
     */
    suspend fun processInputImageForKnownCustomer(image: InputImage, customerId: Long): AutoBillOcrResult {
        if (customerId <= 0L) return AutoBillOcrResult.CustomerMissing
        val customer = customerRepository.getCustomerById(customerId) ?: return AutoBillOcrResult.CustomerMissing
        return try {
            OcrTelemetry.event("live_frame_begin", mapOf("customerScoped" to "1"))
            val visionText = recognizeText(image)
            val len = visionText.text.length
            val amountDetail = OCRExtractor.extractAmountDetail(visionText.text)
            val amount = amountDetail.rupees
            if (amount <= 0.0) {
                OcrTelemetry.event(
                    "live_frame_end",
                    mapOf(
                        "outcome" to "no_amount",
                        "textLen" to len.toString(),
                        "amtConf" to amountDetail.confidence.name,
                        "amtSrc" to amountDetail.source.name,
                    ),
                )
                return AutoBillOcrResult.NoAmount
            }
            if (amountDetail.confidence == BillAmountConfidence.LOW) {
                OcrTelemetry.event(
                    "live_frame_end",
                    mapOf(
                        "outcome" to "low_confidence",
                        "textLen" to len.toString(),
                        "amtConf" to amountDetail.confidence.name,
                        "amtSrc" to amountDetail.source.name,
                    ),
                )
                return AutoBillOcrResult.LowConfidence(
                    "Low-confidence amount (₹$amount). Tap the camera button to confirm, or enter manually."
                )
            }
            val transaction = Transaction(
                customerId = customerId,
                amount = (amount * 100).toLong(),
                type = TransactionType.CREDIT,
                note = "OCR Auto · ${customer.name}",
                txnRef = UUID.randomUUID().toString(),
                createdAt = System.currentTimeMillis()
            )
            val saveResult = transactionRepository.addTransaction(transaction)
            if (saveResult.isFailure) {
                OcrTelemetry.event("live_frame_end", mapOf("outcome" to "save_failed"))
                return AutoBillOcrResult.Failed
            }
            OcrTelemetry.event(
                "live_frame_end",
                mapOf(
                    "outcome" to "saved",
                    "textLen" to len.toString(),
                    "amtConf" to amountDetail.confidence.name,
                    "amtSrc" to amountDetail.source.name,
                ),
            )
            val liveMsg = buildString {
                append("Saved ₹$amount for ${customer.name}.")
                if (amountDetail.confidence == BillAmountConfidence.LOW) {
                    append(" ")
                    append(LOW_CONFIDENCE_LIVE_TOAST_SUFFIX)
                }
            }
            AutoBillOcrResult.Saved(liveMsg)
        } catch (_: Exception) {
            OcrTelemetry.event("live_frame_end", mapOf("outcome" to "failed"))
            AutoBillOcrResult.Failed
        }
    }

    suspend fun extractForManualEntry(bitmap: Bitmap): ManualBillScan? {
        val working = OcrImageLoader.scaleDownCopyIfNeeded(bitmap)
        return try {
            OcrTelemetry.event("prefill_extract_begin")
            val image = InputImage.fromBitmap(working, 0)
            val visionText = recognizeText(image)
            val text = visionText.text
            val amountDetail = OCRExtractor.extractAmountDetail(text)
            val amount = amountDetail.rupees
            if (amount <= 0.0) {
                OcrTelemetry.event(
                    "prefill_extract_end",
                    mapOf(
                        "outcome" to "no_amount",
                        "textLen" to text.length.toString(),
                        "amtConf" to amountDetail.confidence.name,
                        "amtSrc" to amountDetail.source.name,
                    ),
                )
                return null
            }
            val vendor = OCRExtractor.extractName(text).trim()
            val lines = OCRExtractor.extractLineItemsSummary(text)
            OcrTelemetry.event(
                "prefill_extract_end",
                mapOf(
                    "outcome" to "ok",
                    "textLen" to text.length.toString(),
                    "amtConf" to amountDetail.confidence.name,
                    "amtSrc" to amountDetail.source.name,
                ),
            )
            ManualBillScan(
                amountRupees = amount,
                vendorHint = vendor,
                lineItemsSummary = lines,
                amountConfidence = amountDetail.confidence,
            )
        } catch (_: Exception) {
            OcrTelemetry.event("prefill_extract_end", mapOf("outcome" to "error"))
            null
        } finally {
            if (working !== bitmap) {
                working.recycle()
            }
        }
    }

    suspend fun processAndSaveBill(bitmap: Bitmap, customerId: Long): String {
        if (!canCaptureSaveToLedger(customerId)) {
            OcrTelemetry.event("capture_save_end", mapOf("outcome" to "customer_required"))
            return "Open a customer ledger first, then scan the bill to save an entry."
        }
        val customer = customerRepository.getCustomerById(customerId)
            ?: run {
                OcrTelemetry.event("capture_save_end", mapOf("outcome" to "customer_missing"))
                return "Customer not found. Open the ledger again and retry."
            }
        val working = OcrImageLoader.scaleDownCopyIfNeeded(bitmap)
        return try {
            OcrTelemetry.event("capture_save_begin", mapOf("customerIdPresent" to "true"))
            val image = InputImage.fromBitmap(working, 0)
            val visionText = recognizeText(image)
            val extractedText = visionText.text
            val textLen = extractedText.length

            val amountDetail = OCRExtractor.extractAmountDetail(extractedText)
            val amount = amountDetail.rupees

            if (amount > 0 && amountDetail.confidence == BillAmountConfidence.LOW) {
                OcrTelemetry.event(
                    "capture_save_end",
                    mapOf(
                        "outcome" to "low_confidence",
                        "textLen" to textLen.toString(),
                        "amtConf" to amountDetail.confidence.name,
                        "amtSrc" to amountDetail.source.name,
                    ),
                )
                return buildString {
                    append("Low-confidence amount (₹$amount). Please verify before saving.")
                    append(" ")
                    append(LOW_CONFIDENCE_CAPTURE_SUFFIX)
                }
            }

            if (amount > 0) {
                val transaction = Transaction(
                    customerId = customerId,
                    amount = (amount * 100).toLong(), // Convert to Paise
                    type = TransactionType.CREDIT,
                    note = "OCR Scan: ${customer.name}",
                    txnRef = UUID.randomUUID().toString(),
                    createdAt = System.currentTimeMillis()
                )
                val saveResult = transactionRepository.addTransaction(transaction)
                if (saveResult.isFailure) {
                    OcrTelemetry.event(
                        "capture_save_end",
                        mapOf("outcome" to "save_failed"),
                    )
                    return "Could not save to ledger. Please try again."
                }
                OcrTelemetry.event(
                    "capture_save_end",
                    mapOf(
                        "outcome" to "saved",
                        "textLen" to textLen.toString(),
                        "amtConf" to amountDetail.confidence.name,
                        "amtSrc" to amountDetail.source.name,
                    ),
                )
                buildString {
                    append("Success! Added ₹$amount from the bill for ${customer.name}.")
                    if (amountDetail.confidence == BillAmountConfidence.LOW) {
                        append(" ")
                        append(LOW_CONFIDENCE_CAPTURE_SUFFIX)
                    }
                }
            } else {
                OcrTelemetry.event(
                    "capture_save_end",
                    mapOf(
                        "outcome" to "no_amount",
                        "textLen" to textLen.toString(),
                        "amtConf" to amountDetail.confidence.name,
                        "amtSrc" to amountDetail.source.name,
                    ),
                )
                "No amount was found on the bill."
            }
        } catch (_: Exception) {
            OcrTelemetry.event("capture_save_end", mapOf("outcome" to "exception"))
            "OCR processing failed. Please try again."
        } finally {
            if (working !== bitmap) {
                working.recycle()
            }
        }
    }

    companion object {
        /** P0 — [processAndSaveBill] must not post to the ledger without a known customer id. */
        fun canCaptureSaveToLedger(customerId: Long): Boolean = customerId > 0L

        /** Wave 7 — user-facing English suffix appended after capture save when confidence is LOW. */
        private const val LOW_CONFIDENCE_CAPTURE_SUFFIX = "Please double-check the amount."
        private const val LOW_CONFIDENCE_LIVE_TOAST_SUFFIX = "Verify the amount in the ledger if unsure."

        /** Matches [updateAmount] keypad rules: whole rupees without ".0", else up to 2 decimals. */
        fun formatAmountForKeypad(amount: Double): String {
            val rounded = kotlin.math.round(amount * 100.0) / 100.0
            val asLong = rounded.toLong()
            if (kotlin.math.abs(rounded - asLong.toDouble()) < 1e-9) return asLong.toString()
            return String.format(Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
        }
    }
}
