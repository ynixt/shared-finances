package com.ynixt.sharedfinances.application.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.web.ReactivePageableHandlerMethodArgumentResolver
import org.springframework.data.web.ReactiveSortHandlerMethodArgumentResolver
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer

@Configuration
class WebConfig : WebFluxConfigurer {
    override fun configureArgumentResolvers(configurer: ArgumentResolverConfigurer) {
        configurer.addCustomResolver(pageableResolver())
        configurer.addCustomResolver(ReactiveSortHandlerMethodArgumentResolver())
    }

    internal fun pageableResolver() =
        ReactivePageableHandlerMethodArgumentResolver().apply {
            setMaxPageSize(MAX_PAGE_SIZE)
        }

    companion object {
        const val MAX_PAGE_SIZE = 100
    }
}
