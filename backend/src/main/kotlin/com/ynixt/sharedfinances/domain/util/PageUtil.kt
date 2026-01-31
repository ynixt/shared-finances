package com.ynixt.sharedfinances.domain.util

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

object PageUtil {
    fun <T : Any> createPageWithMono(
        pageable: Pageable,
        countFn: () -> Mono<Long>,
        getPageFn: () -> Flux<T>,
    ): Mono<Page<T>> =
        countFn().flatMap { count ->
            if (count == 0L) {
                Mono.just(Page.empty(pageable))
            } else {
                getPageFn().collectList().map { items ->
                    PageImpl(items, pageable, count)
                }
            }
        }

    suspend fun <T : Any> createPage(
        pageable: Pageable,
        countFn: () -> Mono<Long>,
        getPageFn: () -> Flux<T>,
    ): Page<T> {
        val count = countFn().awaitSingle()

        if (count == 0L) {
            return Page.empty(pageable)
        }

        val items =
            getPageFn()
                .collectList()
                .awaitSingle()

        return PageImpl(items, pageable, count)
    }
}
