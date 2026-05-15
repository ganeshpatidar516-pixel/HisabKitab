package com.ganesh.hisabkitabpro.domain.customers

enum class CustomerListMenuTab {
    SORT_BY,
    REMINDER_DATE
}

enum class CustomerListSortOption {
    DEFAULT,
    LAST_PAYMENT,
    LATEST_ACTIVITY,
    DUE_AMOUNT,
    NAME,
    DEFAULTERS
}

/** Reminder-date filter segments (multi-select). Compared using device-local calendar day on [nextReminderDate]. */
enum class CustomerListReminderSegment {
    /** Reminder falls on today's local date. */
    TODAY,

    /** Reminder date is before today (overdue). */
    PENDING,

    /** Reminder date is after today. */
    UPCOMING
}
