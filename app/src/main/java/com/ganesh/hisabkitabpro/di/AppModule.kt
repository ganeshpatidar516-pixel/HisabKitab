package com.ganesh.hisabkitabpro.di

import android.content.Context
import android.content.SharedPreferences
import com.ganesh.hisabkitabpro.commandos.SuperCommandFeatureToggle
import com.ganesh.hisabkitabpro.feature.banksettle.BankAutoSettleFeatureToggle
import com.ganesh.hisabkitabpro.feature.sharedkhata.SharedKhataFeatureToggle
import com.ganesh.hisabkitabpro.feature.telemetry.TelemetryFeatureToggle
import com.ganesh.hisabkitabpro.commandos.adapters.hisabkitab.HisabKitabDomainAdapter
import com.ganesh.hisabkitabpro.commandos.dialect.DialectRegistry
import com.ganesh.hisabkitabpro.commandos.intent.DeterministicIntentParser
import com.ganesh.hisabkitabpro.commandos.normalize.InputNormalizer
import com.ganesh.hisabkitabpro.commandos.orchestrator.SuperCommandOrchestrator
import com.ganesh.hisabkitabpro.commandos.observability.CanaryGuard
import com.ganesh.hisabkitabpro.commandos.observability.CommandSloTracker
import com.ganesh.hisabkitabpro.commandos.policy.PolicyGuard
import com.ganesh.hisabkitabpro.commandos.sync.OfflineCommandJournal
import com.ganesh.hisabkitabpro.commandos.sync.PersistentOfflineCommandJournal
import com.ganesh.hisabkitabpro.commandos.sync.QueueHealthMetrics
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.data.local.ReminderDao
import com.ganesh.hisabkitabpro.data.local.ProductDao
import com.ganesh.hisabkitabpro.data.repository.local.*
import com.ganesh.hisabkitabpro.addon.audit.AuditLogRecorder
import com.ganesh.hisabkitabpro.data.repository.*
import com.ganesh.hisabkitabpro.domain.repository.*
import com.ganesh.hisabkitabpro.domain.backup.CloudBackupManager
import com.ganesh.hisabkitabpro.domain.businesscard.repository.BusinessCardRepository
import com.ganesh.hisabkitabpro.domain.businesscard.repository.BusinessCardRepositoryImpl
import com.ganesh.hisabkitabpro.domain.cloud.SelectiveCloudMirror
import com.ganesh.hisabkitabpro.network.api.*
import com.ganesh.hisabkitabpro.network.RetrofitClient
import com.ganesh.hisabkitabpro.security.SecurityManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideSettingsDao(db: AppDatabase): SettingsDao = db.settingsDao()

    @Provides
    fun provideBusinessProfileDao(db: AppDatabase): BusinessProfileDao = db.businessProfileDao()

    @Provides
    fun provideCustomerDao(db: AppDatabase): CustomerDao = db.customerDao()

    @Provides
    fun provideSupplierDao(db: AppDatabase): SupplierDao = db.supplierDao()

    @Provides
    fun providePartyDao(db: AppDatabase): PartyDao = db.partyDao()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideBillDao(db: AppDatabase): BillDao = db.billDao()

    @Provides
    fun provideSyncDao(db: AppDatabase): SyncDao = db.syncDao()

    @Provides
    fun provideInvoiceDao(db: AppDatabase): InvoiceDao = db.invoiceDao()

    @Provides
    fun provideReminderDao(db: AppDatabase): ReminderDao = db.reminderDao()

    @Provides
    fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()

    @Provides
    fun provideStaffDao(db: AppDatabase): StaffDao = db.staffDao()

    @Provides
    fun provideStaffAttendanceDao(db: AppDatabase): StaffAttendanceDao = db.staffAttendanceDao()

    @Provides
    fun provideStaffPayrollDao(db: AppDatabase): StaffPayrollDao = db.staffPayrollDao()

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("hisabkitab_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        }
    }

    @Provides
    @Singleton
    fun provideSecurityManager(@ApplicationContext context: Context, prefs: SharedPreferences): SecurityManager {
        return SecurityManager(context, prefs)
    }

    @Provides
    @Singleton
    fun provideSuperCommandFeatureToggle(
        prefs: SharedPreferences
    ): SuperCommandFeatureToggle {
        return SuperCommandFeatureToggle(prefs)
    }

    @Provides
    @Singleton
    fun provideSharedKhataFeatureToggle(
        prefs: SharedPreferences
    ): SharedKhataFeatureToggle {
        return SharedKhataFeatureToggle(prefs)
    }

    @Provides
    @Singleton
    fun provideBankAutoSettleFeatureToggle(
        prefs: SharedPreferences
    ): BankAutoSettleFeatureToggle {
        return BankAutoSettleFeatureToggle(prefs)
    }

    @Provides
    @Singleton
    fun provideTelemetryFeatureToggle(
        prefs: SharedPreferences
    ): TelemetryFeatureToggle {
        return TelemetryFeatureToggle(prefs)
    }

    @Provides
    @Singleton
    fun provideOfflineCommandJournal(
        prefs: SharedPreferences
    ): OfflineCommandJournal {
        return PersistentOfflineCommandJournal(prefs)
    }

    @Provides
    @Singleton
    fun provideQueueHealthMetrics(
        prefs: SharedPreferences
    ): QueueHealthMetrics {
        return QueueHealthMetrics(prefs)
    }

    @Provides
    @Singleton
    fun provideCommandSloTracker(
        prefs: SharedPreferences
    ): CommandSloTracker {
        return CommandSloTracker(prefs)
    }

    @Provides
    @Singleton
    fun provideCanaryGuard(): CanaryGuard {
        return CanaryGuard()
    }

    @Provides
    @Singleton
    fun provideSuperCommandOrchestrator(
        adapter: HisabKitabDomainAdapter
    ): SuperCommandOrchestrator {
        return SuperCommandOrchestrator(
            normalizer = InputNormalizer(),
            dialectRegistry = DialectRegistry(),
            parser = DeterministicIntentParser(),
            policyGuard = PolicyGuard(),
            adapter = adapter
        )
    }

    @Provides
    @Singleton
    fun provideSettingsApi(): SettingsApi {
        return RetrofitClient.retrofit.create(SettingsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideCustomerApi(): CustomerApi {
        return RetrofitClient.retrofit.create(CustomerApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTransactionApi(): TransactionApi {
        return RetrofitClient.retrofit.create(TransactionApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAiAssistantApi(): AiAssistantApi {
        return RetrofitClient.retrofit.create(AiAssistantApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDeviceAttestationApi(): DeviceAttestationApi {
        return RetrofitClient.retrofit.create(DeviceAttestationApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        settingsDao: SettingsDao,
        businessProfileDao: BusinessProfileDao,
        settingsApi: SettingsApi,
        selectiveCloudMirror: SelectiveCloudMirror
    ): SettingsRepository {
        return SettingsRepositoryImpl(settingsDao, businessProfileDao, settingsApi, selectiveCloudMirror)
    }

    @Provides
    @Singleton
    fun provideCustomerRepository(
        customerDao: Lazy<CustomerDao>,
        selectiveCloudMirror: SelectiveCloudMirror
    ): CustomerRepository {
        return CustomerRepositoryImpl(customerDao, selectiveCloudMirror)
    }

    @Provides
    @Singleton
    fun provideSupplierRepository(
        supplierDao: Lazy<SupplierDao>
    ): SupplierRepository {
        return SupplierRepositoryImpl(supplierDao)
    }

    @Provides
    @Singleton
    fun providePartyRepository(
        partyDao: PartyDao
    ): PartyRepository {
        return PartyRepositoryImpl(partyDao)
    }

    @Provides
    @Singleton
    fun provideTransactionRepository(
        database: Lazy<AppDatabase>,
        transactionDao: Lazy<TransactionDao>,
        billDao: Lazy<BillDao>,
        customerDao: Lazy<CustomerDao>,
        cloudBackupManager: CloudBackupManager,
        auditLogRecorder: AuditLogRecorder,
        selectiveCloudMirror: SelectiveCloudMirror,
        @ApplicationContext context: Context
    ): TransactionRepository {
        return TransactionRepositoryImpl(
            database,
            transactionDao,
            billDao,
            customerDao,
            cloudBackupManager,
            auditLogRecorder,
            selectiveCloudMirror,
            context
        )
    }

    @Provides
    @Singleton
    fun provideBusinessCardRepository(
        settingsRepository: SettingsRepository
    ): BusinessCardRepository {
        return BusinessCardRepositoryImpl(settingsRepository)
    }

    @Provides
    @Singleton
    fun provideInvoiceRepository(
        invoiceDao: InvoiceDao,
        transactionDao: TransactionDao,
        transactionRepository: TransactionRepository,
        customerRepository: CustomerRepository,
        @ApplicationContext context: Context,
        appDatabase: Lazy<AppDatabase>
    ): InvoiceRepository {
        return InvoiceRepositoryImpl(
            invoiceDao,
            transactionDao,
            transactionRepository,
            customerRepository,
            context,
            appDatabase
        )
    }
}
