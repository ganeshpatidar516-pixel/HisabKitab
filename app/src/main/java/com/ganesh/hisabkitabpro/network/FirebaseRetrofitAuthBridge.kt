package com.ganesh.hisabkitabpro.network

import com.ganesh.hisabkitabpro.domain.sync.SyncHealthMonitor
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Keeps [RetrofitClient] `Authorization: Bearer` aligned with the active Firebase session
 * so FastAPI / Railway endpoints that expect a Firebase ID token receive one.
 *
 * Does not touch Room, ledger, or sync logic — network header only.
 */
object FirebaseRetrofitAuthBridge {

    /** Suspending: use from coroutines (e.g. [com.ganesh.hisabkitabpro.auth.AuthRepository]). */
    suspend fun syncBearerFromFirebase(firebaseAuth: FirebaseAuth) {
        val user = firebaseAuth.currentUser
        if (user == null) {
            RetrofitClient.setToken("")
            return
        }
        val token = runCatching { user.getIdToken(false).await().token }.getOrNull()
        RetrofitClient.setToken(token.orEmpty())
        if (!token.isNullOrBlank()) {
            SyncHealthMonitor.clearWorkerPause()
        }
    }

    /**
     * Non-blocking: for Firebase `OnCompleteListener` / phone flows where we cannot suspend.
     * Clears header immediately if there is no current user.
     */
    fun enqueueBearerFromFirebase(firebaseAuth: FirebaseAuth) {
        val user = firebaseAuth.currentUser
        if (user == null) {
            RetrofitClient.setToken("")
            return
        }
        user.getIdToken(false).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result?.token.orEmpty()
                RetrofitClient.setToken(token)
                if (token.isNotBlank()) {
                    SyncHealthMonitor.clearWorkerPause()
                }
            } else {
                RetrofitClient.setToken("")
            }
        }
    }
}
