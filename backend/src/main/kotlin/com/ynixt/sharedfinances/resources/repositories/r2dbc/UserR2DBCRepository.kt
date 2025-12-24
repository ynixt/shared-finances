package com.ynixt.sharedfinances.resources.repositories.r2dbc

import com.ynixt.sharedfinances.domain.entities.UserEntity
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
class UserR2DBCRepository(
    private val template: R2dbcEntityTemplate,
) {
    fun insert(user: UserEntity): Mono<UserEntity> = template.insert(UserEntity::class.java).using(user)
}
