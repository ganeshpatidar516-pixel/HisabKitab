package com.ganesh.hisabkitabpro.addon.reminder

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.addon.audit.AuditLogEntry
import com.ganesh.hisabkitabpro.commandos.adapters.contracts.ReminderDispatchReport
import com.ganesh.hisabkitabpro.core.locale.AppLocaleManager
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.domain.payment.UpiIntentBuilder
import com.ganesh.hisabkitabpro.domain.reminder.ReminderLocalization
import com.ganesh.hisabkitabpro.domain.reminder.SmsPaymentReminder
import com.ganesh.hisabkitabpro.domain.reminder.WhatsAppPaymentShowcaseRenderer
import com.ganesh.hisabkitabpro.domain.reminder.WhatsAppQrAttachmentValidator
import com.ganesh.hisabkitabpro.domain.reminder.WhatsAppSender
import com.ganesh.hisabkitabpro.domain.profile.ProfileMapFooter
import com.ganesh.hisabkitabpro.domain.repository.CustomerRepository
import com.ganesh.hisabkitabpro.domain.repository.SettingsRepository
import com.ganesh.hisabkitabpro.domain.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs the same adaptive WhatsApp/SMS reminder flow as the customer ledger screen,
 * triggered by Super Command / AI assistant (any customer in DB, resolved by name).
 */
