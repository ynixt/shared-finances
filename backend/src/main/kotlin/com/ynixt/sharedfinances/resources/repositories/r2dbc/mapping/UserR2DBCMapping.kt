package com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping

import com.ynixt.sharedfinances.domain.entities.User
import io.r2dbc.spi.Row
import java.time.OffsetDateTime
import java.util.UUID

class UserR2DBCMapping {
    companion object {
        fun createSelectForUser(
            tableAlias: String = "u",
            columnPrefix: String = "u_",
        ): String =
            """
            $tableAlias.id                AS ${columnPrefix}id,
            $tableAlias.created_at        AS ${columnPrefix}created_at,
            $tableAlias.updated_at        AS ${columnPrefix}updated_at,
            $tableAlias.external_id       AS ${columnPrefix}external_id,
            $tableAlias.email             AS ${columnPrefix}email,
            $tableAlias.first_name        AS ${columnPrefix}first_name,
            $tableAlias.last_name         AS ${columnPrefix}last_name,
            $tableAlias.lang              AS ${columnPrefix}lang,
            $tableAlias.default_currency  AS ${columnPrefix}default_currency
            """.trimIndent()

        fun userFromRow(
            row: Row,
            columnPrefix: String = "u_",
        ): User =
            User(
                externalId = row.get("${columnPrefix}external_id", String::class.java)!!,
                email = row.get("${columnPrefix}email", String::class.java)!!,
                firstName = row.get("${columnPrefix}first_name", String::class.java)!!,
                lastName = row.get("${columnPrefix}last_name", String::class.java)!!,
                lang = row.get("${columnPrefix}lang", String::class.java)!!,
                defaultCurrency = row.get("${columnPrefix}default_currency", String::class.java),
            ).also { u ->
                u.id = row.get("${columnPrefix}id", UUID::class.java)!!
                u.createdAt = row.get("${columnPrefix}created_at", OffsetDateTime::class.java)
                u.updatedAt = row.get("${columnPrefix}updated_at", OffsetDateTime::class.java)
            }
    }
}
