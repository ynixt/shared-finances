package com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient

import com.ynixt.sharedfinances.domain.entities.groups.GroupUserEntity
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.UserR2DBCMapping
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.util.UUID

@Repository
class GroupUsersDatabaseClientRepository(
    private val dbClient: DatabaseClient,
) : DatabaseClientRepository() {
    fun findAllMembers(groupId: UUID): Flux<GroupUserEntity> {
        val sql =
            """
            SELECT
              gu.id,
              gu.group_id,
              gu.user_id,
              gu.role,
              gu.allow_planning_simulator,
              ${UserR2DBCMapping.createSelectForUser("u")}
            FROM group_user gu
            JOIN users u ON u.id = gu.user_id
            WHERE gu.group_id = :groupId
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("groupId", groupId)
            .map { row, _ ->
                GroupUserEntity(
                    groupId = row.get("group_id", UUID::class.java)!!,
                    userId = row.get("user_id", UUID::class.java)!!,
                    role = UserGroupRole.valueOf(row.get("role", String::class.java)!!),
                    allowPlanningSimulator = row.get("allow_planning_simulator", java.lang.Boolean::class.java)?.booleanValue() ?: true,
                ).also { gu ->
                    gu.id = row.get("id", UUID::class.java)
                    gu.user = UserR2DBCMapping.userFromRow(row)
                }
            }.all()
    }

    fun findAllOptedInUserIds(groupId: UUID): Flux<UUID> {
        val sql =
            """
            SELECT user_id
            FROM group_user
            WHERE group_id = :groupId
              AND allow_planning_simulator = TRUE
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("groupId", groupId)
            .map { row, _ -> row.get("user_id", UUID::class.java)!! }
            .all()
    }
}
