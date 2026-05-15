package com.ganesh.hisabkitabpro.domain.businessidentity

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.LinkedHashSet

/**
 * Read-only global + India-oriented business category taxonomy (Phase 2 stub).
 * Persisted value remains a single string on [com.ganesh.hisabkitabpro.domain.model.BusinessProfile.businessCategory].
 */
data class BusinessCategoryTaxonomyRoot(
    @SerializedName("version")
    val version: Int = 1,
    @SerializedName("industries")
    val industries: List<TaxonomyIndustry> = emptyList(),
)

data class TaxonomyIndustry(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("label")
    val label: String = "",
    @SerializedName("synonyms")
    val synonyms: List<String> = emptyList(),
    @SerializedName("subcategories")
    val subcategories: List<TaxonomySubcategory> = emptyList(),
)

data class TaxonomySubcategory(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("label")
    val label: String = "",
    @SerializedName("synonyms")
    val synonyms: List<String> = emptyList(),
    @SerializedName("tags")
    val tags: List<String> = emptyList(),
)

/** One selectable row; [path] is written to business_profile.businessCategory. */
data class CategoryPick(
    val path: String,
    val searchBlob: String,
)

object BusinessCategoryTaxonomyCatalog {

    fun parseJson(json: String): BusinessCategoryTaxonomyRoot =
        Gson().fromJson(json, BusinessCategoryTaxonomyRoot::class.java)
            ?: BusinessCategoryTaxonomyRoot()

    fun loadFromStream(stream: InputStream): BusinessCategoryTaxonomyRoot {
        val text = stream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
        return runCatching { parseJson(text) }.getOrDefault(BusinessCategoryTaxonomyRoot())
    }

    fun flatten(root: BusinessCategoryTaxonomyRoot): List<CategoryPick> {
        val out = LinkedHashSet<String>()
        val picks = mutableListOf<CategoryPick>()
        fun addPath(path: String, extraTokens: Collection<String>) {
            if (path.isBlank() || path in out) return
            out += path
            val blob = (sequenceOf(path) + extraTokens.asSequence())
                .joinToString(" ")
                .lowercase()
            picks += CategoryPick(path = path, searchBlob = blob)
        }
        for (ind in root.industries) {
            val indLabel = ind.label.trim()
            if (indLabel.isEmpty()) continue
            addPath(indLabel, ind.synonyms)
            for (sub in ind.subcategories) {
                val subLabel = sub.label.trim()
                if (subLabel.isEmpty()) continue
                val base = "$indLabel › $subLabel"
                addPath(base, ind.synonyms + sub.synonyms)
                val tags = sub.tags.map { it.trim() }.filter { it.isNotEmpty() }
                if (tags.isEmpty()) {
                    continue
                }
                for (tag in tags) {
                    addPath("$base › $tag", ind.synonyms + sub.synonyms + listOf(tag))
                }
            }
        }
        return picks
    }
}
