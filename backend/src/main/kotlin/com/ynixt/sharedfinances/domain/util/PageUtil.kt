package com.ynixt.sharedfinances.domain.util

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

object PageUtil {
    fun <T> createPage(
        pageable: Pageable,
        countFn: () -> Mono<Long>,
        getPageFn: () -> Flux<T>,
    ): Mono<Page<T>> =
        countFn().flatMap { count ->
            if (count == 0L) {
                Mono.just(Page.empty())
            } else {
                getPageFn().collectList().map { items ->
                    PageImpl(items, pageable, count)
                }
            }
        }
}
