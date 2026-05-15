package com.ganesh.hisabkitabpro.addon.reminder

enum class AutoReminderTone {
    POLITE,
    PROFESSIONAL,
    STRICT,
    PARTIAL_OFFER
}

enum class AutoReminderChannel {
    WHATSAPP,
    SMS
}

data class AutoReminderPlan(
    val tone: AutoReminderTone,
    val channel: AutoReminderChannel
)

object ReminderBehaviorEngine {
    fun selectPlan(
        daysOverdue: Int,
        netDuePaise: Long,
        previousAttempts: Int,
        preferredChannel: AutoReminderChannel?
    ): AutoReminderPlan {
        val amountRisk = when {
            netDuePaise >= 100_000L -> 2
            netDuePaise >= 25_000L -> 1
            else -> 0
        }
        val overdueRisk = when {
            daysOverdue >= 14 -> 3
            daysOverdue >= 7 -> 2
            daysOverdue >= 3 -> 1
            else -> 0
        }
        val attemptRisk = when {
            previousAttempts >= 6 -> 3
            previousAttempts >= 3 -> 2
            previousAttempts >= 1 -> 1
            else -> 0
        }
        val riskScore = amountRisk + overdueRisk + attemptRisk

        val tone = when {
            riskScore <= 1 -> AutoReminderTone.POLITE
            riskScore <= 3 -> AutoReminderTone.PROFESSIONAL
            riskScore <= 5 -> AutoReminderTone.STRICT
            else -> AutoReminderTone.PARTIAL_OFFER
        }

        val channel = preferredChannel ?: if (previousAttempts % 3 == 2) {
            AutoReminderChannel.SMS
        } else {
            AutoReminderChannel.WHATSAPP
        }
        return AutoReminderPlan(tone = tone, channel = channel)
    }
}
