package com.ynixt.sharedfinances.domain.models.dashboard

import java.util.UUID

sealed interface OverviewDashboardScope {
    val actorUserId: UUID

    data class Individual(
        override val actorUserId: UUID,
    ) : OverviewDashboardScope

    data class Group(
        override val actorUserId: UUID,
        val groupId: UUID,
    ) : OverviewDashboardScope
}
