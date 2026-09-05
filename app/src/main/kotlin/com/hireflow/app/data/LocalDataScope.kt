package com.hireflow.app.data

object LocalDataScope {
    fun matches(
        rowOrganizationId: String?,
        activeOrganizationId: String?,
        authenticated: Boolean,
        offlineMode: Boolean
    ): Boolean = when {
        offlineMode -> rowOrganizationId == null
        authenticated && activeOrganizationId != null -> rowOrganizationId == activeOrganizationId
        else -> false
    }
}
