package com.ynixt.sharedfinances.application.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun openAPI(): OpenAPI {
        val bearerScheme =
            SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")

        val apiKeyScheme =
            SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .scheme("api-key")
                .name("Authorization")
                .`in`(SecurityScheme.In.HEADER)

        return OpenAPI()
            .components(
                Components()
                    .addSecuritySchemes("bearer-jwt", bearerScheme)
                    .addSecuritySchemes("api-key", apiKeyScheme),
            ).addSecurityItem(SecurityRequirement().addList("bearer-jwt"))
    }

    @Bean
    fun openPathsArePublic(): OpenApiCustomizer =
        OpenApiCustomizer { openApi ->
            openApi.paths?.forEach { (path, pathItem) ->
                if (path == "/open" || path.startsWith("/open/")) {
                    pathItem.readOperations()?.forEach { op ->
                        op.security = emptyList()
                    }
                }
            }
        }
}
