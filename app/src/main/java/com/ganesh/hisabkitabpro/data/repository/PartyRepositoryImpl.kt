package com.ganesh.hisabkitabpro.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.ganesh.hisabkitabpro.data.repository.local.PartyDao
import com.ganesh.hisabkitabpro.domain.model.Party
import com.ganesh.hisabkitabpro.domain.repository.PartyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * HISABKITAB PRO ULTRA - PARTY REPOSITORY IMPLEMENTATION
 */
class PartyRepositoryImpl @Inject constructor(
    private val partyDao: PartyDao
) : PartyRepository {

    override fun observeActiveSuppliersTotalBalancePaise(): Flow<Long> =
        partyDao.observeActiveSuppliersTotalBalancePaise().map { it.coerceAtLeast(0L) }

    override fun getPartiesPaged(isSupplier: Boolean, query: String?): Flow<PagingData<Party>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                if (query.isNullOrBlank()) partyDao.getPartiesPagingSource(isSupplier)
                else partyDao.searchPartiesPagingSource(isSupplier, query)
            }
        ).flow
    }

    override fun getParties(isSupplier: Boolean): Flow<List<Party>> {
        return partyDao.getAllParties(isSupplier)
    }

    override suspend fun addParty(party: Party): Long {
        return partyDao.insertParty(party)
    }

    override suspend fun updateParty(party: Party) {
        partyDao.updateParty(party)
    }

    override suspend fun deleteParty(id: Long) {
        partyDao.softDeleteParty(id)
    }

    override suspend fun getPartyById(id: Long): Party? {
        return partyDao.getPartyById(id)
    }
}
