package com.ganesh.hisabkitabpro.network.api

import com.ganesh.hisabkitabpro.domain.model.Reminder
import retrofit2.http.*

interface ReminderApi {

    @GET("reminders")
    suspend fun getReminders(): List<Reminder>

    @POST("reminders")
    suspend fun addReminder(
        @Body reminder: Reminder
    ): Reminder

    @DELETE("reminders/{reminder_id}")
    suspend fun deleteReminder(
        @Path("reminder_id") reminderId: Int
    ): Unit
}