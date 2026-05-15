package com.ganesh.hisabkitabpro.auth.phone

/**
 * Outcome of starting Firebase phone verification so the UI knows whether to
 * show the OTP field or treat the session as already completed (instant verify).
 */
sealed interface PhoneVerificationStartResult {
    /** SMS path — user should enter the code. */
    data object AwaitingSmsCode : PhoneVerificationStartResult

    /** Instant verification / auto-retrieval — no OTP UI. */
    data object CompletedWithoutSmsUi : PhoneVerificationStartResult

    data class Failed(val message: String) : PhoneVerificationStartResult
}
