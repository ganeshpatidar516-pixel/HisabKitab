package com.ganesh.hisabkitabpro.domain.ai.automation

import com.ganesh.hisabkitabpro.domain.ai.AIAction
import com.ganesh.hisabkitabpro.domain.ai.AIResponse
import com.ganesh.hisabkitabpro.domain.ai.ResponseType
import com.ganesh.hisabkitabpro.domain.model.Customer

/**
 * LEGACY / SCAFFOLDING — DO NOT USE FOR NEW CODE.
 *
 * Has zero callers in `app/src/main`. Confirmed stripped from release AABs by R8
 * (see `app/build/outputs/mapping/release/usage.txt:48251`).
 *
 * Originally drafted as the proactive-suggestion engine for the AI assistant
 * (auto-detect overdue customers, suggest monthly reports, etc.). The current
 * AI flow (`ui/ai/AIChatScreen` + the `domain/ai/` package) handles these suggestions
 * inline in the chat ViewModel and does not invoke this object.
 *
 * Retained as a design reference for a future "background suggestion" feature.
 * If revived, it must respect the user-confirmation guarantee — never auto-send,
 * only surface a suggestion in the AI chat UI.
 */
@Deprecated(
    message = "Reference scaffolding for proactive AI suggestions. Not wired into the live AI flow.",
    level = DeprecationLevel.WARNING
)
object AIAutomationEngine {

    /**
     * ग्राहकों के बैलेंस चेक करता है और अगर उधार 1000 से ज्यादा है तो ऑटो-रिमाइंडर सुझाव देता है।
     */
    fun scanForPendingReminders(customers: List<Customer>): List<AIResponse> {
        return customers.filter { it.balanceCache > 100000 }.map { customer -> // 100000 Paise = 1000 Rupees
            AIResponse(
                message = "ऑटो-अलर्ट: ${customer.name} का उधार ₹${customer.balanceCache / 100.0} हो गया है। क्या मैं रिमाइंडर भेज दूँ?",
                type = ResponseType.TEXT,
                actions = listOf(
                    AIAction("WhatsApp भेजें", "send_reminder/${customer.id}"),
                    AIAction("अभी नहीं", "ignore")
                )
            )
        }
    }

    /**
     * महीने के अंत में ऑटो-रिपोर्ट जनरेट करने का सुझाव।
     */
    fun suggestMonthlyReport(): AIResponse {
        return AIResponse(
            message = "महीना खत्म होने वाला है। क्या आप अपनी 'Monthly Profit & Loss' रिपोर्ट देखना चाहेंगे?",
            type = ResponseType.TEXT,
            actions = listOf(
                AIAction("रिपोर्ट देखें", "analytics_dashboard")
            )
        )
    }
}
