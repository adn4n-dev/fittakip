package com.fittakip.app.notify

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fittakip.app.data.AppDatabase
import com.fittakip.app.data.Backup
import com.fittakip.app.data.SettingsStore
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Kullanıcının seçtiği klasöre (ör. Google Drive klasörü) haftada bir otomatik JSON yedeği yazar. */
object AutoBackup {
    private const val WORK = "auto_backup"

    fun schedule(context: Context, on: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!on) { wm.cancelUniqueWork(WORK); return }
        val req = PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS).build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    suspend fun run(context: Context): Boolean {
        val store = SettingsStore.get(context)
        val tree = store.state.value.autoBackupUri.takeIf { it.isNotEmpty() }?.let(Uri::parse) ?: return false
        return try {
            val json = Backup.export(AppDatabase.get(context), context)
            val dir = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            val file = DocumentsContract.createDocument(
                context.contentResolver, dir, "application/json", "fittakip-otomatik-${LocalDate.now()}.json"
            ) ?: return false
            context.contentResolver.openOutputStream(file)?.use { it.write(json.toByteArray()) } ?: return false
            store.update { it.copy(lastAutoBackup = System.currentTimeMillis()) }
            true
        } catch (_: Exception) {
            false
        }
    }
}

class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = if (AutoBackup.run(applicationContext)) Result.success() else Result.retry()
}
