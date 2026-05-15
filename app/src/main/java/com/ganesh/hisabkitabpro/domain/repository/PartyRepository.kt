package com.ganesh.hisabkitabpro.domain.repository

import androidx.paging.PagingData
import com.ganesh.hisabkitabpro.domain.model.Party
import kotlinx.coroutines.flow.Flow

/**
 * HISABKITAB PRO ULTRA - PARTY REPOSITORY
 */
interface PartyRepository {
    /** P0 — live SUM of active supplier balances (paise); not limited to a paging window. */
    fun observeActiveSuppliersTotalBalancePaise(): Flow<Long>

    fun getPartiesPaged(isSupplier: Boolean, query: String?): Flow<PagingData<Party>>
    fun getParties(isSupplier: Boolean): Flow<List<Party>>
    suspend fun addParty(party: Party): Long
    suspend fun updateParty(party: Party)
    suspend fun deleteParty(id: Long)
    suspend fun getPartyById(id: Long): Party?
}
