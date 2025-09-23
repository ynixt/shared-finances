package com.ynixt.sharedfinances.application.config.graphql

import graphql.scalars.ExtendedScalars
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.RuntimeWiringConfigurer

@Configuration
class GraphqlConfig {
    @Bean
    fun runtimeWiringConfigurer() =
        RuntimeWiringConfigurer { wiring ->
            wiring
                .scalar(ExtendedScalars.UUID)
                .scalar(ExtendedScalars.GraphQLBigDecimal)
        }
}
