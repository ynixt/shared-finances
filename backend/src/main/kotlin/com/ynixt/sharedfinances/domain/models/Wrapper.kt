package com.ynixt.sharedfinances.domain.models

data class Wrapper<T>(
    val value: T?,
) {
    companion object {
        fun <T> empty(): Wrapper<T> = Wrapper(null)
    }
}
