package com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient

import org.springframework.data.domain.Pageable

abstract class DatabaseClientRepository {
    protected fun pageableToSortQuery(
        pageable: Pageable,
        allowedSortColumns: Set<String>,
    ): String {
        val sortClause =
            pageable.sort
                .filter { it.property in allowedSortColumns }
                .joinToString(", ") { "${it.property} ${it.direction.name}" }
                .ifBlank { "name ASC" }

        return "ORDER BY $sortClause"
    }
}
