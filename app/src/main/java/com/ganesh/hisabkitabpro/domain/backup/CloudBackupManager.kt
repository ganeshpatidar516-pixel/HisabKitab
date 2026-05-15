package com.ganesh.hisabkitabpro.domain.backup

import android.content.Context
import com.ganesh.hisabkitabpro.core.storage.StorageSpaceGuard
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudBackupManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Explicit human confirmation phrase required for destructive restore apply.
        const val RESTORE_CONFIRMATION_PHRASE = "RESTORE_MY_DATA"
    }

    data class BackupValidationReport(
        val valid: Boolean,
        val filePath: String,
        val sizeBytes: Long,
        val checksumSha256: String,
        val checksumMatchesSidecar: Boolean
    )

    suspend fun backupDatabaseToDrive(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!StorageSpaceGuard.hasMinFreeSpace(context, minFreeMb = 64L)) {
                return@withContext Result.failure(Exception("Not enough free storage for backup"))
            }
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.failure(Exception("User not signed in to Google"))

            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA)
            ).apply {
                selectedAccount = account.account
            }

            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory(),
                credential
            ).setApplicationName("HisabKitab Pro").build()

            val dbFile = listOf(
                AppDatabase.DATABASE_NAME,
                "hisabkitab_pro_blueprint_v27.db"
            ).asSequence()
                .map { context.getDatabasePath(it) }
                .firstOrNull { it.exists() }
                ?: return@withContext Result.failure(Exception("Database file not found"))

            val backupDir = File(context.cacheDir, "db_backup").apply { mkdirs() }
            val snapshotFile = File(backupDir, "snapshot_${System.currentTimeMillis()}.db")
            dbFile.copyTo(snapshotFile, overwrite = true)
            if (!snapshotFile.exists() || snapshotFile.length() <= 0L) {
                return@withContext Result.failure(Exception("Snapshot creation failed"))
            }
            val checksum = sha256(snapshotFile)
            File(backupDir, "${snapshotFile.name}.sha256").writeText(checksum)

            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = "hisabkitab_backup_${System.currentTimeMillis()}.db"
                parents = listOf("appDataFolder")
            }

            val mediaContent = FileContent("application/x-sqlite3", snapshotFile)
            driveService.files().create(fileMetadata, mediaContent).execute()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun validateBackupFile(backupFile: File): Result<BackupValidationReport> = withContext(Dispatchers.IO) {
        try {
            if (!backupFile.exists()) {
                return@withContext Result.failure(Exception("Backup file not found"))
            }
            if (backupFile.length() <= 0L) {
                return@withContext Result.failure(Exception("Backup file is empty"))
            }

            val computed = sha256(backupFile)
            val sidecar = File("${backupFile.absolutePath}.sha256")
            val checksumMatchesSidecar = if (sidecar.exists()) {
                sidecar.readText().trim().equals(computed, ignoreCase = true)
            } else {
                true
            }

            Result.success(
                BackupValidationReport(
                    valid = checksumMatchesSidecar,
                    filePath = backupFile.absolutePath,
                    sizeBytes = backupFile.length(),
                    checksumSha256 = computed,
                    checksumMatchesSidecar = checksumMatchesSidecar
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun previewRestoreFromLocalFile(localBackupPath: String): Result<BackupValidationReport> {
        return validateBackupFile(File(localBackupPath))
    }

    /**
     * Controlled restore API.
     * Default is dry-run (applyRestore = false), so no destructive action happens.
     */
    suspend fun restoreDatabaseFromLocalFile(
        localBackupPath: String,
        applyRestore: Boolean = false,
        confirmationPhrase: String? = null
    ): Result<BackupValidationReport> = withContext(Dispatchers.IO) {
        val backupFile = File(localBackupPath)
        val validation = validateBackupFile(backupFile).getOrElse { return@withContext Result.failure(it) }
        if (!validation.valid) {
            return@withContext Result.failure(Exception("Backup checksum validation failed"))
        }
        if (!applyRestore) {
            return@withContext Result.success(validation)
        }
        if (confirmationPhrase != RESTORE_CONFIRMATION_PHRASE) {
            return@withContext Result.failure(
                Exception("Restore blocked: explicit confirmation phrase is required for applyRestore=true")
            )
        }

        try {
            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            val restoreDir = File(context.cacheDir, "db_restore").apply { mkdirs() }
            val safetyBackup = File(restoreDir, "pre_restore_${System.currentTimeMillis()}.db")

            if (dbFile.exists()) {
                dbFile.copyTo(safetyBackup, overwrite = true)
            }

            val dbParent = dbFile.parentFile ?: return@withContext Result.failure(Exception("DB parent dir missing"))
            if (!dbParent.exists()) dbParent.mkdirs()

            // Remove WAL/SHM so restored file doesn't conflict with old journal state.
            File("${dbFile.absolutePath}-wal").delete()
            File("${dbFile.absolutePath}-shm").delete()

            backupFile.copyTo(dbFile, overwrite = true)

            Result.success(validation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
