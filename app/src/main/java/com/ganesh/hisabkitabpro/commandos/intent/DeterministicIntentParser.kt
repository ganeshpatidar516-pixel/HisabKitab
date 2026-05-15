package com.ganesh.hisabkitabpro.commandos.intent

import com.ganesh.hisabkitabpro.commandos.model.CommandIntent
import com.ganesh.hisabkitabpro.commandos.model.ExecutionPolicy
import com.ganesh.hisabkitabpro.commandos.model.IntentName
import com.ganesh.hisabkitabpro.commandos.model.ParsedCommand
import com.ganesh.hisabkitabpro.commandos.model.ResolvedEntities
import com.ganesh.hisabkitabpro.commandos.model.RiskLevel
import com.ganesh.hisabkitabpro.commandos.model.TransactionClass
import java.util.Locale

class DeterministicIntentParser {
    private val ledgerPattern = Regex("^(.+) (ko|ke account me|ke khate mein|ke khate me) (\\d+) (add|jama|credit|plus|jodo)( karo)?$")
    private val billClearPattern = Regex("^(.+) (ka|ki) (bill|hisab|hisaab) (clear|saaf|settle|nipta)( karo)?$")
    private val reminderPattern = Regex("^(.+) ko (reminder|yaad) (bhejo|send|lagao)( karo)?$")
    private val reminderPatternAlt = Regex("^(.+) ko (send reminder|reminder bhejo|yaad bhejo)( karo)?$")
    private val customerAddPattern = Regex("^(.+) (customer|naya customer) add phone (\\d{10,15})( karo)?$")
    private val customerAddNameFirstPattern = Regex("^customer (.+) add phone (\\d{10,15})( karo)?$")
    private val customerAddWithoutPhonePattern = Regex("^(.+) (customer|naya customer) add( karo)?$")
    private val openScreenPattern = Regex("^(open|khol|jao|go) (.+)$")
    private val settingPattern = Regex("^setting (.+) (.+) set( karo)?$")
    // After [InputNormalizer], "balance" often becomes "bill"; exclude "ka bill clear" (bill settlement).
    private val balanceAskKaPattern =
        Regex("^(.+?)\\s+ka\\s+(?:balance|bill)(?!\\s+clear)\\b.*$")
    private val balanceAskKiPattern =
        Regex("^(.+?)\\s+ki\\s+(?:balance|bill)(?!\\s+clear)\\b.*$")
    private val numberPattern = Regex("\\b\\d+\\b")
    private val phonePattern = Regex("\\b\\d{10,15}\\b")
    private val stopWords = setOf(
        "ko", "ka", "ki", "ke", "me", "mein", "account", "bill", "hisab", "hisaab",
        "customer", "naya", "open", "go", "jao", "khol", "karo", "karo", "phone",
        "aaye", "hain", "hai", "khate", "khaate", "khata"
    )

    private val ledgerAggregateNoiseNameTokens = setOf(
        "sab", "saare", "pure", "total", "kitna", "kitne", "mera", "hamare", "business",
        "overall", "pura", "poora", "sabka", "sabki", "poori"
    )

    private val amountAnchorTokens = setOf("add", "jama", "jodo", "credit", "plus", "jod")

