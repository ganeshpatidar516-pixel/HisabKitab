package com.ganesh.hisabkitabpro.domain.sync

data class SyncItem(

    val id: String,

    val type: String,

    val payload: String,

    val status: SyncStatus = SyncStatus.PENDING
)