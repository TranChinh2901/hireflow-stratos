package com.hireflow.app.cloud

import com.hireflow.app.data.SyncState

object SyncConflictResolver {
    fun keepLocal(localSyncState: String, localUpdatedAt: Long, remoteUpdatedAt: Long): Boolean =
        localSyncState == SyncState.PENDING.name && localUpdatedAt > remoteUpdatedAt
}