@Singleton
class AssistantCustomerReminderDispatcher @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val customerRepository: CustomerRepository,
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend fun dispatchForCustomerId(customerId: Long): ReminderDispatchReport {
        val customer = customerRepository.getCustomerById(customerId)
            ?: return ReminderDispatchReport(
                success = false,
                customerName = "unknown",
                reason = "customer_missing"
            )
        val netBalancePaise = transactionRepository.getCalculateBalance(customerId)

        val businessProfile = settingsRepository.getBusinessProfile().firstOrNull()
        val businessTitle = businessProfile?.businessName?.trim()?.ifBlank { null }
            ?: "GOLDEN RAJWADI CHAI"

        val payLink = buildAssistantUpiPayLink(
            upiId = businessProfile?.upiId,
            amountPaise = netBalancePaise,
            customerName = customer.name,
            payeeDisplayName = businessTitle
        )
        val qrPath = businessProfile?.qrImagePath?.takeIf { it.isNotBlank() }
        val qrFile = qrPath?.let(::File)?.takeIf { it.exists() }
        val qrReason = WhatsAppQrAttachmentValidator.validateQrImageFileOrReason(qrFile)
        val qrOk = qrReason == null
        val digits = customer.phone.filter { it.isDigit() }
        if (digits.isBlank()) {
            return ReminderDispatchReport(
                success = false,
                customerName = customer.name,
                reason = "phone_missing"
            )
        }

        val transactions = transactionRepository.getTransactionsByCustomer(customerId).firstOrNull().orEmpty()
        val oldestTxnAt = transactions.minOfOrNull { it.createdAt } ?: System.currentTimeMillis()
        val daysOverdue = ((System.currentTimeMillis() - oldestTxnAt) / (24L * 60L * 60L * 1000L)).toInt()
            .coerceAtLeast(0)
        val previousAttempts = ReminderAutomationPrefs.getReminderAttempts(appContext, customer.id)
        val preferredChannel = ReminderAutomationPrefs.getPreferredChannel(appContext, customer.id)
        val plan = ReminderBehaviorEngine.selectPlan(
            daysOverdue = daysOverdue,
            netDuePaise = netBalancePaise,
            previousAttempts = previousAttempts,
            preferredChannel = preferredChannel
        )

        val hasDue = netBalancePaise > 0L
        val message = if (hasDue) {
            when (plan.tone) {
                AutoReminderTone.POLITE -> buildAssistantMasterReminderMessage(
                    appContext,
                    businessTitle,
                    customer.name,
                    netBalancePaise,
                    payLink,
                    AssistantReminderTone.POLITE
                )
                AutoReminderTone.PROFESSIONAL -> buildAssistantMasterReminderMessage(
                    appContext,
                    businessTitle,
                    customer.name,
                    netBalancePaise,
                    payLink,
                    AssistantReminderTone.PROFESSIONAL
                )
                AutoReminderTone.STRICT -> buildAssistantMasterReminderMessage(
                    appContext,
                    businessTitle,
                    customer.name,
                    netBalancePaise,
                    payLink,
                    AssistantReminderTone.STRICT
                )
                AutoReminderTone.PARTIAL_OFFER -> buildAssistantPartialOfferReminderMessage(
                    appContext,
                    businessTitle,
                    customer.name,
                    netBalancePaise,
                    payLink,
                    upfrontPercent = 25
                )
            }
        } else {
            val lc = AppLocaleManager.wrapContext(appContext)
            lc.getString(
                R.string.reminder_sms_body_no_pay,
                businessTitle,
                customer.name,
                NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(0.0)
            )
        }

        val messageWithProfileFooter = ProfileMapFooter.mapFooter(businessProfile)
            ?.let { "$message\n\n$it" }
            ?: message
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val waImageFile = withContext(Dispatchers.IO) {
            if (!qrOk || qrFile == null) return@withContext null
            WhatsAppPaymentShowcaseRenderer.renderToCacheFileOrNull(
                appContext,
                businessProfile,
                customer.name,
                netBalancePaise,
                qrFile
            )?.takeIf { it.exists() && it.length() > 1024L }
        }
        val waFinalAttachmentFile = waImageFile ?: (if (qrOk) qrFile else null)

        val finalChannel = withContext(Dispatchers.Main) {
            when (plan.channel) {
                AutoReminderChannel.WHATSAPP -> {
                    val waOpened = if (waFinalAttachmentFile != null) {
                        WhatsAppSender.sendReminderWithImage(appContext, digits, messageWithProfileFooter, waFinalAttachmentFile)
                    } else {
                        WhatsAppSender.sendTextReminder(appContext, digits, messageWithProfileFooter)
                    }
                    if (waOpened) {
                        AutoReminderChannel.WHATSAPP
                    } else {
                        val smsOpened = if (hasDue) {
                            SmsPaymentReminder.openLedgerReminder(
                                context = appContext,
                                customerName = customer.name,
                                rawPhone = customer.phone,
                                netDuePaise = netBalancePaise,
                                businessName = businessTitle,
                                upiId = businessProfile?.upiId,
                                currencyFormatter = currencyFormatter
                            )
                        } else {
                            openSimpleSmsDraft(appContext, digits, messageWithProfileFooter)
                        }
                        if (smsOpened) AutoReminderChannel.SMS else null
                    }
                }
                AutoReminderChannel.SMS -> {
                    val smsOpened = if (hasDue) {
                        SmsPaymentReminder.openLedgerReminder(
                            context = appContext,
                            customerName = customer.name,
                            rawPhone = customer.phone,
                            netDuePaise = netBalancePaise,
                            businessName = businessTitle,
                            upiId = businessProfile?.upiId,
                            currencyFormatter = currencyFormatter
                        )
                    } else {
                            openSimpleSmsDraft(appContext, digits, messageWithProfileFooter)
                    }
                    if (smsOpened) {
                        AutoReminderChannel.SMS
                    } else {
                        val waOpened = if (waFinalAttachmentFile != null) {
                            WhatsAppSender.sendReminderWithImage(appContext, digits, messageWithProfileFooter, waFinalAttachmentFile)
                        } else {
                            WhatsAppSender.sendTextReminder(appContext, digits, messageWithProfileFooter)
                        }
                        if (waOpened) AutoReminderChannel.WHATSAPP else null
                    }
                }
            }
        }

        if (finalChannel == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    appContext,
                    ReminderLocalization.channelUnavailableText(appContext),
                    Toast.LENGTH_SHORT
                ).show()
            }
            return ReminderDispatchReport(
                success = false,
                customerName = customer.name,
                reason = "channel_unavailable"
            )
        }

        ReminderAutomationPrefs.markManualReminderSent(appContext, customer.id, transactionId = 0L)
        ReminderAutomationPrefs.setManualPauseForCustomer(appContext, customer.id, days = 7)
        ReminderAutomationPrefs.incrementReminderAttempts(appContext, customer.id)
        ReminderAutomationPrefs.setPreferredChannel(appContext, customer.id, finalChannel)

        withContext(Dispatchers.Main) {
            Toast.makeText(
                appContext,
                if (finalChannel == AutoReminderChannel.WHATSAPP) {
                    ReminderLocalization.whatsappOpenedText(appContext)
                } else {
                    ReminderLocalization.smsOpenedText(appContext)
                },
                Toast.LENGTH_SHORT
            ).show()
        }

        customerRepository.markReminderSent(customerId)

        withContext(Dispatchers.IO) {
            try {
                AppDatabase.getDatabase(appContext).auditLogDao().insert(
                    AuditLogEntry(
                        entityType = "REMINDER",
                        entityId = customer.id,
                        action = "ASSISTANT_${plan.tone}_${finalChannel.name}",
                        detail = "customerId=${customer.id},daysOverdue=$daysOverdue," +
                            "attempts=$previousAttempts,netDue=$netBalancePaise," +
                            "payLink=${payLink ?: "NA"},source=super_command"
                    )
                )
            } catch (_: Exception) {
            }
        }

        return ReminderDispatchReport(
            success = true,
            customerName = customer.name,
            channel = finalChannel.name,
            reason = if (qrOk) null else "qr_invalid:$qrReason"
        )
    }
}

