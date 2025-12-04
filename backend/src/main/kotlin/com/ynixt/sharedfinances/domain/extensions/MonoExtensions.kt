package com.ynixt.sharedfinances.domain.extensions

import com.ynixt.sharedfinances.application.web.dto.CursorPageDto
import com.ynixt.sharedfinances.domain.models.CursorPage
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

    inline fun <T, R> Mono<CursorPageDto<T>>.mapCursorPage(crossinline mapper: (T) -> R): Mono<CursorPageDto<R>> =
        this.map { cursorPage ->
            cursorPage.map { mapper(it) }
        }

    inline fun <T, R> Mono<CursorPage<T>>.mapCursorPageToDto(crossinline mapper: (T) -> R): Mono<CursorPageDto<R>> =
        this.map { cursorPage ->
            CursorPageDto(
                items = cursorPage.items,
                hasNext = cursorPage.hasNext,
                nextCursor = cursorPage.nextCursor,
            ).map { mapper(it) }
        }
}
