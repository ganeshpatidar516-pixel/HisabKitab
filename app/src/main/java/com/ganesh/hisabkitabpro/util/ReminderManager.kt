package com.ganesh.hisabkitabpro.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ganesh.hisabkitabpro.receiver.ReminderBroadcastReceiver

object ReminderManager {
    /**
     * HISABKITAB PRO - SMART SCHEDULING ENGINE
     * Uses inexact-safe scheduling for better Play policy compatibility.
     */
    fun scheduleReminder(
        context: Context,
        customerName: String,
        amount: String,
        timeInMillis: Long,
        requestCode: Int = customerName.hashCode()
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("CUSTOMER_NAME", customerName)
            putExtra("AMOUNT", amount)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            }
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        }
    }
}
