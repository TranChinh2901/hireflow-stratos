package com.hireflow.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDataScopeTest {
    @Test
    fun `offline mode only exposes unscoped demo rows`() {
        assertTrue(LocalDataScope.matches(null, null, authenticated = false, offlineMode = true))
        assertFalse(LocalDataScope.matches("org-a", null, authenticated = false, offlineMode = true))
    }

    @Test
    fun `authenticated mode only exposes rows in the active organization`() {
        assertTrue(LocalDataScope.matches("org-a", "org-a", authenticated = true, offlineMode = false))
        assertFalse(LocalDataScope.matches("org-b", "org-a", authenticated = true, offlineMode = false))
        assertFalse(LocalDataScope.matches(null, "org-a", authenticated = true, offlineMode = false))
    }

    @Test
    fun `signed out state exposes no local recruitment data`() {
        assertFalse(LocalDataScope.matches(null, null, authenticated = false, offlineMode = false))
        assertFalse(LocalDataScope.matches("org-a", null, authenticated = false, offlineMode = false))
    }
}
