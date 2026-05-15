package com.ganesh.hisabkitabpro.core.locale

data class LanguageItem(
    val code: String,
    val label: String
)

object IndianLanguageCatalog {
    val supportedLanguages: List<LanguageItem> = listOf(
        LanguageItem("system", "System Default"),
        LanguageItem("en", "English"),
        LanguageItem("as", "Assamese (অসমীয়া)"),
        LanguageItem("bn", "Bengali (বাংলা)"),
        LanguageItem("brx", "Bodo (बर')"),
        LanguageItem("doi", "Dogri (डोगरी)"),
        LanguageItem("gu", "Gujarati (ગુજરાતી)"),
        LanguageItem("hi", "Hindi (हिन्दी)"),
        LanguageItem("kn", "Kannada (ಕನ್ನಡ)"),
        LanguageItem("ks", "Kashmiri (कॉशुर)"),
        LanguageItem("kok", "Konkani (कोंकणी)"),
        LanguageItem("mai", "Maithili (मैथिली)"),
        LanguageItem("ml", "Malayalam (മലയാളം)"),
        LanguageItem("mni", "Manipuri (মৈতৈলোন)"),
        LanguageItem("mr", "Marathi (मराठी)"),
        LanguageItem("ne", "Nepali (नेपाली)"),
        LanguageItem("or", "Odia (ଓଡ଼ିଆ)"),
        LanguageItem("pa", "Punjabi (ਪੰਜਾਬੀ)"),
        LanguageItem("sa", "Sanskrit (संस्कृत)"),
        LanguageItem("sat", "Santali (ᱥᱟᱱᱛᱟᱲᱤ)"),
        LanguageItem("sd", "Sindhi (سنڌي)"),
        LanguageItem("ta", "Tamil (தமிழ்)"),
        LanguageItem("te", "Telugu (తెలుగు)"),
        LanguageItem("ur", "Urdu (اردو)")
    )

    val supportedLanguageCodes: Set<String> = supportedLanguages.map { it.code }.toSet()
}
