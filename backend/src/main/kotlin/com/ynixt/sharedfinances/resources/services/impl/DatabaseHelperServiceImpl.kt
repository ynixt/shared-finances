package com.ynixt.sharedfinances.resources.services.impl

import com.ynixt.sharedfinances.domain.services.DatabaseHelperService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import reactor.core.Exceptions

@Service
class DatabaseHelperServiceImpl : DatabaseHelperService {
    override fun isUniqueViolation(
        t: Throwable,
        indexName: String,
    ): Boolean {
        val root = Exceptions.unwrap(t)

        if (root is DuplicateKeyException) return root.message?.contains(indexName, ignoreCase = true) == true
        if (root is DataIntegrityViolationException &&
            (root.message?.contains(indexName, ignoreCase = true) == true)
        ) {
            return true
        }

        val hasIndexInChain =
            generateSequence(root as Throwable?) { it.cause }
                .any { it.message?.contains(indexName, ignoreCase = true) == true }

        return hasIndexInChain
    }
}
