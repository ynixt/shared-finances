package com.ynixt.sharedfinances.application.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

@Configuration
@EnableR2dbcRepositories(
    basePackages = [
        "com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata",
    ],
)
class R2DBCConfig {
//    @Bean
//    fun flywayInitializer(flyway: Flyway): FlywayMigrationInitializer {
//        flyway.clean()
//        return FlywayMigrationInitializer(flyway)
//    }
}
