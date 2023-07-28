package com.ynixt.sharedfinances.config

import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.format.datetime.DateFormatter
import org.springframework.format.datetime.DateFormatterRegistrar
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Configuration
class WebMvcConfig : WebMvcConfigurer {
    override fun addFormatters(registry: FormatterRegistry) {
        val registrar = DateTimeFormatterRegistrar()
        registrar.setUseIsoFormat(true)
        registrar.registerFormatters(registry)

        val dateRegistrar = DateFormatterRegistrar()
        dateRegistrar.setFormatter(DateFormatter("yyyy-MM-dd"))
        dateRegistrar.registerFormatters(registry)
    }
}
