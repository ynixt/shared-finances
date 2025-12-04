package com.ynixt.sharedfinances.domain.models

data class CursorPageRequest(
    val size: Int = 10,
    val nextCursor: Map<String, Any>? = null,
) {
    init {
        require(size <= 100) { "Size cannot be greater than 100" }
    }
}
