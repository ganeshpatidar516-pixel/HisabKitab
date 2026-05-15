package com.ganesh.hisabkitabpro.ui.navigation

import android.net.Uri

/**
 * Typed navigation routes for the main [hisabAppNavGraph].
 * Route strings stay stable for deep links and instrumented tests.
 */
object AppRoutes {
    const val Dashboard = "dashboard"
    const val Customers = "customers"
    const val Suppliers = "suppliers"
    const val AiAssistant = "ai_assistant"
    const val Settings = "settings"
    const val SettingsBilling = "settings_billing"
    const val SettingsCloud = "settings_cloud"
    const val PrivacyAndData = "privacy_and_data"
    const val StaffList = "staff_list"
    const val StaffAdd = "staff_add"
    const val Inventory = "inventory"
    const val HelpSupport = "help_support"
    const val PrivacyPolicy = "privacy_policy"
    const val SecurityPin = "security_pin"
    const val BusinessProfile = "business_profile"
    const val VisitingCardStudio = "visiting_card_studio"
    const val BusinessInsights = "business_insights"
    const val AddCustomer = "add_customer"
    const val AddSupplier = "add_supplier"
    const val BulkReminderSchedule = "bulk_reminder_schedule"
    const val HtmlBrandTemplates = "html_brand_templates"

    const val CustomerLedger = "customer_ledger/{customerId}"
    const val CustomerProfile = "customer_profile/{customerId}"
    const val EditCustomer = "edit_customer/{customerId}"
    const val CustomerStatement = "customer_statement/{customerId}"
    const val CustomerReminderControl = "customer_reminder_control/{customerId}/{customerName}"
    const val CustomerReminderHistory = "customer_reminder_history/{customerId}/{customerName}"
    const val AddTransaction = "add_transaction/{customerId}/{customerName}/{type}"
    const val FullBill = "full_bill/{customerId}/{customerName}/{type}"
    const val ScanBill = "scan_bill/{customerId}"

    const val SupplierLedger = "supplier_ledger/{supplierId}"
    const val SupplierStatement = "supplier_statement/{supplierId}"
    const val SupplierPartyReminderControl = "supplier_party_reminder_control/{partyId}/{partyName}"
    const val SupplierPartyReminderHistory = "supplier_party_reminder_history/{partyId}/{partyName}"
    const val ScanSupplierBill = "scan_supplier_bill/{supplierId}"

    const val StaffDetail = "staff_detail/{staffId}"
    const val StaffEdit = "staff_edit/{staffId}"
    const val StaffAttendance = "staff_attendance/{staffId}"

    fun customerLedger(customerId: Long) = "customer_ledger/$customerId"
    fun supplierLedger(supplierId: Long) = "supplier_ledger/$supplierId"
    fun scanBill(customerId: Long = 0L) = "scan_bill/$customerId"
    fun scanSupplierBill(supplierId: Long) = "scan_supplier_bill/$supplierId"
}
