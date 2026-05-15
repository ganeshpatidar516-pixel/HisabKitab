@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.ganesh.hisabkitabpro.ui.settings.businessidentity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ManageSearch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ganesh.hisabkitabpro.BuildConfig
import com.ganesh.hisabkitabpro.domain.businessidentity.BusinessCategoryHintEngine
import com.ganesh.hisabkitabpro.domain.businessidentity.BusinessCategoryTaxonomyCatalog
import com.ganesh.hisabkitabpro.domain.businessidentity.BusinessCategoryTaxonomyRoot
import com.ganesh.hisabkitabpro.domain.businessidentity.CategoryPick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val TAXONOMY_ASSET = "business_category_taxonomy.json"
private const val SEARCH_DEBOUNCE_MS = 260L
private const val MIN_QUERY_FOR_SEARCH = 2
private const val MAX_MATCHES = 60

/**
 * Taxonomy-aware category: compact field + bottom-sheet directory (debounced search, grouped, recents).
 */
@Composable
fun BusinessCategoryTaxonomyField(
    businessCategory: String,
    onBusinessCategoryChange: (String) -> Unit,
    businessNameHint: String = "",
    modifier: Modifier = Modifier,
) {
    if (!BuildConfig.BUSINESS_IDENTITY_TAXONOMY_STUB) {
        OutlinedTextField(
            value = businessCategory,
            onValueChange = onBusinessCategoryChange,
            label = { Text("Business category") },
            modifier = modifier.fillMaxWidth(),
        )
        return
    }

    val context = LocalContext.current
    var picks by remember { mutableStateOf<List<CategoryPick>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        loading = true
        picks = withContext(Dispatchers.IO) {
            val root = runCatching {
                context.assets.open(TAXONOMY_ASSET).use { stream ->
                    BusinessCategoryTaxonomyCatalog.loadFromStream(stream)
                }
            }.getOrElse { BusinessCategoryTaxonomyRoot() }
            BusinessCategoryTaxonomyCatalog.flatten(root)
        }
        loading = false
    }

    val recentPaths = remember { mutableStateListOf<String>() }
    var sheetOpen by remember { mutableStateOf(false) }

    fun rememberSelection(path: String) {
        recentPaths.remove(path)
        recentPaths.add(0, path)
        while (recentPaths.size > 10) recentPaths.removeAt(recentPaths.lastIndex)
    }

    fun applyPath(path: String) {
        onBusinessCategoryChange(path)
        rememberSelection(path)
        sheetOpen = false
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            OutlinedTextField(
                value = businessCategory,
                onValueChange = onBusinessCategoryChange,
                label = { Text("Category") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { sheetOpen = true }) {
                Icon(
                    imageVector = Icons.Outlined.ManageSearch,
                    contentDescription = "Browse category directory",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    if (sheetOpen) {
        CategoryDirectorySheet(
            picks = picks,
            loading = loading,
            businessNameHint = businessNameHint,
            recentPaths = recentPaths,
            onDismiss = { sheetOpen = false },
            onSelect = ::applyPath,
        )
    }
}

@Composable
private fun CategoryDirectorySheet(
    picks: List<CategoryPick>,
    loading: Boolean,
    businessNameHint: String,
    recentPaths: SnapshotStateList<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var searchRaw by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    LaunchedEffect(searchRaw) {
        delay(SEARCH_DEBOUNCE_MS)
        debouncedQuery = searchRaw
    }
    var selectedIndustry by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        searchRaw = ""
        debouncedQuery = ""
        selectedIndustry = null
    }

    val nameHints = remember(businessNameHint, picks) {
        BusinessCategoryHintEngine.suggest(businessNameHint, picks, max = 4)
    }
    val trending = remember(picks) {
        if (picks.isEmpty()) return@remember emptyList()
        picks.groupingBy { it.path.substringBefore(" › ").trim().ifEmpty { it.path } }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .distinct()
            .take(14)
    }
    val narrowed = remember(selectedIndustry, picks) {
        val ind = selectedIndustry ?: return@remember emptyList()
        picks.filter { p ->
            val root = p.path.substringBefore(" › ").trim().ifEmpty { p.path }
            root == ind
        }.take(MAX_MATCHES)
    }
    val filteredMatches = remember(debouncedQuery, picks) {
        val q = debouncedQuery.trim().lowercase()
        if (q.length < MIN_QUERY_FOR_SEARCH) return@remember emptyList()
        picks.filter { it.searchBlob.contains(q) }.take(MAX_MATCHES)
    }
    val groupedMatches = remember(filteredMatches) {
        filteredMatches.groupBy { it.path.substringBefore(" › ").trim().ifEmpty { "Other" } }
            .toList()
            .sortedBy { it.first.lowercase() }
    }
    val showSearchResults = debouncedQuery.trim().length >= MIN_QUERY_FOR_SEARCH

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Category directory",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            OutlinedTextField(
                value = searchRaw,
                onValueChange = { searchRaw = it },
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (loading && picks.isEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (showSearchResults) {
                        if (groupedMatches.isEmpty()) {
                            item {
                                Text(
                                    "No matches. Try another word or browse industries below.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                        } else {
                            groupedMatches.forEach { (group, list) ->
                                item(key = "h-$group") {
                                    Text(
                                        group,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                    )
                                }
                                items(list, key = { it.path }) { pick ->
                                    CategoryRow(
                                        path = pick.path,
                                        highlight = debouncedQuery.trim(),
                                        onClick = { onSelect(pick.path) },
                                    )
                                }
                            }
                        }
                    } else {
                        if (recentPaths.isNotEmpty()) {
                            item {
                                Text(
                                    "Recent",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            items(recentPaths.toList(), key = { it }) { path ->
                                CategoryRow(
                                    path = path,
                                    highlight = "",
                                    onClick = { onSelect(path) },
                                )
                            }
                        }
                        if (nameHints.isNotEmpty()) {
                            item {
                                Text(
                                    "Suggested",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 10.dp),
                                )
                            }
                            items(nameHints, key = { it.path }) { pick ->
                                CategoryRow(
                                    path = pick.path,
                                    highlight = "",
                                    onClick = { onSelect(pick.path) },
                                )
                            }
                        }
                        item {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Industries",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 10.dp),
                                )
                                if (selectedIndustry != null) {
                                    TextButton(onClick = { selectedIndustry = null }) {
                                        Text("Clear")
                                    }
                                }
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                for (label in trending) {
                                    FilterChip(
                                        selected = selectedIndustry == label,
                                        onClick = {
                                            selectedIndustry = if (selectedIndustry == label) null else label
                                        },
                                        label = {
                                            Text(
                                                label,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.labelLarge,
                                            )
                                        },
                                    )
                                }
                            }
                        }
                        items(narrowed, key = { it.path }) { pick ->
                            CategoryRow(
                                path = pick.path,
                                highlight = "",
                                onClick = { onSelect(pick.path) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    path: String,
    highlight: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            val hq = highlight.trim()
            if (hq.length >= MIN_QUERY_FOR_SEARCH) {
                val lowerPath = path.lowercase()
                val q = hq.lowercase()
                val idx = lowerPath.indexOf(q)
                if (idx >= 0) {
                    val end = (idx + hq.length).coerceAtMost(path.length)
                    Text(
                        buildAnnotatedString {
                            append(path.substring(0, idx))
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                ),
                            ) {
                                append(path.substring(idx, end))
                            }
                            append(path.substring(end))
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text(
                        path,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                Text(
                    path,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
