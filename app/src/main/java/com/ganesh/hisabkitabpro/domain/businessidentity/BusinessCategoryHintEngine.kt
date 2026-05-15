package com.ganesh.hisabkitabpro.domain.businessidentity

/**
 * Phase 3 lite: on-device hints from business name + taxonomy picks (no network).
 */
object BusinessCategoryHintEngine {

    private val TOKEN_SPLIT = Regex("[^a-zA-Z0-9\u0900-\u097F]+")

    fun suggest(businessName: String, picks: List<CategoryPick>, max: Int): List<CategoryPick> {
        if (max <= 0 || picks.isEmpty()) return emptyList()
        val tokens = tokenize(businessName)
        if (tokens.isEmpty()) return emptyList()
        return picks
            .map { pick -> pick to scorePick(pick, tokens) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
            .distinctBy { it.path }
            .take(max)
    }

    private fun tokenize(name: String): List<String> =
        TOKEN_SPLIT.split(name.trim())
            .map { it.lowercase() }
            .filter { it.length >= 2 }

    private fun scorePick(pick: CategoryPick, tokens: List<String>): Int {
        var score = 0
        for (t in tokens) {
            if (t.isEmpty()) continue
            if (pick.searchBlob.contains(t)) score += 2
            else if (pick.path.lowercase().contains(t)) score += 1
        }
        return score
    }
}
