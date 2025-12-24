package com.ynixt.sharedfinances.application.config.security

import com.ynixt.sharedfinances.application.converters.JwtToUserAuthConverter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.core.convert.converter.Converter
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

@PreAuthorize("hasRole('SERVICE_SECRET')")
annotation class OnlyServiceSecretAllowed

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig(
    @param:Value("\${APP_SERVICE_SECRET}") private val serviceSecret: String,
    private val jwtDecoder: ReactiveJwtDecoder,
    private val jwtToUserAuthConverter: JwtToUserAuthConverter,
) {
    @Bean
    fun jwtChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .csrf { csrf ->
//                csrf.csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
//                csrf.requireCsrfProtectionMatcher(csrfForCookieAuthOnly())
                csrf.disable()
            }.addFilterAt(
                serviceSecretFilter(serviceSecret),
                SecurityWebFiltersOrder.AUTHENTICATION,
            ).authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/actuator/**")
                    .permitAll()
                    .pathMatchers("/open/**")
                    .permitAll()
                    .anyExchange()
                    .authenticated()
            }
//            .oauth2Login { }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtDecoder(jwtDecoder)
                    @Suppress("UNCHECKED_CAST")
                    jwt.jwtAuthenticationConverter(
                        jwtToUserAuthConverter as Converter<Jwt, Mono<AbstractAuthenticationToken>>,
                    )
                }
            }.build()

    private fun serviceSecretFilter(secret: String): AuthenticationWebFilter =
        AuthenticationWebFilter(ServiceSecretAuthManager(secret)).apply {
            setServerAuthenticationConverter(ServiceSecretConverter())
        }
}

// private fun csrfForCookieAuthOnly(): ServerWebExchangeMatcher {
//    val paths = ServerWebExchangeMatchers.pathMatchers(
//        HttpMethod.POST, "/auth/refresh", "/auth/logout"
//    )
//
//    return ServerWebExchangeMatcher { exchange ->
//        paths.matches(exchange).flatMap { pathMatch ->
//            if (!pathMatch.isMatch) {
//                return@flatMap ServerWebExchangeMatcher.MatchResult.notMatch()
//            }
//
//            val hasRefreshCookie = exchange.request.cookies.containsKey("refresh_token")
//            if (hasRefreshCookie) ServerWebExchangeMatcher.MatchResult.match()
//            else ServerWebExchangeMatcher.MatchResult.notMatch()
//        }
//    }
// }

class ServiceSecretAuthManager(
    private val expected: String,
) : ReactiveAuthenticationManager {
    override fun authenticate(auth: Authentication): Mono<Authentication> =
        if (auth is ServiceSecretAuthenticationToken && auth.secret == expected) {
            Mono.just(auth)
        } else {
            Mono.error(BadCredentialsException("invalid service secret"))
        }
}

class ServiceSecretAuthenticationToken(
    val secret: String,
) : AbstractAuthenticationToken(listOf(SimpleGrantedAuthority("ROLE_SERVICE_SECRET"))) {
    override fun getPrincipal() = "service"

    override fun getCredentials() = secret

    init {
        isAuthenticated = true
    }
}

class ServiceSecretConverter(
    private val header: String = "Authorization",
) : ServerAuthenticationConverter {
    override fun convert(exchange: ServerWebExchange): Mono<Authentication> {
        val value = exchange.request.headers.getFirst(header) ?: return Mono.empty()

        if (value.startsWith("Bearer ")) return Mono.empty()

        return Mono.just(ServiceSecretAuthenticationToken(value))
    }
}

@Component
class ServiceSecretGateFilter(
    @param:Qualifier("requestMappingHandlerMapping")
    private val mapping: RequestMappingHandlerMapping,
) : WebFilter,
    Ordered {
    override fun getOrder(): Int = SecurityWebFiltersOrder.AUTHORIZATION.order + 1

    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
    ): Mono<Void> =
        mapping
            .getHandler(exchange)
            .switchIfEmpty(chain.filter(exchange))
            .flatMap { handler ->
                val requiresSecret =
                    (handler as? HandlerMethod)?.let {
                        AnnotationUtils.findAnnotation(it.method, OnlyServiceSecretAllowed::class.java) != null ||
                            AnnotationUtils.findAnnotation(it.beanType, OnlyServiceSecretAllowed::class.java) != null
                    } ?: false

                exchange
                    .getPrincipal<Authentication>()
                    .map { Optional.of(it) }
                    .switchIfEmpty(Mono.just(Optional.empty()))
                    .flatMap { auth ->
                        val isService =
                            auth
                                .getOrNull()
                                ?.authorities
                                ?.any { it.authority == "ROLE_SERVICE_SECRET" } ?: false

                        when {
                            isService && !requiresSecret ->
                                Mono.error(AccessDeniedException("service‑secret not allowed here"))

                            !isService && requiresSecret ->
                                Mono.error(AccessDeniedException("JWT not allowed here"))

                            else -> chain.filter(exchange)
                        }
                    }
            }
}
