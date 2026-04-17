package com.ynixt.sharedfinances.domain.repositories

import java.util.UUID

enum class WalletTransactionQueryPath {
    OWNERSHIP,
    GROUP_SCOPE,
}

data class WalletTransactionQueryScope(
    val path: WalletTransactionQueryPath,
    val ownerUserIds: Set<UUID> = emptySet(),
    val groupIds: Set<UUID> = emptySet(),
) {
    init {
        when (path) {
            WalletTransactionQueryPath.OWNERSHIP ->
                require(ownerUserIds.isNotEmpty()) {
                    "Ownership scope requires at least one owner user id."
                }
            WalletTransactionQueryPath.GROUP_SCOPE ->
                require(groupIds.isNotEmpty()) {
                    "Group scope requires at least one group id."
                }
        }
    }

    companion object {
        fun ownership(
            ownerUserIds: Set<UUID>,
            groupIds: Set<UUID> = emptySet(),
        ): WalletTransactionQueryScope =
            WalletTransactionQueryScope(
                path = WalletTransactionQueryPath.OWNERSHIP,
                ownerUserIds = ownerUserIds,
                groupIds = groupIds,
            )

        fun group(groupIds: Set<UUID>): WalletTransactionQueryScope =
            WalletTransactionQueryScope(
                path = WalletTransactionQueryPath.GROUP_SCOPE,
                groupIds = groupIds,
            )
    }
}
