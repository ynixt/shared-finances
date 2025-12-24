package com.ynixt.sharedfinances.domain.repositories

import reactor.core.publisher.Mono

interface FailedLoginRepository {
    fun incrementFails(
        ip: String,
        email: String,
        ttlSeconds: Long,
    ): Mono<Void>

    fun deleteByIpAndEmail(
        ip: String,
        email: String,
    ): Mono<Boolean>

    fun getFails(
        ip: String,
        email: String,
    ): Mono<Int>
}