    private fun cleanCustomerName(raw: String): String {
        return raw
            .trim()
            .replace(Regex("\\b(naam ka|name)\\b"), " ")
            .replace(Regex("\\b(naya|new)\\b"), " ")
            .replace(Regex("\\b(bro|bhai|bhaiya|ji)\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun resolveRoute(screenHint: String): String? {
        val hint = screenHint.trim()
        return when {
            hint.contains("home") || hint.contains("dashboard") -> "dashboard"
            hint.contains("customer") || hint.contains("grahak") -> "customers"
            hint.contains("supplier") || hint.contains("party") -> "suppliers"
            hint.contains("inventory") || hint.contains("product") || hint.contains("stock") -> "inventory"
            hint.contains("assistant") || hint.contains("ai assistant") -> "ai_assistant"
            hint.contains("pilot") || hint.contains("ai pilot") -> "business_insights"
            hint.contains("insight") || hint.contains("analysis") || hint.contains("report") -> "business_insights"
            hint.contains("setting") -> "settings"
            else -> null
        }
    }

    private fun splitTokens(input: String): List<String> = input.split(" ").filter { it.isNotBlank() }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }

    private fun hasApproxWord(input: String, variants: Set<String>): Boolean {
        return splitTokens(input).any { token ->
            variants.any { variant ->
                token == variant || (token.length >= 2 && levenshtein(token, variant) <= 1)
            }
        }
    }

    private fun extractAmount(input: String): Long? {
        val toks = splitTokens(input)
        val anchorIndex = toks.indexOfFirst { it in amountAnchorTokens }
        val slice = if (anchorIndex >= 0) {
            toks.subList(0, anchorIndex).joinToString(" ")
        } else {
            input
        }
        val amounts = Regex("\\d+").findAll(slice).map { it.value }.toList()
        val lastBeforeVerb = amounts.lastOrNull()?.toLongOrNull()
        return lastBeforeVerb ?: numberPattern.find(input)?.value?.toLongOrNull()
    }

    private fun extractPhone(input: String): String? {
        return phonePattern.find(input)?.value
    }

    private fun isLedgerAggregateQuestion(normalizedInput: String): Boolean {
        if (hasApproxWord(normalizedInput, setOf("add", "jama", "jodo", "credit", "plus", "clear", "nipta", "settle"))) {
            return false
        }
        val low = normalizedInput.lowercase(Locale.getDefault())
        if (low.contains("balance") || low.contains("hisab") || low.contains("hisaab")) return true
        if (low.contains("bill") && !low.contains("clear") &&
            (low.contains("kitna") || low.contains("kya") || low.contains("total") || low.contains("sab"))
        ) {
            return true
        }
        if (low.contains("udhaar") || low.contains("udhar") || low.contains("bakaya") || low.contains("baki")) return true
        if ((low.contains("kitne") || low.contains("kitna")) &&
            (low.contains("customer") || low.contains("grahak") || low.contains("graahak"))
        ) {
            return true
        }
        if (low.contains("total") &&
            (low.contains("customer") || low.contains("udhaar") || low.contains("udhar") || low.contains("balance"))
        ) {
            return true
        }
        if ((low.contains("sab") || low.contains("saare") || low.contains("pure") || low.contains("pura")) &&
            (low.contains("balance") || low.contains("udhaar") || low.contains("udhar") || low.contains("hisab"))
        ) {
            return true
        }
        if (low.contains("debtor") || low.contains("vasuli")) return true
        if (low.contains("sabse") && (low.contains("zyada") || low.contains("jyada")) &&
            (low.contains("udhaar") || low.contains("udhar") || low.contains("balance"))
        ) {
            return true
        }
        return false
    }

    private fun isAggregateNoiseName(name: String): Boolean {
        return name.lowercase(Locale.getDefault()).split(" ").any { it in ledgerAggregateNoiseNameTokens }
    }

    private fun extractCustomerCandidate(input: String): String? {
        val match = Regex("^(.+?)\\s+(ko|ka|ki|ke)\\b").find(input)
        if (match != null) {
            val candidate = cleanCustomerName(match.groupValues[1])
            if (candidate.isNotBlank()) return candidate
        }
        val filtered = splitTokens(input)
            .filterNot { it in stopWords }
            .take(3)
            .joinToString(" ")
            .trim()
        return filtered.ifBlank { null }
    }

    fun parse(rawInput: String, normalizedInput: String, locale: String): ParsedCommand {
        ledgerPattern.matchEntire(normalizedInput)?.let { match ->
            return ParsedCommand(
                rawInput = rawInput,
                normalizedInput = normalizedInput,
                locale = locale,
                intent = CommandIntent(IntentName.LEDGER_ADD, 0.92, "ledger_add_v1"),
                entities = ResolvedEntities(
                    customerName = cleanCustomerName(match.groupValues[1]),
                    amount = match.groupValues[3].toLongOrNull()
                ),
                policy = ExecutionPolicy(
                    transactionClass = TransactionClass.HARD_ATOMIC,
                    riskLevel = RiskLevel.MEDIUM,
                    requiresConfirmation = false
                )
            )
        }

        billClearPattern.matchEntire(normalizedInput)?.let { match ->
            return ParsedCommand(
                rawInput = rawInput,
                normalizedInput = normalizedInput,
                locale = locale,
                intent = CommandIntent(IntentName.BILL_CLEAR, 0.90, "bill_clear_v1"),
                entities = ResolvedEntities(customerName = cleanCustomerName(match.groupValues[1])),
                policy = ExecutionPolicy(
                    transactionClass = TransactionClass.HARD_ATOMIC,
                    riskLevel = RiskLevel.HIGH,
                    requiresConfirmation = true
                )
            )
        }

        reminderPattern.matchEntire(normalizedInput)?.let { match ->
            return ParsedCommand(
                rawInput = rawInput,
                normalizedInput = normalizedInput,
                locale = locale,
                intent = CommandIntent(IntentName.REMINDER_SEND, 0.88, "reminder_send_v1"),
                entities = ResolvedEntities(customerName = cleanCustomerName(match.groupValues[1])),
                policy = ExecutionPolicy(
                    transactionClass = TransactionClass.FINANCIAL_ATOMIC_NOTIFICATION_EVENTUAL,
                    riskLevel = RiskLevel.LOW,
                    requiresConfirmation = false
                )
            )
        }

        reminderPatternAlt.matchEntire(normalizedInput)?.let { match ->
            return ParsedCommand(
                rawInput = rawInput,
                normalizedInput = normalizedInput,
                locale = locale,
                intent = CommandIntent(IntentName.REMINDER_SEND, 0.88, "reminder_send_v1"),
                entities = ResolvedEntities(customerName = cleanCustomerName(match.groupValues[1])),
                policy = ExecutionPolicy(
                    transactionClass = TransactionClass.FINANCIAL_ATOMIC_NOTIFICATION_EVENTUAL,
                    riskLevel = RiskLevel.LOW,
                    requiresConfirmation = false
                )
            )
        }

        customerAddPattern.matchEntire(normalizedInput)?.let { match ->
            return ParsedCommand(
                rawInput = rawInput,
                normalizedInput = normalizedInput,
                locale = locale,
                intent = CommandIntent(IntentName.CUSTOMER_ADD, 0.90, "customer_add_v1"),
                entities = ResolvedEntities(
                    customerName = cleanCustomerName(match.groupValues[1]),
                    customerPhone = match.groupValues[3].trim()
                ),
                policy = ExecutionPolicy(
                    transactionClass = TransactionClass.HARD_ATOMIC,
                    riskLevel = RiskLevel.LOW,
                    requiresConfirmation = false
                )
            )
        }

        customerAddNameFirstPattern.matchEntire(normalizedInput)?.let { match ->
            return ParsedCommand(
                rawInput = rawInput,
                normalizedInput = normalizedInput,
                locale = locale,
                intent = CommandIntent(IntentName.CUSTOMER_ADD, 0.89, "customer_add_v1"),
                entities = ResolvedEntities(
                    customerName = cleanCustomerName(match.groupValues[1]),
                    customerPhone = match.groupValues[2].trim()
                ),
                policy = ExecutionPolicy(
                    transactionClass = TransactionClass.HARD_ATOMIC,
                    riskLevel = RiskLevel.LOW,
                    requiresConfirmation = false
                )
            )
        }

        customerAddWithoutPhonePattern.matchEntire(normalizedInput)?.let { match ->
            return ParsedCommand(
                rawInput = rawInput,
                normalizedInput = normalizedInput,
                locale = locale,
                intent = CommandIntent(IntentName.CUSTOMER_ADD, 0.86, "customer_add_v1"),
                entities = ResolvedEntities(
                    customerName = cleanCustomerName(match.groupValues[1])
                ),
                policy = ExecutionPolicy(
                    transactionClass = TransactionClass.HARD_ATOMIC,
                    riskLevel = RiskLevel.LOW,
                    requiresConfirmation = false
                )
            )
        }

        openScreenPattern.matchEntire(normalizedInput)?.let { match ->
            val route = resolveRoute(match.groupValues[2])
            if (route != null) {
                return ParsedCommand(
                    rawInput = rawInput,
                    normalizedInput = normalizedInput,
                    locale = locale,
                    intent = CommandIntent(IntentName.OPEN_SCREEN, 0.90, "open_screen_v1"),
                    entities = ResolvedEntities(targetRoute = route),
                    policy = ExecutionPolicy(
                        transactionClass = TransactionClass.HARD_ATOMIC,
                        riskLevel = RiskLevel.LOW,
                        requiresConfirmation = false
                    )
                )
            }
        }

        // Fuzzy fallback path for broken words/order:
        // keep confidence medium so policy guard asks confirmation when needed.
        val hasAdd = hasApproxWord(normalizedInput, setOf("add", "jama", "credit", "plus", "jodo", "jod"))
        val hasClear = hasApproxWord(normalizedInput, setOf("clear", "saaf", "settle", "nipta"))
        val hasReminder = hasApproxWord(normalizedInput, setOf("reminder", "yaad", "send", "bhejo", "bheja", "lagao", "dila", "dilao"))
        val hasCustomerWord = hasApproxWord(normalizedInput, setOf("customer", "grahak"))
        val hasOpen = hasApproxWord(normalizedInput, setOf("open", "khol", "jao", "go"))
        val hasSetting = hasApproxWord(normalizedInput, setOf("setting", "set"))

        if (hasOpen) {
            val route = resolveRoute(normalizedInput)
            if (route != null) {
                return ParsedCommand(
                    rawInput = rawInput,
                    normalizedInput = normalizedInput,
                    locale = locale,
                    intent = CommandIntent(IntentName.OPEN_SCREEN, 0.88, "open_screen_fuzzy_v1"),
                    entities = ResolvedEntities(targetRoute = route),
                    policy = ExecutionPolicy(
                        transactionClass = TransactionClass.HARD_ATOMIC,
                        riskLevel = RiskLevel.LOW,
                        requiresConfirmation = false
                    )
                )
            }
        }

        if (hasCustomerWord && hasAdd) {
            val phone = extractPhone(normalizedInput)
            val name = extractCustomerCandidate(normalizedInput)
            if (!name.isNullOrBlank()) {
                return ParsedCommand(
                    rawInput = rawInput,
                    normalizedInput = normalizedInput,
                    locale = locale,
                    intent = CommandIntent(IntentName.CUSTOMER_ADD, 0.84, "customer_add_fuzzy_v1"),
                    entities = ResolvedEntities(customerName = name, customerPhone = phone),
                    policy = ExecutionPolicy(
                        transactionClass = TransactionClass.HARD_ATOMIC,
                        riskLevel = RiskLevel.LOW,
                        requiresConfirmation = false
                    )
                )
            }
        }

        if (hasAdd && !hasSetting) {
            val amount = extractAmount(normalizedInput)
            val name = extractCustomerCandidate(normalizedInput)
            if (amount != null && !name.isNullOrBlank()) {
                return ParsedCommand(
                    rawInput = rawInput,
                    normalizedInput = normalizedInput,
                    locale = locale,
                    intent = CommandIntent(IntentName.LEDGER_ADD, 0.83, "ledger_add_fuzzy_v1"),
                    entities = ResolvedEntities(customerName = name, amount = amount),
                    policy = ExecutionPolicy(
                        transactionClass = TransactionClass.HARD_ATOMIC,
                        riskLevel = RiskLevel.MEDIUM,
                        requiresConfirmation = false
                    )
                )
            }
        }

        if (hasClear) {
            val name = extractCustomerCandidate(normalizedInput)
            if (!name.isNullOrBlank()) {
                return ParsedCommand(
                    rawInput = rawInput,
                    normalizedInput = normalizedInput,
                    locale = locale,
                    intent = CommandIntent(IntentName.BILL_CLEAR, 0.82, "bill_clear_fuzzy_v1"),
                    entities = ResolvedEntities(customerName = name),
                    policy = ExecutionPolicy(
                        transactionClass = TransactionClass.HARD_ATOMIC,
                        riskLevel = RiskLevel.HIGH,
                        requiresConfirmation = true
                    )
                )
            }
        }

        if (hasReminder) {
            val name = extractCustomerCandidate(normalizedInput)
            if (!name.isNullOrBlank()) {
                return ParsedCommand(
                    rawInput = rawInput,
                    normalizedInput = normalizedInput,
                    locale = locale,
                    intent = CommandIntent(IntentName.REMINDER_SEND, 0.82, "reminder_fuzzy_v1"),
                    entities = ResolvedEntities(customerName = name),
                    policy = ExecutionPolicy(
                        transactionClass = TransactionClass.FINANCIAL_ATOMIC_NOTIFICATION_EVENTUAL,
                        riskLevel = RiskLevel.LOW,
                        requiresConfirmation = false
                    )
                )
            }
        }

        settingPattern.matchEntire(normalizedInput)?.let { match ->
            return ParsedCommand(
                rawInput = rawInput,
                normalizedInput = normalizedInput,
                locale = locale,
                intent = CommandIntent(IntentName.SETTING_UPDATE, 0.87, "setting_update_v1"),
                entities = ResolvedEntities(
                    settingKey = match.groupValues[1].trim(),
                    settingValue = match.groupValues[2].trim()
                ),
                policy = ExecutionPolicy(
                    transactionClass = TransactionClass.HARD_ATOMIC,
                    riskLevel = RiskLevel.MEDIUM,
                    requiresConfirmation = true
                )
            )
        }

        balanceAskKaPattern.matchEntire(normalizedInput)?.let { match ->
            val name = cleanCustomerName(match.groupValues[1])
            if (name.isNotBlank() && !isAggregateNoiseName(name)) {
                return ParsedCommand(
                    rawInput = rawInput,
                    normalizedInput = normalizedInput,
                    locale = locale,
                    intent = CommandIntent(IntentName.LEDGER_QUERY, 0.88, "ledger_query_balance_v1"),
                    entities = ResolvedEntities(customerName = name),
                    policy = ExecutionPolicy(
                        transactionClass = TransactionClass.HARD_ATOMIC,
                        riskLevel = RiskLevel.LOW,
                        requiresConfirmation = false
                    )
                )
            }
        }

        balanceAskKiPattern.matchEntire(normalizedInput)?.let { match ->
            val name = cleanCustomerName(match.groupValues[1])
            if (name.isNotBlank() && !isAggregateNoiseName(name)) {
                return ParsedCommand(
                    rawInput = rawInput,
                    normalizedInput = normalizedInput,
                    locale = locale,
                    intent = CommandIntent(IntentName.LEDGER_QUERY, 0.88, "ledger_query_balance_v1"),
                    entities = ResolvedEntities(customerName = name),
                    policy = ExecutionPolicy(
                        transactionClass = TransactionClass.HARD_ATOMIC,
                        riskLevel = RiskLevel.LOW,
                        requiresConfirmation = false
                    )
                )
            }
        }

        if (isLedgerAggregateQuestion(normalizedInput)) {
            return ParsedCommand(
                rawInput = rawInput,
                normalizedInput = normalizedInput,
                locale = locale,
                intent = CommandIntent(IntentName.LEDGER_QUERY, 0.88, "ledger_query_aggregate_v1"),
                entities = ResolvedEntities(),
                policy = ExecutionPolicy(
                    transactionClass = TransactionClass.HARD_ATOMIC,
                    riskLevel = RiskLevel.LOW,
                    requiresConfirmation = false
                )
            )
        }

        return ParsedCommand(
            rawInput = rawInput,
            normalizedInput = normalizedInput,
            locale = locale,
            intent = CommandIntent(IntentName.UNKNOWN, 0.0, "unknown_v1"),
            entities = ResolvedEntities(),
            policy = ExecutionPolicy(
                transactionClass = TransactionClass.HARD_ATOMIC,
                riskLevel = RiskLevel.HIGH,
                requiresConfirmation = true
            )
        )
    }
}
