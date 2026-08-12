package com.ynixt.sharedfinances.application.config

import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.data.domain.Pageable
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.reactive.BindingContext
import kotlin.test.assertEquals

class WebConfigTest {
    @Test
    fun `caps requested page size at one hundred`() {
        val pageable = resolvePageable(500)

        assertEquals(100, pageable.pageSize)
    }

    @Test
    fun `preserves page sizes below the cap`() {
        val pageable = resolvePageable(10)

        assertEquals(10, pageable.pageSize)
    }

    private fun resolvePageable(size: Int): Pageable {
        val method = Handler::class.java.getDeclaredMethod("handle", Pageable::class.java)
        val parameter = MethodParameter(method, 0)
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/?size=$size"))
        return WebConfig().pageableResolver().resolveArgumentValue(parameter, BindingContext(), exchange)
    }

    private class Handler {
        @Suppress("unused")
        fun handle(pageable: Pageable) = pageable
    }
}
