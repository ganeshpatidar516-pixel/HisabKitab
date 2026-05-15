package com.ganesh.hisabkitabpro.commandos.normalize

class InputNormalizer {
    private val numberWordMap = mapOf(
        "ek" to 1L,
        "do" to 2L,
        "teen" to 3L,
        "char" to 4L,
        "chaar" to 4L,
        "paanch" to 5L,
        "cheh" to 6L,
        "chhe" to 6L,
        "saat" to 7L,
        "aath" to 8L,
        "nau" to 9L,
        "das" to 10L,
        "sau" to 100L
    )
    private val tokenAlias = mapOf(
        "kr" to "karo",
        "kro" to "karo",
        "krdo" to "karo",
        "karo" to "karo",
        "karoo" to "karo",
        "krr" to "karo",
        "bil" to "bill",
        "bll" to "bill",
        "ad" to "add",
        "addr" to "add",
        "adde" to "add",
        "jma" to "jama",
        "jamma" to "jama",
        "jodo" to "add",
        "jode" to "add",
        "jodein" to "add",
        "jamao" to "jama",
        "juddo" to "add",
        "addo" to "add",
        "remindar" to "reminder",
        "remindr" to "reminder",
        "remider" to "reminder",
        "rminder" to "reminder",
        "yad" to "reminder",
        "yrad" to "reminder",
        "custmer" to "customer",
        "costomer" to "customer",
        "coustomer" to "customer",
        "grahak" to "customer",
        "graahak" to "customer",
        "hisab" to "bill",
        "hisaab" to "bill",
        "hishab" to "bill",
        "balance" to "bill",
        "niptado" to "clear",
        "settel" to "settle",
        "seting" to "setting",
        "stting" to "setting"
    )

    private val devanagariDigitMap = mapOf(
        '०' to '0',
        '१' to '1',
        '२' to '2',
        '३' to '3',
        '४' to '4',
        '५' to '5',
        '६' to '6',
        '७' to '7',
        '८' to '8',
        '९' to '9'
    )

    private val hindiPhraseAlias = listOf(
        "को" to "ko",
        "का" to "ka",
        "की" to "ki",
        "के" to "ke",
        "में" to "me",
        "मे" to "me",
        "खाते में" to "account me",
        "खाते मे" to "account me",
        "खाता" to "account",
        "ऐड" to "add",
        "जोड़ो" to "add",
        "जोडो" to "add",
        "जोड़" to "add",
        "जमा" to "jama",
        "रिमाइंडर" to "reminder",
        "याद" to "yaad",
        "भेजो" to "bhejo",
        "भेजा" to "bhejo",
        "करो" to "karo",
        "क्लीयर" to "clear",
        "साफ" to "saaf",
        "सेटल" to "settle"
    )

    fun normalize(raw: String): String {
        var lowered = raw.trim().lowercase()
        lowered = normalizeDevanagariDigits(lowered)
        lowered = lowered.replace("₹", " ")
            .replace(Regex("\\brupees?\\b"), " ")
            .replace(Regex("\\brs\\.?\\b"), " ")
        lowered = normalizeHindiPhrases(lowered)
        lowered = normalizeSpokenPhrases(lowered)
        // Keep Unicode marks (\p{M}) so Hindi matras like "े" survive normalization.
        val stripped = lowered.replace(Regex("[^\\p{L}\\p{M}0-9\\s]"), " ")
        val compact = stripped.replace(Regex("\\s+"), " ").trim()
        val numberNormalized = normalizeNumberWords(compact)
        val aliasNormalized = normalizeAliases(numberNormalized)
        return removeRepeatedTokens(aliasNormalized)
    }

    private fun normalizeDevanagariDigits(input: String): String {
        val sb = StringBuilder(input.length)
        input.forEach { ch ->
            sb.append(devanagariDigitMap[ch] ?: ch)
        }
        return sb.toString()
    }

    private fun normalizeHindiPhrases(input: String): String {
        var out = input
        hindiPhraseAlias.forEach { (from, to) ->
            out = out.replace(from, " $to ")
        }
        return out
    }

    private fun normalizeSpokenPhrases(input: String): String {
        var s = input
        listOf(
            "jode jaaye" to "add karo",
            "jode jaye" to "add karo",
            "jama karaye" to "add karo",
            "jama kara de" to "add karo",
            "ke khate mein" to "ke account me",
            "ke khate me" to "ke account me",
            "ke khaate mein" to "ke account me",
            "ke khaate me" to "ke account me",
            "khate mein" to "account me",
            "khaate mein" to "account me"
        ).forEach { (from, to) -> s = s.replace(from, to) }
        return s
    }

    private fun normalizeNumberWords(input: String): String {
        val tokens = input.split(" ").toMutableList()
        if (tokens.size < 2) {
            return input
        }

        for (i in 0 until tokens.size - 1) {
            val first = numberWordMap[tokens[i]]
            val second = numberWordMap[tokens[i + 1]]
            if (first != null && second != null && second == 100L) {
                tokens[i] = (first * second).toString()
                tokens[i + 1] = ""
            }
        }
        return tokens.filter { it.isNotBlank() }.joinToString(" ")
    }

    private fun normalizeAliases(input: String): String {
        return input
            .split(" ")
            .map { tokenAlias[it] ?: it }
            .joinToString(" ")
    }

    private fun removeRepeatedTokens(input: String): String {
        val tokens = input.split(" ").filter { it.isNotBlank() }
        if (tokens.isEmpty()) return input
        val deduped = ArrayList<String>(tokens.size)
        var previous: String? = null
        for (token in tokens) {
            if (token != previous) deduped.add(token)
            previous = token
        }
        return deduped.joinToString(" ")
    }
}
