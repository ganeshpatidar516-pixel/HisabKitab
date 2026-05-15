package com.ganesh.hisabkitabpro.commandos.adapters.hisabkitab

import com.ganesh.hisabkitabpro.addon.reminder.AssistantCustomerReminderDispatcher
import com.ganesh.hisabkitabpro.commandos.adapters.contracts.DomainAdapter
import com.ganesh.hisabkitabpro.commandos.adapters.contracts.ReminderDispatchReport
import com.ganesh.hisabkitabpro.data.repository.local.ProductDao
import com.ganesh.hisabkitabpro.data.local.ProductEntity
import com.ganesh.hisabkitabpro.domain.inventory.InventoryCommandParser
import com.ganesh.hisabkitabpro.domain.model.Customer
import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import com.ganesh.hisabkitabpro.domain.repository.CUSTOMER_AI_SNAPSHOT_LIMIT
import com.ganesh.hisabkitabpro.domain.repository.CustomerRepository
import com.ganesh.hisabkitabpro.domain.repository.SettingsRepository
import com.ganesh.hisabkitabpro.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.firstOrNull
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class HisabKitabDomainAdapter @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository,
    private val assistantReminderDispatcher: AssistantCustomerReminderDispatcher,
    private val productDao: ProductDao
) : DomainAdapter {

    override suspend fun searchCustomer(name: String): Boolean {
        return resolveCustomerId(name) != null
    }

    override suspend fun suggestCustomerNames(query: String, limit: Int): List<String> {
        val q = query.trim()
        if (q.isEmpty() || limit <= 0) return emptyList()
        return rankCustomers(q, loadCustomersForMatching(q))
            .take(limit)
            .map { it.customer.name }
    }

    override suspend fun answerLedgerInsightQuery(normalizedInput: String, customerHint: String?): String? {
        val q = normalizedInput.lowercase().trim()
        val focus = customerHint?.trim()?.takeIf { it.isNotBlank() }
            ?: extractNameForBalanceQuery(normalizedInput)

        if (focus != null) {
            if ((customerRepository.getCustomerCount().firstOrNull() ?: 0) == 0) {
                return "अभी ऐप में कोई ग्राहक नहीं है। पहले ग्राहक जोड़ें, फिर मैं बैलेंस बता सकता हूँ।"
            }
            val id = resolveCustomerId(focus)
            if (id != null) {
                val c = customerRepository.getCustomerById(id) ?: return null
                return formatCustomerBalanceLine(c)
            }
            val sug = suggestCustomerNames(focus, 5)
            return if (sug.isNotEmpty()) {
                "कोई बदलाव नहीं किया। '$focus' से साफ़ मैच नहीं मिला। क्या इनमें से कोई: ${sug.joinToString(", ")}? पूरा नाम लिखकर फिर पूछें।"
            } else {
                "कोई बदलाव नहीं किया। '$focus' नाम का ग्राहक सूची में नहीं मिला। ग्राहक स्क्रीन पर नाम जाँच लें।"
            }
        }

        if (!looksLikeLedgerQuestion(q)) return null

        val count = customerRepository.getCustomerCount().firstOrNull() ?: 0
        if (count == 0) {
            return "अभी ऐप में कोई ग्राहक डेटा नहीं है। पहले ग्राहक जोड़ें, फिर कुल उधार/सारांश पूछ सकते हैं।"
        }

        val netPaise = customerRepository.getOverallNetBalancePaise().firstOrNull() ?: 0L
        val debtors = customerRepository.getTopDebtorsLimited(3)

        val lines = mutableListOf<String>()
        lines.add("📒 आपके डेटा से (लोकल ग्राहक सूची): कुल $count ग्राहक।")
        lines.add("कुल नेट बैलेंस (सभी balanceCache का योग): ₹${rupees(netPaise)}।")
        if (debtors.isNotEmpty()) {
            val top = debtors.joinToString { "${it.name}: ₹${rupees(it.balanceCache)}" }
            lines.add("सबसे ज़्यादा बकाया (शीर्ष ${debtors.size}): $top।")
        }
        lines.add("डिटेल के लिए ग्राहक की लेजर स्क्रीन खोलें — यह सारांश तेज़ कैश से आता है।")
        return lines.joinToString("\n")
    }

    override suspend fun addCustomer(name: String, phone: String): Boolean {
        val cleanName = name.trim()
        val cleanPhone = phone.filter(Char::isDigit)
        if (cleanName.isBlank() || cleanPhone.length < 10) return false
        val alreadyExists = customerRepository.searchCustomers(cleanName)
            .any { it.name.equals(cleanName, ignoreCase = true) }
        if (alreadyExists) return true
        return runCatching {
            customerRepository.addCustomer(
                Customer(
                    name = cleanName,
                    phone = cleanPhone
                )
            )
        }.isSuccess
    }

    override suspend fun addLedgerEntry(customerName: String, amount: Long): Boolean {
        val customerId = resolveCustomerId(customerName) ?: return false
        val paise = amount.coerceAtLeast(0L) * 100L
        if (paise <= 0L) return false
        val transaction = Transaction(
            customerId = customerId,
            amount = paise,
            type = TransactionType.CREDIT,
            note = "Super Command: ledger add",
            txnRef = UUID.randomUUID().toString()
        )
        return transactionRepository.addTransaction(transaction).isSuccess
    }

    override suspend fun clearBill(customerName: String): Boolean {
        val customerId = resolveCustomerId(customerName) ?: return false
        val balancePaise = transactionRepository.getCalculateBalance(customerId)
        if (balancePaise <= 0L) return false

        // Financially safe settlement: only settle outstanding receivable with DEBIT entry.
        val settlement = Transaction(
            customerId = customerId,
            amount = balancePaise,
            type = TransactionType.DEBIT,
            note = "Super Command: bill clear settlement",
            txnRef = UUID.randomUUID().toString()
        )
        return transactionRepository.addTransaction(settlement).isSuccess
    }

    override suspend fun sendReminder(customerName: String): Boolean {
        return sendReminderWithReport(customerName).success
    }

    override suspend fun sendReminderWithReport(customerName: String): ReminderDispatchReport {
        val customerId = resolveCustomerId(customerName) ?: return ReminderDispatchReport(
            success = false,
            customerName = customerName,
            reason = "customer_not_found"
        )
        return assistantReminderDispatcher.dispatchForCustomerId(customerId)
    }

    override suspend fun updateSetting(key: String, value: String): Boolean {
        val current = settingsRepository.getSettings().firstOrNull() ?: return false
        val updated = when (key.lowercase()) {
            "voiceassistantenabled", "voice_assistant_enabled" -> {
                current.copy(voiceAssistantEnabled = value.toBooleanStrictOrNull() ?: return false)
            }
            "smartsuggestionsenabled", "smart_suggestions_enabled" -> {
                current.copy(smartSuggestionsEnabled = value.toBooleanStrictOrNull() ?: return false)
            }
            "language", "languagecode", "language_code" -> {
                current.copy(languageCode = value.lowercase())
            }
            "currency" -> {
                current.copy(currency = value)
            }
            "gstenabled", "gst_enabled" -> {
                current.copy(gstEnabled = value.toBooleanStrictOrNull() ?: return false)
            }
            "gstrate", "gst_rate" -> {
                val rate = value.toDoubleOrNull() ?: return false
                current.copy(gstRate = abs(rate))
            }
            else -> return false
        }
        settingsRepository.saveSettings(updated)
        return true
    }

    override suspend fun handleInventoryCommand(normalizedInput: String): String? {
        val command = InventoryCommandParser.parse(normalizedInput) ?: return null
        return when (command) {
            is InventoryCommandParser.Command.Summary -> inventorySummary()
            is InventoryCommandParser.Command.LowStock -> lowStockSummary()
            is InventoryCommandParser.Command.FindProduct -> productLookup(command.query)
            is InventoryCommandParser.Command.AddProduct -> addProductFromAssistant(command)
            is InventoryCommandParser.Command.AdjustStock -> adjustStockFromAssistant(command)
        }
    }

    private suspend fun loadCustomersForMatching(query: String): List<Customer> {
        val cleaned = query.trim()
        if (cleaned.isEmpty()) return emptyList()
        val seen = linkedSetOf<Long>()
        val out = mutableListOf<Customer>()
        val tokens = cleaned.split(Regex("\\s+")).filter { it.length >= 2 }.take(4)
        val queries = (tokens + cleaned).distinct()
        for (q in queries) {
            customerRepository.searchCustomers(q).forEach { c ->
                if (!c.isDeleted && seen.add(c.id)) {
                    out.add(c)
                    if (out.size >= CUSTOMER_AI_SNAPSHOT_LIMIT) return out
                }
            }
        }
        if (out.isEmpty()) {
            out.addAll(customerRepository.getTopDebtorsLimited(50))
        }
        return out.take(CUSTOMER_AI_SNAPSHOT_LIMIT)
    }

    private suspend fun inventorySummary(): String {
        val products = productDao.getAllProducts().firstOrNull().orEmpty()
        if (products.isEmpty()) return "Inventory खाली है। पहले product add करें या barcode scan करें।"
        val low = products.count { it.stockQuantity <= it.minStockLevel }
        val saleValue = products.sumOf { it.stockQuantity * it.sellingPrice }
        val costValue = products.sumOf { it.stockQuantity * it.purchasePrice }
        return buildString {
            append("📦 Inventory summary: ${products.size} products.")
            append(" Sale value ₹${rupeesDouble(saleValue)}, cost value ₹${rupeesDouble(costValue)}.")
            if (low > 0) append(" Low-stock items: $low.")
            else append(" Low-stock items: 0.")
        }
    }

    private suspend fun lowStockSummary(): String {
        val low = productDao.getLowStockProductsOnce(limit = 8)
        if (low.isEmpty()) return "Inventory healthy है। कोई low-stock product नहीं मिला।"
        val lines = low.joinToString { p ->
            "${p.name}: ${formatQty(p.stockQuantity)} ${p.unit} (min ${formatQty(p.minStockLevel)})"
        }
        return "⚠️ Low-stock products: $lines."
    }

    private suspend fun productLookup(query: String): String {
        val matches = productDao.searchProductsOnce(query, limit = 5)
        if (matches.isEmpty()) return "Product '$query' inventory में नहीं मिला। नाम, SKU या barcode check करें।"
        return matches.joinToString(separator = "\n") { p ->
            "${p.name}: stock ${formatQty(p.stockQuantity)} ${p.unit}, price ₹${rupeesDouble(p.sellingPrice)}" +
                (p.barcode?.takeIf { it.isNotBlank() }?.let { ", barcode $it" } ?: "")
        }
    }

    private suspend fun addProductFromAssistant(command: InventoryCommandParser.Command.AddProduct): String {
        val existing = productDao.getProductByExactName(command.name)
        if (existing != null) {
            productDao.adjustStockById(existing.id, command.quantity)
            val priceChanged = command.sellingPrice > 0.0 && command.sellingPrice != existing.sellingPrice
            if (priceChanged) {
                productDao.updateProduct(
                    existing.copy(
                        sellingPrice = command.sellingPrice,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            return "Product '${existing.name}' already exists. Stock +${formatQty(command.quantity)} synced."
        }

        val now = System.currentTimeMillis()
        productDao.insertProduct(
            ProductEntity(
                id = now.toString(),
                name = command.name,
                purchasePrice = command.sellingPrice,
                sellingPrice = command.sellingPrice,
                stockQuantity = command.quantity,
                barcode = command.barcode,
                createdAt = now,
                updatedAt = now
            )
        )
        return "Product added: ${command.name}, stock ${formatQty(command.quantity)}, price ₹${rupeesDouble(command.sellingPrice)}."
    }

    private suspend fun adjustStockFromAssistant(command: InventoryCommandParser.Command.AdjustStock): String {
        val product = productDao.getProductByExactName(command.productName)
            ?: productDao.searchProductsOnce(command.productName, limit = 1).firstOrNull()
            ?: return "Product '${command.productName}' नहीं मिला। Stock change नहीं किया।"
        productDao.adjustStockById(product.id, command.delta)
        val label = if (command.delta >= 0.0) "+${formatQty(command.delta)}" else "-${formatQty(abs(command.delta))}"
        return "Inventory synced: ${product.name} stock $label ${product.unit}."
    }

    private data class RankedCustomer(val customer: Customer, val rank: Int)

    private fun rankCustomers(query: String, customers: List<Customer>): List<RankedCustomer> {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return emptyList()
        if (q.length < 2) {
            val exact = customers.firstOrNull { it.name.equals(query, ignoreCase = true) }
            return if (exact != null) listOf(RankedCustomer(exact, 0)) else emptyList()
        }
        return customers.mapNotNull { c ->
            val n = c.name.lowercase(Locale.ROOT)
            val rank: Int? = when {
                c.name.equals(query, ignoreCase = true) -> 0
                q.length >= 3 && (n.contains(q) || (n.length >= 3 && q.contains(n))) -> 1
                else -> {
                    val d = levenshtein(q, n)
                    val maxDist = when {
                        q.length <= 4 -> 2
                        q.length <= 8 -> 3
                        else -> 4
                    }
                    if (d <= maxDist) d + 2 else null
                }
            }
            rank?.let { RankedCustomer(c, it) }
        }.sortedWith(compareBy({ it.rank }, { it.customer.name.lowercase(Locale.ROOT) }))
    }

    private suspend fun resolveCustomerId(inputName: String): Long? {
        val query = inputName.trim()
        if (query.isEmpty()) return null
        val ranked = rankCustomers(query, loadCustomersForMatching(query))
        val best = ranked.firstOrNull() ?: return null
        if (best.rank > 6) return null
        return best.customer.id
    }

    private fun rupees(paise: Long): String =
        String.format(Locale.US, "%.2f", abs(paise) / 100.0)

    private fun rupeesDouble(value: Double): String =
        String.format(Locale.US, "%.2f", abs(value))

    private fun formatQty(value: Double): String =
        if (value % 1.0 == 0.0) String.format(Locale.US, "%.0f", value)
        else String.format(Locale.US, "%.2f", value)

    private fun formatCustomerBalanceLine(c: Customer): String {
        val amt = abs(c.balanceCache) / 100.0
        val hint = when {
            c.balanceCache > 0L ->
                "यह राशि ग्राहक से वसूलनी (उधार) की ओर है — लेजर में पूरी डिटेल देखें।"
            c.balanceCache < 0L ->
                "नकारात्मक बैलेंस अक्सर अग्रिम या आपको देना बाकी दिखाता है — लेजर खोलकर जाँच लें।"
            else -> "खाता अभी सेटल दिख रहा है।"
        }
        return "${c.name} का बैलेंस: ₹${String.format(Locale.US, "%.2f", amt)}। $hint"
    }

    private fun extractNameForBalanceQuery(normalized: String): String? {
        val n = normalized.trim()
        Regex("^(.+?)\\s+ka\\s+(?:balance|bill)(?!\\s+clear)\\b").find(n)?.groupValues?.getOrNull(1)
            ?.let { return cleanInsightName(it) }
        Regex("^(.+?)\\s+ki\\s+(?:balance|bill)(?!\\s+clear)\\b").find(n)?.groupValues?.getOrNull(1)
            ?.let { return cleanInsightName(it) }
        Regex("^(?:balance|bill)\\s+(.+)$").find(n)?.groupValues?.getOrNull(1)?.let { return cleanInsightName(it) }
        return null
    }

    private fun cleanInsightName(raw: String): String {
        return raw
            .replace(Regex("\\b(kya|hai|batao|bata|dekho|please|plz)\\b", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trimEnd('?', '.', '।')
    }

    private fun looksLikeLedgerQuestion(lowercaseNormalized: String): Boolean {
        val q = lowercaseNormalized
        if (q.contains("balance") || q.contains("hisaab") || q.contains("hisab")) return true
        if (q.contains("bill") && !q.contains("clear") &&
            (q.contains("kitna") || q.contains("kya") || q.contains("total") || q.contains("sab"))
        ) {
            return true
        }
        if (q.contains("udhaar") || q.contains("udhar") || q.contains("bakaya") || q.contains("baki")) return true
        if ((q.contains("kitne") || q.contains("kitna")) &&
            (q.contains("customer") || q.contains("grahak") || q.contains("graahak") || q.contains("ग्राहक"))
        ) {
            return true
        }
        if (q.contains("total") && (q.contains("customer") || q.contains("udhaar") || q.contains("udhar") || q.contains("balance"))) return true
        if ((q.contains("sab") || q.contains("saare") || q.contains("pure")) &&
            (q.contains("balance") || q.contains("udhaar") || q.contains("udhar") || q.contains("hisab"))
        ) {
            return true
        }
        if (q.contains("debtor") || q.contains("lenaar") || q.contains("vasuli")) return true
        if (q.contains("sabse") && (q.contains("zyada") || q.contains("jyada")) &&
            (q.contains("udhaar") || q.contains("udhar") || q.contains("balance"))
        ) {
            return true
        }
        return false
    }

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
}
