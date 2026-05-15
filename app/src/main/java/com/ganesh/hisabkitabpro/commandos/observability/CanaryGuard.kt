package com.ganesh.hisabkitabpro.commandos.observability

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CanaryGuard @Inject constructor() {
    fun shouldAutoDisable(snapshot: CommandSloSnapshot): Boolean {
        if (snapshot.total < 20) return false
        return snapshot.failureRate >= 0.35
    }
}
