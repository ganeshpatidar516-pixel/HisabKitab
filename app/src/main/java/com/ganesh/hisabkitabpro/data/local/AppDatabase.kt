package com.ganesh.hisabkitabpro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SupportFactory
import net.sqlcipher.database.SQLiteDatabase
import com.ganesh.hisabkitabpro.data.repository.local.*
import com.ganesh.hisabkitabpro.data.repository.converters.*
import com.ganesh.hisabkitabpro.domain.model.*
import com.ganesh.hisabkitabpro.domain.sync.SyncItemEntity
import com.ganesh.hisabkitabpro.addon.audit.AuditLogDao
import com.ganesh.hisabkitabpro.addon.audit.AuditLogEntry
import com.ganesh.hisabkitabpro.security.KeyStoreCryptoManager

/**
 * HISABKITAB PRO ULTRA - 🏰 THE GLOBAL CONNECTIVITY ENGINE
 * Version 34: whatsappSentAt on transactions (bill delivery timeline)
 * Version 35: audit_log, settlementKind, reminder escalation tier
 * Version 36: app language preference in app_settings
 * Version 37: reminders.counterpartyKind + partyId (supplier party autopilot)
 * Version 38: Staff payroll suite — staff payroll fields + staff_attendance
 *             + staff_payroll_entry tables. Customer/Supplier ledger untouched.
 * Version 39: Inventory V5 — product barcode/SKU/tax/soft-delete fields +
 *             lookup indexes. Customer/Supplier ledger untouched.
 * Version 40: Profile intelligence layer — QR/logo/location/signature/social
 *             metadata only. Ledger/billing math untouched.
 * Version 41: Extra social / discovery URLs on business_profile only (nullable TEXT).
 * Version 42: Card identity copy on business_profile — tagline, servicesDescription, cardCtaText (additive).
 * Version 43: app_settings.ocrLiveAutoSaveEnabled (nullable INTEGER) — Wave 0 OCR safety flag; additive only.
 * Version 44: parties.city (nullable TEXT) — supplier search + prefs backfill; ledger math untouched.
 * Version 33: Unified Party System (Customer + Supplier)
 * Optimized for Million-User Scale with Zero-Lag Startup.
 */
