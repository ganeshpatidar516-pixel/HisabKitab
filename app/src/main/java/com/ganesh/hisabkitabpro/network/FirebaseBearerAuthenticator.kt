package com.ganesh.hisabkitabpro.network

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.TimeUnit

/**
 * OkHttp [Authenticator] — on 401, force-refresh Firebase ID token and retry once.
 * Does not touch Room, ledger, or sync queue.
 */
object FirebaseBearerAuthenticator : Authenticator {

    private const val TAG = "FirebaseBearerAuth"
    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.code != 401) return null
        if (response.priorResponse != null) return null

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            RetrofitClient.setToken("")
            return null
        }

        synchronized(refreshLock) {
            val newToken = runCatching {
                Tasks.await(user.getIdToken(true), 15, TimeUnit.SECONDS).token
            }.getOrNull()?.takeIf { it.isNotBlank() }

            if (newToken == null) {
                Log.w(TAG, "401 but Firebase token refresh failed")
                return null
            }

            RetrofitClient.setToken(newToken)
            return response.request.newBuilder()
                .header("Authorization", "Bearer $newToken")
                .build()
        }
    }
}
