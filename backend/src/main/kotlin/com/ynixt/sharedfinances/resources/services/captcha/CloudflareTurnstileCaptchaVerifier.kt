package com.ynixt.sharedfinances.resources.services.captcha

import com.ynixt.sharedfinances.application.config.AuthProperties
import com.ynixt.sharedfinances.domain.services.captcha.CaptchaVerifier
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

@Component
class CloudflareTurnstileCaptchaVerifier(
    private val authProperties: AuthProperties,
    webClientBuilder: WebClient.Builder,
    private val objectMapper: ObjectMapper,
) : CaptchaVerifier {
    private val webClient: WebClient = webClientBuilder.build()
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun verify(
        token: String?,
        remoteIp: String?,
    ): Boolean {
        val secret = authProperties.turnstile.secretKey
        if (secret.isBlank()) {
            logger.warn("Turnstile secret is blank; failing verification")
            return false
        }
        if (token.isNullOrBlank()) {
            return false
        }
        val form =
            LinkedMultiValueMap<String, String>().apply {
                add("secret", secret)
                add("response", token)
                remoteIp?.let { add("remoteip", it) }
            }
        val body =
            try {
                webClient
                    .post()
                    .uri(authProperties.turnstile.verifyUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .bodyToMono(String::class.java)
                    .awaitSingle()
            } catch (e: Exception) {
                logger.warn("Turnstile verification request failed: {}", e.message)
                return false
            }
        val node: JsonNode = objectMapper.readTree(body)
        return node.path("success").asBoolean(false)
    }
}
