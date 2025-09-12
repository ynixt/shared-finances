package com.ynixt.sharedfinances.domain.services

interface DatabaseHelperService {
    fun isUniqueViolation(
        t: Throwable,
        indexName: String,
    ): Boolean
}
