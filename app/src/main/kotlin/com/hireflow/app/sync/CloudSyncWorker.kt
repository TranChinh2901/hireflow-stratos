package com.hireflow.app.sync

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
import com.hireflow.app.HireFlowApplication
import java.util.concurrent.TimeUnit

class CloudSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as HireFlowApplication
        if (!app.backend.isConfigured || app.backend.currentUserId() == null) return Result.success()
        return runCatching {
            app.syncManager.syncAll(app.backend.profile())
        }.fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }

    companion object {
        private val connected = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<CloudSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(connected)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "hireflow_periodic_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun requestNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<CloudSyncWorker>().setConstraints(connected).build()
            WorkManager.getInstance(context).enqueueUniqueWork("hireflow_immediate_sync", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
