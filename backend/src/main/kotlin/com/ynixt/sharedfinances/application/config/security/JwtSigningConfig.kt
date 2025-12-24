package com.ynixt.sharedfinances.application.config.security

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

@Configuration
class JwtSigningConfig(
    @param:Value("\${app.security.jwt.publicKeyBase64}") private val publicKeyBase64: String,
    @param:Value("\${app.security.jwt.privateKeyBase64}") private val privateKeyBase64: String,
    @param:Value("\${app.security.jwt.kid}") private val kid: String,
) {
    @Bean
    fun jwtEncoder(
        @Qualifier("jwtPrivateKey") rsaPrivateKey: RSAPrivateKey,
        @Qualifier("jwtPublicKey") rsaPublicKey: RSAPublicKey,
    ): JwtEncoder {
        val rsaJwk =
            RSAKey
                .Builder(rsaPublicKey)
                .privateKey(rsaPrivateKey)
                .keyID(kid)
                .build()

        val jwkSet = JWKSet(rsaJwk)
        val jwkSource = ImmutableJWKSet<SecurityContext>(jwkSet)
        return NimbusJwtEncoder(jwkSource)
    }

    @Bean
    fun reactiveJwtDecoder(
        @Qualifier("jwtPublicKey") rsaPublicKey: RSAPublicKey,
    ): ReactiveJwtDecoder = NimbusReactiveJwtDecoder.withPublicKey(rsaPublicKey).build()

    @Bean("jwtPrivateKey")
    fun rsaPrivateKey(): RSAPrivateKey {
        val der = Base64.getDecoder().decode(privateKeyBase64.trim())
        val spec = PKCS8EncodedKeySpec(der)
        return KeyFactory.getInstance("RSA").generatePrivate(spec) as RSAPrivateKey
    }

    @Bean("jwtPublicKey")
    fun rsaPublicKey(): RSAPublicKey {
        val der = Base64.getDecoder().decode(publicKeyBase64.trim())
        val spec = X509EncodedKeySpec(der)
        return KeyFactory.getInstance("RSA").generatePublic(spec) as RSAPublicKey
    }
}
