package com.ynixt.sharedfinances.domain.extensions

import org.springframework.data.domain.Page
import reactor.core.publisher.Mono

object MonoExtensions {
    inline fun <T, R> Mono<Page<T>>.mapPage(crossinline mapper: (T) -> R): Mono<Page<R>> =
        this.map { page ->
            page.map { mapper(it) }
        }

    inline fun <T, R> Mono<List<T>>.mapList(crossinline mapper: (T) -> R): Mono<List<R>> =
        this.map { list ->
            list.map { mapper(it) }
        }
}
