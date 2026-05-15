package com.ganesh.hisabkitabpro.security

import android.util.Log

object AuditLogger {
    /**
     * [Block 8: Security Layer] - महत्वपूर्ण गतिविधियों को लॉग करता है।
     */
    fun logAction(userId: String, action: String, details: String) {
        val timestamp = System.currentTimeMillis()
        val logEntry = "AUDIT_LOG | Action: $action | Details(redacted, len=${details.length}) | Time: $timestamp"
        
        // अभी के लिए Logcat में, भविष्य में इसे सर्वर/एन्क्रिप्टेड फाइल में भेजा जा सकता है
        Log.i("HisabKitab_Audit", logEntry)
    }
}