@Database(
    entities = [
        Party::class,
        Customer::class,
        Supplier::class,
        SupplierTransaction::class,
        Transaction::class,
        Bill::class,
        TransactionHistory::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        ReminderEntity::class,
        ProductEntity::class,
        StaffEntity::class,
        StaffAttendanceEntity::class,
        StaffPayrollEntryEntity::class,
        AppSettings::class,
        BusinessProfile::class,
        SyncItemEntity::class,
        AuditLogEntry::class
    ],
    version = 44,
    exportSchema = true
)
@TypeConverters(
    StringListConverter::class, 
    TransactionTypeConverter::class, 
    SupplierTransactionTypeConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun partyDao(): PartyDao
    abstract fun customerDao(): com.ganesh.hisabkitabpro.data.repository.local.CustomerDao
    abstract fun supplierDao(): SupplierDao
    abstract fun supplierTransactionDao(): SupplierTransactionDao
    abstract fun transactionDao(): com.ganesh.hisabkitabpro.data.repository.local.TransactionDao
    abstract fun billDao(): BillDao
    abstract fun invoiceDao(): com.ganesh.hisabkitabpro.data.repository.local.InvoiceDao
    abstract fun reminderDao(): ReminderDao
    abstract fun productDao(): ProductDao
    abstract fun staffDao(): com.ganesh.hisabkitabpro.data.repository.local.StaffDao
    abstract fun staffAttendanceDao(): com.ganesh.hisabkitabpro.data.repository.local.StaffAttendanceDao
    abstract fun staffPayrollDao(): com.ganesh.hisabkitabpro.data.repository.local.StaffPayrollDao
    abstract fun settingsDao(): SettingsDao
    abstract fun businessProfileDao(): BusinessProfileDao
    abstract fun syncDao(): SyncDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        const val DATABASE_NAME = "hisabkitab_pro_blueprint_v33.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null
        private var libsLoaded = false

        private val MIGRATION_33_34 = object : Migration(33, 34) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN whatsappSentAt INTEGER")
            }
        }

        private val MIGRATION_34_35 = object : Migration(34, 35) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `audit_log` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`entityType` TEXT NOT NULL, " +
                        "`entityId` INTEGER NOT NULL, " +
                        "`action` TEXT NOT NULL, " +
                        "`detail` TEXT, " +
                        "`createdAt` INTEGER NOT NULL" +
                        ")"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_audit_log_entityType_entityId_createdAt` ON `audit_log` (`entityType`, `entityId`, `createdAt`)")
                db.execSQL("ALTER TABLE transactions ADD COLUMN settlementKind TEXT")
                db.execSQL("ALTER TABLE reminders ADD COLUMN lastEscalationTier INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_35_36 = object : Migration(35, 36) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN languageCode TEXT NOT NULL DEFAULT 'en'")
            }
        }

        private val MIGRATION_36_37 = object : Migration(36, 37) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE reminders ADD COLUMN counterpartyKind TEXT NOT NULL DEFAULT 'CUSTOMER'"
                )
                db.execSQL("ALTER TABLE reminders ADD COLUMN partyId INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_reminders_counterpartyKind_partyId_isSent_scheduledAt` " +
                        "ON `reminders` (`counterpartyKind`, `partyId`, `isSent`, `scheduledAt`)"
                )
            }
        }

        // v38 — Staff payroll suite. Strictly additive:
        //   - existing `staff` rows keep all their columns; new columns get safe defaults
        //   - two new tables (staff_attendance, staff_payroll_entry)
        //   - zero touches to bills / customers / suppliers / transactions
        private val MIGRATION_37_38 = object : Migration(37, 38) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE staff ADD COLUMN designation TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE staff ADD COLUMN salaryType TEXT NOT NULL DEFAULT 'MONTHLY'")
                db.execSQL("ALTER TABLE staff ADD COLUMN salaryAmountPaise INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE staff ADD COLUMN joiningDate INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE staff ADD COLUMN workdaysPerWeek INTEGER NOT NULL DEFAULT 6")
                db.execSQL("ALTER TABLE staff ADD COLUMN phoneEnc TEXT")
                db.execSQL("ALTER TABLE staff ADD COLUMN emailEnc TEXT")
                db.execSQL("ALTER TABLE staff ADD COLUMN address TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE staff ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE staff ADD COLUMN photoUri TEXT NOT NULL DEFAULT ''")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `staff_attendance` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`staffId` TEXT NOT NULL, " +
                        "`dateMillis` INTEGER NOT NULL, " +
                        "`status` TEXT NOT NULL, " +
                        "`note` TEXT NOT NULL DEFAULT '', " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL" +
                        ")"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_staff_attendance_staffId_dateMillis` " +
                        "ON `staff_attendance` (`staffId`, `dateMillis`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_staff_attendance_dateMillis` " +
                        "ON `staff_attendance` (`dateMillis`)"
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `staff_payroll_entry` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`staffId` TEXT NOT NULL, " +
                        "`kind` TEXT NOT NULL, " +
                        "`amountPaise` INTEGER NOT NULL, " +
                        "`note` TEXT NOT NULL DEFAULT '', " +
                        "`dateMillis` INTEGER NOT NULL, " +
                        "`cycleKey` TEXT NOT NULL DEFAULT '', " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "`isDeleted` INTEGER NOT NULL DEFAULT 0" +
                        ")"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_staff_payroll_entry_staffId_dateMillis` " +
                        "ON `staff_payroll_entry` (`staffId`, `dateMillis`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_staff_payroll_entry_staffId_kind_dateMillis` " +
                        "ON `staff_payroll_entry` (`staffId`, `kind`, `dateMillis`)"
                )
            }
        }

        // v39 — Inventory V5. Strictly additive:
        //   - legacy product rows remain valid with safe defaults
        //   - barcode/SKU lookup indexes are optional and sparse
        //   - customer/supplier/bill/transaction tables are untouched
        private val MIGRATION_38_39 = object : Migration(38, 39) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN barcode TEXT")
                db.execSQL("ALTER TABLE products ADD COLUMN sku TEXT")
                db.execSQL("ALTER TABLE products ADD COLUMN taxRatePercent REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE products ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE products ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE products ADD COLUMN lastStockSyncAt INTEGER NOT NULL DEFAULT 0")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_name` ON `products` (`name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_category` ON `products` (`category`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_products_barcode` ON `products` (`barcode`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_products_sku` ON `products` (`sku`)")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_products_isDeleted_name` " +
                        "ON `products` (`isDeleted`, `name`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_products_isDeleted_stockQuantity_minStockLevel` " +
                        "ON `products` (`isDeleted`, `stockQuantity`, `minStockLevel`)"
                )
            }
        }

        // v40 — Professional profile intelligence. Strictly additive:
        //   - only business_profile receives nullable/defaulted presentation fields
        //   - existing GST toggle and current database mapping remain untouched
        //   - customer/supplier/bill/payment/reminder tables are not altered
        private val MIGRATION_39_40 = object : Migration(39, 40) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE business_profile ADD COLUMN businessCategory TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE business_profile ADD COLUMN operatingHours TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE business_profile ADD COLUMN websiteUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE business_profile ADD COLUMN instagramUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE business_profile ADD COLUMN facebookUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE business_profile ADD COLUMN signatureImagePath TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE business_profile ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE business_profile ADD COLUMN longitude REAL")
                db.execSQL("ALTER TABLE business_profile ADD COLUMN locationLockedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE business_profile ADD COLUMN mapLink TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_40_41 = object : Migration(40, 41) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE business_profile ADD COLUMN linkedInUrl TEXT")
                db.execSQL("ALTER TABLE business_profile ADD COLUMN youtubeUrl TEXT")
                db.execSQL("ALTER TABLE business_profile ADD COLUMN twitterUrl TEXT")
                db.execSQL("ALTER TABLE business_profile ADD COLUMN whatsAppBusinessUrl TEXT")
                db.execSQL("ALTER TABLE business_profile ADD COLUMN googleBusinessProfileUrl TEXT")
            }
        }

        private val MIGRATION_41_42 = object : Migration(41, 42) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Nullable TEXT so older API payloads / Gson omitting keys stay compatible.
                db.execSQL("ALTER TABLE business_profile ADD COLUMN tagline TEXT")
                db.execSQL("ALTER TABLE business_profile ADD COLUMN servicesDescription TEXT")
                db.execSQL("ALTER TABLE business_profile ADD COLUMN cardCtaText TEXT")
            }
        }

        private val MIGRATION_42_43 = object : Migration(42, 43) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE app_settings ADD COLUMN ocrLiveAutoSaveEnabled INTEGER"
                )
            }
        }

        private val MIGRATION_43_44 = object : Migration(43, 44) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE parties ADD COLUMN city TEXT")
            }
        }

        internal val APP_MIGRATION_33_34: Migration = MIGRATION_33_34
        internal val APP_MIGRATION_34_35: Migration = MIGRATION_34_35
        internal val APP_MIGRATION_35_36: Migration = MIGRATION_35_36
        internal val APP_MIGRATION_36_37: Migration = MIGRATION_36_37
        internal val APP_MIGRATION_37_38: Migration = MIGRATION_37_38
        internal val APP_MIGRATION_38_39: Migration = MIGRATION_38_39
        internal val APP_MIGRATION_39_40: Migration = MIGRATION_39_40
        internal val APP_MIGRATION_40_41: Migration = MIGRATION_40_41
        internal val APP_MIGRATION_41_42: Migration = MIGRATION_41_42
        internal val APP_MIGRATION_42_43: Migration = MIGRATION_42_43
        internal val APP_MIGRATION_43_44: Migration = MIGRATION_43_44

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = INSTANCE
                if (instance != null) return instance

                // ✅ PERFORMANCE: loadLibs is heavy, ensure it runs only once.
                if (!libsLoaded) {
                    SQLiteDatabase.loadLibs(context)
                    libsLoaded = true
                }
                
                val passphrase = SQLiteDatabase.getBytes(
                    KeyStoreCryptoManager.getOrCreateDatabasePassphrase(context)
                )
                val factory = SupportFactory(passphrase)

                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .openHelperFactory(factory)
                .addMigrations(
                    MIGRATION_33_34,
                    MIGRATION_34_35,
                    MIGRATION_35_36,
                    MIGRATION_36_37,
                    MIGRATION_37_38,
                    MIGRATION_38_39,
                    MIGRATION_39_40,
                    MIGRATION_40_41,
                    MIGRATION_41_42,
                    MIGRATION_42_43,
                    MIGRATION_43_44
                )
                .build()
                
                INSTANCE = db
                db
            }
        }
    }
}
