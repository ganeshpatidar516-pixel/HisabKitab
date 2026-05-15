package com.ganesh.hisabkitabpro.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val userId: String = "default_user",
    val theme: String = "amoled_gold",
    val voiceAssistantEnabled: Boolean = false,
    val smartSuggestionsEnabled: Boolean = false,
    val cloudBackupEnabled: Boolean = false,
    val securityPinHash: String? = null,
    val currency: String = "₹",
    val gstEnabled: Boolean = false,
    val gstRate: Double = 18.0,
    val invoiceTemplateId: String = "template_1",
    val languageCode: String = "en", // en, hi, system
    val advancedSettingsJson: String = "{}", // To store flexible fields
    /**
     * When not [false], camera live-frame auto-save from customer ledger OCR may run (if navigation enables it).
     * Nullable so Gson/Room upgrades default to “on” when the key is absent.
     */
    @ColumnInfo(name = "ocrLiveAutoSaveEnabled")
    val ocrLiveAutoSaveEnabled: Boolean? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
