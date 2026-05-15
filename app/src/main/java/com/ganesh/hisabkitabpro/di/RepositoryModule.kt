package com.ganesh.hisabkitabpro.di

import com.ganesh.hisabkitabpro.data.repository.ActionRepositoryImpl
import com.ganesh.hisabkitabpro.data.repository.CustomerRepositoryImpl
import com.ganesh.hisabkitabpro.data.repository.InvoiceRepositoryImpl
import com.ganesh.hisabkitabpro.data.repository.PartyRepositoryImpl
import com.ganesh.hisabkitabpro.data.repository.SettingsRepositoryImpl
import com.ganesh.hisabkitabpro.data.repository.SupplierRepositoryImpl
import com.ganesh.hisabkitabpro.data.repository.TransactionRepositoryImpl
import com.ganesh.hisabkitabpro.domain.businesscard.repository.BusinessCardRepository
import com.ganesh.hisabkitabpro.domain.businesscard.repository.BusinessCardRepositoryImpl
import com.ganesh.hisabkitabpro.domain.repository.ActionRepository
import com.ganesh.hisabkitabpro.domain.repository.CustomerRepository
import com.ganesh.hisabkitabpro.domain.repository.InvoiceRepository
import com.ganesh.hisabkitabpro.domain.repository.PartyRepository
import com.ganesh.hisabkitabpro.domain.repository.SettingsRepository
import com.ganesh.hisabkitabpro.domain.repository.SupplierRepository
import com.ganesh.hisabkitabpro.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Tier-B: repository interface bindings (implementations stay in `data.repository`). */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindCustomerRepository(impl: CustomerRepositoryImpl): CustomerRepository

    @Binds
    @Singleton
    abstract fun bindSupplierRepository(impl: SupplierRepositoryImpl): SupplierRepository

    @Binds
    @Singleton
    abstract fun bindPartyRepository(impl: PartyRepositoryImpl): PartyRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindBusinessCardRepository(impl: BusinessCardRepositoryImpl): BusinessCardRepository

    @Binds
    @Singleton
    abstract fun bindInvoiceRepository(impl: InvoiceRepositoryImpl): InvoiceRepository

    @Binds
    @Singleton
    abstract fun bindActionRepository(impl: ActionRepositoryImpl): ActionRepository
}
