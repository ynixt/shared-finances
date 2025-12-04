package com.ynixt.sharedfinances.domain.models

data class CursorPage<T>(
    val items: List<T>,
    val nextCursor: Map<String, Any>?,
    val hasNext: Boolean,
) {
    fun <U> map(mapper: (T) -> U): CursorPage<U> = CursorPage(items.map(mapper), nextCursor, hasNext)
}
