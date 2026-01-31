package com.ynixt.sharedfinances.domain.extensions

import com.ynixt.sharedfinances.application.web.dto.CursorPageDto
import com.ynixt.sharedfinances.domain.models.CursorPage

object CursorPageExtensions {
    inline fun <T, R> CursorPage<T>.mapCursorPageToDto(crossinline mapper: (T) -> R): CursorPageDto<R> =
        this.let { cursorPage ->
            CursorPageDto(
                items = cursorPage.items,
                hasNext = cursorPage.hasNext,
                nextCursor = cursorPage.nextCursor,
            ).map { mapper(it) }
        }
}
