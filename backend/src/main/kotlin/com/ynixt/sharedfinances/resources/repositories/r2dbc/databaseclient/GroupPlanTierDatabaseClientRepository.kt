package com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient

import com.ynixt.sharedfinances.domain.enums.GroupPlanTier
import com.ynixt.sharedfinances.domain.repositories.GroupPlanTierRepository
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class GroupPlanTierDatabaseClientRepository(
    private val dbClient: DatabaseClient,
) : GroupPlanTierRepository {
    override suspend fun resolve(groupId: UUID): GroupPlanTier? =
        dbClient
            .sql(
                """
                SELECT CASE WHEN u.role IN ('PRO', 'ADMINISTRATOR') THEN 'PRO' ELSE 'COMMON' END AS tier
                FROM "group" g
                JOIN users u ON u.id = g.owner_user_id
                WHERE g.id = :groupId
                """.trimIndent(),
            ).bind("groupId", groupId)
            .map { row, _ -> GroupPlanTier.valueOf(row.get("tier", String::class.java)!!) }
            .one()
            .awaitSingleOrNull()
}
