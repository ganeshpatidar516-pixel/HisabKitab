@file:OptIn(ExperimentalMaterial3Api::class)

package com.ganesh.hisabkitabpro.ui.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.domain.customers.CustomerListMenuTab
import com.ganesh.hisabkitabpro.domain.customers.CustomerListReminderSegment
import com.ganesh.hisabkitabpro.domain.customers.CustomerListSortOption

private val SheetMint = Color(0xFFE8F5E9)
private val SheetGreen = Color(0xFF2E7D32)
private val SheetGreenDark = Color(0xFF1B5E20)

/**
 * Bottom sheet: Sort By / Reminder Date + radio options + Clear / Apply (reference UX).
 */
@Composable
fun CustomerListSortSheet(
    draftTab: CustomerListMenuTab,
    onDraftTabChange: (CustomerListMenuTab) -> Unit,
    draftSort: CustomerListSortOption,
    onDraftSortChange: (CustomerListSortOption) -> Unit,
    draftReminderSegments: Set<CustomerListReminderSegment>,
    onDraftReminderSegmentsChange: (Set<CustomerListReminderSegment>) -> Unit,
    onDismiss: () -> Unit,
    onClearDraft: () -> Unit,
    onApply: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val bodyMaxHeight = (configuration.screenHeightDp * 0.5f).dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SheetMint,
        dragHandle = null
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = bodyMaxHeight)
            ) {
                Column(
                    Modifier
                        .width(120.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFF1F8F2))
                ) {
                    SheetSideTab(
                        label = stringResource(R.string.customer_sort_tab_sort_by),
                        selected = draftTab == CustomerListMenuTab.SORT_BY,
                        icon = Icons.Default.Sort,
                        onClick = { onDraftTabChange(CustomerListMenuTab.SORT_BY) }
                    )
                    HorizontalDivider(color = Color(0x33000000))
                    SheetSideTab(
                        label = stringResource(R.string.customer_sort_tab_reminder_date),
                        selected = draftTab == CustomerListMenuTab.REMINDER_DATE,
                        icon = Icons.Default.Schedule,
                        onClick = { onDraftTabChange(CustomerListMenuTab.REMINDER_DATE) }
                    )
                }
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    when (draftTab) {
                        CustomerListMenuTab.SORT_BY -> {
                            Text(
                                text = stringResource(R.string.customer_sort_sheet_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF546E7A),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            SortRadioRow(
                                label = stringResource(R.string.customer_sort_option_default),
                                selected = draftSort == CustomerListSortOption.DEFAULT,
                                onSelect = { onDraftSortChange(CustomerListSortOption.DEFAULT) }
                            )
                            SortRadioRow(
                                label = stringResource(R.string.customer_sort_option_last_payment),
                                selected = draftSort == CustomerListSortOption.LAST_PAYMENT,
                                onSelect = { onDraftSortChange(CustomerListSortOption.LAST_PAYMENT) }
                            )
                            SortRadioRow(
                                label = stringResource(R.string.customer_sort_option_latest_activity),
                                selected = draftSort == CustomerListSortOption.LATEST_ACTIVITY,
                                onSelect = { onDraftSortChange(CustomerListSortOption.LATEST_ACTIVITY) }
                            )
                            SortRadioRow(
                                label = stringResource(R.string.customer_sort_option_due_amount),
                                selected = draftSort == CustomerListSortOption.DUE_AMOUNT,
                                onSelect = { onDraftSortChange(CustomerListSortOption.DUE_AMOUNT) }
                            )
                            SortRadioRow(
                                label = stringResource(R.string.customer_sort_option_name),
                                selected = draftSort == CustomerListSortOption.NAME,
                                onSelect = { onDraftSortChange(CustomerListSortOption.NAME) }
                            )
                            SortRadioRow(
                                label = stringResource(R.string.customer_sort_option_defaulters),
                                selected = draftSort == CustomerListSortOption.DEFAULTERS,
                                onSelect = { onDraftSortChange(CustomerListSortOption.DEFAULTERS) }
                            )
                        }
                        CustomerListMenuTab.REMINDER_DATE -> {
                            Text(
                                text = stringResource(R.string.customer_sort_reminder_sheet_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF546E7A),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            ReminderCheckboxRow(
                                label = stringResource(R.string.customer_reminder_filter_today),
                                checked = draftReminderSegments.contains(CustomerListReminderSegment.TODAY),
                                onToggle = {
                                    onDraftReminderSegmentsChange(
                                        if (CustomerListReminderSegment.TODAY in draftReminderSegments) {
                                            draftReminderSegments - CustomerListReminderSegment.TODAY
                                        } else {
                                            draftReminderSegments + CustomerListReminderSegment.TODAY
                                        }
                                    )
                                }
                            )
                            ReminderCheckboxRow(
                                label = stringResource(R.string.customer_reminder_filter_pending),
                                checked = draftReminderSegments.contains(CustomerListReminderSegment.PENDING),
                                onToggle = {
                                    onDraftReminderSegmentsChange(
                                        if (CustomerListReminderSegment.PENDING in draftReminderSegments) {
                                            draftReminderSegments - CustomerListReminderSegment.PENDING
                                        } else {
                                            draftReminderSegments + CustomerListReminderSegment.PENDING
                                        }
                                    )
                                }
                            )
                            ReminderCheckboxRow(
                                label = stringResource(R.string.customer_reminder_filter_upcoming),
                                checked = draftReminderSegments.contains(CustomerListReminderSegment.UPCOMING),
                                onToggle = {
                                    onDraftReminderSegmentsChange(
                                        if (CustomerListReminderSegment.UPCOMING in draftReminderSegments) {
                                            draftReminderSegments - CustomerListReminderSegment.UPCOMING
                                        } else {
                                            draftReminderSegments + CustomerListReminderSegment.UPCOMING
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = Color(0x22000000))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SheetMint,
                shadowElevation = 6.dp,
                tonalElevation = 2.dp
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onClearDraft,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SheetGreenDark)
                    ) {
                        Text(stringResource(R.string.customer_sort_clear), fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onApply,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SheetGreen, contentColor = Color.White)
                    ) {
                        Text(stringResource(R.string.customer_sort_apply), fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetSideTab(
    label: String,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(36.dp)
                .background(if (selected) SheetGreen else Color.Transparent)
        )
        Spacer(Modifier.width(6.dp))
        Column {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) SheetGreen else Color(0xFF78909C),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                color = if (selected) SheetGreen else Color(0xFF546E7A)
            )
        }
    }
}

@Composable
private fun ReminderCheckboxRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp, color = Color(0xFF263238), fontWeight = FontWeight.Medium)
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = SheetGreen, uncheckedColor = Color(0xFFB0BEC5))
        )
    }
}

@Composable
private fun SortRadioRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp, color = Color(0xFF263238), fontWeight = FontWeight.Medium)
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = SheetGreen, unselectedColor = Color(0xFFB0BEC5))
        )
    }
}
