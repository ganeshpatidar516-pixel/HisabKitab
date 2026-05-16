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
    const val ScanBill = "scan_bill/{customerId}?prefillOnly={prefillOnly}"

    const val SupplierLedger = "supplier_ledger/{supplierId}"
    const val SupplierStatement = "supplier_statement/{supplierId}"
    const val SupplierPartyReminderControl = "supplier_party_reminder_control/{partyId}/{partyName}"
    const val SupplierPartyReminderHistory = "supplier_party_reminder_history/{partyId}/{partyName}"
    const val ScanSupplierBill = "scan_supplier_bill/{supplierId}"

    const val StaffDetail = "staff_detail/{staffId}"
    const val StaffEdit = "staff_edit/{staffId}"
    const val StaffAttendance = "staff_attendance/{staffId}"

    fun customerLedger(customerId: Long) = "customer_ledger/$customerId"
    fun customerProfile(customerId: Long) = "customer_profile/$customerId"
    fun editCustomer(customerId: Long) = "edit_customer/$customerId"
    fun customerStatement(customerId: Long) = "customer_statement/$customerId"
    fun customerReminderControl(customerId: Long, encodedName: String) =
        "customer_reminder_control/$customerId/$encodedName"
    fun customerReminderHistory(customerId: Long, encodedName: String) =
        "customer_reminder_history/$customerId/$encodedName"
    fun addTransaction(customerId: Long, encodedName: String, type: String) =
        "add_transaction/$customerId/$encodedName/$type"
    fun fullBill(customerId: Long, encodedName: String, type: String) =
        "full_bill/$customerId/$encodedName/$type"

    fun supplierLedger(supplierId: Long) = "supplier_ledger/$supplierId"
    fun supplierStatement(supplierId: Long) = "supplier_statement/$supplierId"
    fun supplierPartyReminderControl(partyId: Long, encodedName: String) =
        "supplier_party_reminder_control/$partyId/$encodedName"
    fun supplierPartyReminderHistory(partyId: Long, encodedName: String) =
        "supplier_party_reminder_history/$partyId/$encodedName"

    fun staffDetail(staffId: String) = "staff_detail/$staffId"
    fun staffEdit(staffId: String) = "staff_edit/$staffId"
    fun staffAttendance(staffId: String) = "staff_attendance/$staffId"

    fun scanBill(customerId: Long = 0L, prefillOnly: Boolean = true) =
        if (prefillOnly) "scan_bill/$customerId" else "scan_bill/$customerId?prefillOnly=false"
    fun scanSupplierBill(supplierId: Long) = "scan_supplier_bill/$supplierId"
}