private enum class AssistantReminderTone {
    POLITE,
    PROFESSIONAL,
    STRICT
}

private fun buildAssistantUpiPayLink(
    upiId: String?,
    amountPaise: Long,
    customerName: String,
    payeeDisplayName: String
): String? {
    if (upiId.isNullOrBlank()) return null
    val amountText = String.format(Locale.US, "%.2f", amountPaise / 100.0)
    val note = "Payment from $customerName - HisabKitab".take(80)
    val txnRef = "HKR_${System.currentTimeMillis()}"
    return UpiIntentBuilder.buildPayUri(
        payeeVpa = upiId,
        payeeName = payeeDisplayName.ifBlank { "Business" },
        amountRupee = amountText,
        transactionNote = note,
        txnRef = txnRef
    ).toString()
}

private fun buildAssistantMasterReminderMessage(
    context: Context,
    businessName: String,
    customerName: String,
    netBalancePaise: Long,
    payLink: String?,
    tone: AssistantReminderTone
): String {
    val lc = AppLocaleManager.wrapContext(context)
    val amount = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(netBalancePaise / 100.0)
    val bodyRes = when (tone) {
        AssistantReminderTone.POLITE -> R.string.reminder_master_polite
        AssistantReminderTone.PROFESSIONAL -> R.string.reminder_master_professional
        AssistantReminderTone.STRICT -> R.string.reminder_master_strict
    }
    val body = lc.getString(bodyRes, customerName, amount)
    val linkLine = payLink?.let { "\n" + lc.getString(R.string.reminder_pay_line, it) } ?: ""
    return buildString {
        append("*").append(businessName).append("*\n")
        append(lc.getString(R.string.reminder_branding_powered)).append("\n\n")
        append(body).append("\n")
        append(lc.getString(R.string.reminder_qr_footer))
        append(linkLine)
    }
}

private fun buildAssistantPartialOfferReminderMessage(
    context: Context,
    businessName: String,
    customerName: String,
    netBalancePaise: Long,
    payLink: String?,
    upfrontPercent: Int
): String {
    val lc = AppLocaleManager.wrapContext(context)
    val nf = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    val totalAmount = nf.format(netBalancePaise / 100.0)
    val upfrontPaise = (netBalancePaise * upfrontPercent / 100.0).toLong()
    val upfrontAmount = nf.format(upfrontPaise / 100.0)
    val linkLine = payLink?.let { "\n" + lc.getString(R.string.reminder_pay_line, it) } ?: ""
    val body = lc.getString(R.string.reminder_partial_body, customerName, totalAmount, upfrontAmount)
    return buildString {
        append("*").append(businessName).append("*\n")
        append(lc.getString(R.string.reminder_branding_powered)).append("\n\n")
        append(body)
        append(linkLine)
    }
}

private fun openSimpleSmsDraft(context: Context, digits: String, body: String): Boolean {
    return try {
        context.startActivity(
            Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$digits")
                putExtra("sms_body", body)
            }
        )
        true
    } catch (_: Exception) {
        false
    }
}
