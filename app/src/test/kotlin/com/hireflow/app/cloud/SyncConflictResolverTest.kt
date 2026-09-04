package com.hireflow.app.cloud

import com.hireflow.app.data.SyncState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncConflictResolverTest {
    @Test
    fun `keeps newer pending local change`() {
        assertTrue(SyncConflictResolver.keepLocal(SyncState.PENDING.name, 2_000, 1_000))
    }

    @Test
    fun `accepts remote when local is synced or older`() {
        assertFalse(SyncConflictResolver.keepLocal(SyncState.SYNCED.name, 2_000, 1_000))
        assertFalse(SyncConflictResolver.keepLocal(SyncState.PENDING.name, 1_000, 2_000))
    }
}
