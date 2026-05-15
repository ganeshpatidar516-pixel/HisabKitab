package com.ganesh.hisabkitabpro.ui.customers

import android.content.Context
import com.ganesh.hisabkitabpro.domain.customers.CustomerListMenuTab
import com.ganesh.hisabkitabpro.domain.customers.CustomerListReminderSegment
import com.ganesh.hisabkitabpro.domain.customers.CustomerListSortOption

data class CustomerListFilterSettings(
    val menuTab: CustomerListMenuTab,
    val sortOption: CustomerListSortOption,
    val reminderSegments: Set<CustomerListReminderSegment>
)

object CustomerListFilterPrefs {
    private const val PREFS = "customer_list_filter_prefs"
    private const val KEY_TAB = "menu_tab"
    private const val KEY_SORT = "sort_option"
    private const val KEY_REMINDER_SEGMENTS = "reminder_segments"

    fun load(context: Context): CustomerListFilterSettings {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val tab = when (p.getString(KEY_TAB, null)?.lowercase()) {
            "reminder_date" -> CustomerListMenuTab.REMINDER_DATE
            else -> CustomerListMenuTab.SORT_BY
        }
        val sort = when (p.getString(KEY_SORT, null)?.lowercase()) {
            "last_payment" -> CustomerListSortOption.LAST_PAYMENT
            "latest_activity" -> CustomerListSortOption.LATEST_ACTIVITY
            "due_amount" -> CustomerListSortOption.DUE_AMOUNT
            "name" -> CustomerListSortOption.NAME
            "defaulters" -> CustomerListSortOption.DEFAULTERS
            else -> CustomerListSortOption.DEFAULT
        }
        val raw = p.getString(KEY_REMINDER_SEGMENTS, null)?.lowercase()?.trim().orEmpty()
        val segments = if (raw.isEmpty()) {
            emptySet()
        } else {
            raw.split(',')
                .map { it.trim() }
                .mapNotNull { token ->
                    when (token) {
                        "today" -> CustomerListReminderSegment.TODAY
                        "pending" -> CustomerListReminderSegment.PENDING
                        "upcoming" -> CustomerListReminderSegment.UPCOMING
                        else -> null
                    }
                }
                .toSet()
        }
        return CustomerListFilterSettings(tab, sort, segments)
    }

    fun save(context: Context, settings: CustomerListFilterSettings) {
        val tabStr = when (settings.menuTab) {
            CustomerListMenuTab.SORT_BY -> "sort_by"
            CustomerListMenuTab.REMINDER_DATE -> "reminder_date"
        }
        val sortStr = when (settings.sortOption) {
            CustomerListSortOption.DEFAULT -> "default"
            CustomerListSortOption.LAST_PAYMENT -> "last_payment"
            CustomerListSortOption.LATEST_ACTIVITY -> "latest_activity"
            CustomerListSortOption.DUE_AMOUNT -> "due_amount"
            CustomerListSortOption.NAME -> "name"
            CustomerListSortOption.DEFAULTERS -> "defaulters"
        }
        val reminderStr = settings.reminderSegments
            .map {
                when (it) {
                    CustomerListReminderSegment.TODAY -> "today"
                    CustomerListReminderSegment.PENDING -> "pending"
                    CustomerListReminderSegment.UPCOMING -> "upcoming"
                }
            }
            .sorted()
            .joinToString(",")

        val ed = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_TAB, tabStr)
            .putString(KEY_SORT, sortStr)
        if (reminderStr.isEmpty()) ed.remove(KEY_REMINDER_SEGMENTS) else ed.putString(KEY_REMINDER_SEGMENTS, reminderStr)
        ed.apply()
    }
}
