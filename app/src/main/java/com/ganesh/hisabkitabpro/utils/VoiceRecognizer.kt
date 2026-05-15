package com.ganesh.hisabkitabpro.utils

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import java.util.*

/**
 * LEGACY / DEAD-PAIRED — DO NOT USE FOR NEW CODE.
 *
 * Has zero callers in `app/src/main`. Confirmed stripped from release AABs by R8
 * (see `app/build/outputs/mapping/release/usage.txt:49802`).
 *
 * Uses the deprecated `Activity.startActivityForResult` API and an in-app activity
 * result code. The LIVE voice flow uses the modern Activity Result Contracts API
 * directly inside the AI chat / voice-command screens, which is lifecycle-safe
 * and Compose-idiomatic.
 */
@Deprecated(
    message = "Uses legacy startActivityForResult API. Live voice flow uses Activity Result Contracts in ui/ai/*.",
    level = DeprecationLevel.WARNING
)
object VoiceRecognizer {

    fun startVoiceInput(activity: Activity) {

        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        )

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            Locale.getDefault()
        )

        activity.startActivityForResult(intent, 101)
    }
}