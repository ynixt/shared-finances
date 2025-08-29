package com.ynixt.sharedfinances.domain.extensions

import org.springframework.data.domain.Page
import reactor.core.publisher.Mono

object MonoExtensions {
    inline fun <T, R> _root_ide_package_.reactor.core.publisher.Mono<Page<T>>.mapPage(crossinline mapper: (T) -> R): Mono<Page<R>> =
        this.map { page ->
            page.map { mapper(it) }
        }
}
