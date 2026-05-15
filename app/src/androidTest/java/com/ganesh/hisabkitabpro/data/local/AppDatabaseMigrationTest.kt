package com.ganesh.hisabkitabpro.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration safety lock for production builds.
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate36To37_keepsSchemaValid() {
        val dbName = "migration-test-db-36-37"

        helper.createDatabase(dbName, 36).close()

        helper.runMigrationsAndValidate(
            dbName,
            37,
            true,
            AppDatabase.APP_MIGRATION_36_37
        )
    }

    @Test
    fun migrate33To37_keepsSchemaValid() {
        val dbName = "migration-test-db-33-37"

        helper.createDatabase(dbName, 33).close()

        helper.runMigrationsAndValidate(
            dbName,
            37,
            true,
            AppDatabase.APP_MIGRATION_33_34,
            AppDatabase.APP_MIGRATION_34_35,
            AppDatabase.APP_MIGRATION_35_36,
            AppDatabase.APP_MIGRATION_36_37
        )
    }

    @Test
    fun migrate41To42_addsCardCopyColumns() {
        val dbName = "migration-test-db-41-42"
        helper.createDatabase(dbName, 41).close()
        helper.runMigrationsAndValidate(
            dbName,
            42,
            true,
            AppDatabase.APP_MIGRATION_41_42,
        )
    }
}
