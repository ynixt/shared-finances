package com.ynixt.sharedfinances.resources.repositories.r2dbc

import com.ynixt.sharedfinances.domain.entities.GroupUser
import com.ynixt.sharedfinances.domain.entities.User
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class GroupUsersR2DBCRepository(
    private val dbClient: DatabaseClient,
) {
    fun findAllMembers(groupId: UUID): Flux<GroupUser> {
        val sql =
            """
            SELECT
              gu.group_id,
              gu.user_id,
              gu.role,
              u.id                AS u_id,
              u.created_at        AS u_created_at,
              u.updated_at        AS u_updated_at,
              u.external_id       AS u_external_id,
              u.email             AS u_email,
              u.first_name        AS u_first_name,
              u.last_name         AS u_last_name,
              u.lang              AS u_lang,
              u.default_currency  AS u_default_currency
            FROM group_user gu
            JOIN users u ON u.id = gu.user_id
            WHERE gu.group_id = :groupId
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("groupId", groupId)
            .map { row, _ ->
                GroupUser(
                    groupId = row.get("group_id", UUID::class.java)!!,
                    userId = row.get("user_id", UUID::class.java)!!,
                    role = UserGroupRole.valueOf(row.get("role", String::class.java)!!),
                ).also { gu ->
                    gu.user =
                        User(
                            externalId = row.get("u_external_id", String::class.java)!!,
                            email = row.get("u_email", String::class.java)!!,
                            firstName = row.get("u_first_name", String::class.java)!!,
                            lastName = row.get("u_last_name", String::class.java)!!,
                            lang = row.get("u_lang", String::class.java)!!,
                            defaultCurrency = row.get("u_default_currency", String::class.java),
                        ).also { u ->
                            u.id = row.get("u_id", UUID::class.java)!!
                            u.createdAt = row.get("u_created_at", OffsetDateTime::class.java)
                            u.updatedAt = row.get("u_updated_at", OffsetDateTime::class.java)
                        }
                }
            }.all()
    }
}
