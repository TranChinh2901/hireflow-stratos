package com.hireflow.app

import android.app.Application
import com.hireflow.app.data.HireFlowDatabase
import com.hireflow.app.data.HireFlowRepository
import com.hireflow.app.cloud.CloudSyncManager
import com.hireflow.app.cloud.SupabaseBackend
import com.hireflow.app.sync.CloudSyncWorker

class HireFlowApplication : Application() {
    val database by lazy { HireFlowDatabase.getInstance(this) }
    val repository by lazy { HireFlowRepository(database.hireFlowDao()) }
    val backend by lazy { SupabaseBackend() }
    val syncManager by lazy { CloudSyncManager(this, repository, backend) }

    override fun onCreate() {
        super.onCreate()
        CloudSyncWorker.schedulePeriodic(this)
    }

    fun requestCloudSync() = CloudSyncWorker.requestNow(this)
}
