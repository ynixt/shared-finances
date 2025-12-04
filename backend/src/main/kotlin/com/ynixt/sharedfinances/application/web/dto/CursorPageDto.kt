package com.ynixt.sharedfinances.application.web.dto

data class CursorPageDto<T>(
    val items: List<T>,
    val nextCursor: Map<String, Any>?,
    val hasNext: Boolean,
) {
    fun <U> map(mapper: (T) -> U): CursorPageDto<U> = CursorPageDto(items.map(mapper), nextCursor, hasNext)
}
