package com.fittakip.app.notify

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fittakip.app.auth.DriveSync
import com.fittakip.app.data.AppDatabase
import com.fittakip.app.data.Backup
import com.fittakip.app.data.SettingsStore
import java.util.concurrent.TimeUnit

/** Google Drive'a günlük otomatik yedek (internet varken). */
object DriveBackup {
    private const val DAILY = "drive-daily"
    private const val SOON = "drive-soon"
    private val net = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun schedule(context: Context, on: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!on) { wm.cancelUniqueWork(DAILY); wm.cancelUniqueWork(SOON); return }
        wm.enqueueUniquePeriodicWork(
            DAILY, ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DriveBackupWorker>(1, TimeUnit.DAYS).setConstraints(net).build()
        )
    }

    /** Antrenman ya da kayıt sonrası: 20 dk içinde bir yedek (art arda değişiklikler tek yedekte birleşir). */
    fun soon(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            SOON, ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<DriveBackupWorker>().setInitialDelay(20, TimeUnit.MINUTES).setConstraints(net).build()
        )
    }

    /** Şimdi yedekle; hata mesajı ya da null döner. */
    suspend fun run(context: Context, token: String? = null): String? {
        val t = token ?: DriveSync.tokenSilently(context) ?: return "Drive izni gerekli. Ayarlar'dan Drive yedeğini yeniden aç."
        return try {
            DriveSync.save(t, Backup.export(AppDatabase.get(context), context))
            SettingsStore(context).update { it.copy(lastDriveBackup = System.currentTimeMillis()) }
            null
        } catch (e: Exception) {
            e.message ?: "Drive yedeği alınamadı."
        }
    }
}

class DriveBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!SettingsStore(applicationContext).state.value.driveBackupOn) return Result.success()
        val err = DriveBackup.run(applicationContext)
        return if (err == null || err.contains("izni")) Result.success() else Result.retry()
    }
}
