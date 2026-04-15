package com.ynixt.sharedfinances.resources.services.mail

import com.ynixt.sharedfinances.application.config.AuthBrevoMailProperties
import com.ynixt.sharedfinances.application.config.AuthProperties
import com.ynixt.sharedfinances.domain.exceptions.http.auth.NoTransactionalEmailProviderAvailableException
import com.ynixt.sharedfinances.domain.mail.AuthTransactionalEmailMessage
import com.ynixt.sharedfinances.resources.repositories.redis.MailProviderQuotaRedisRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.http.MediaType
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.ObjectMapper

@Service
class TransactionalEmailDispatchService(
    private val authProperties: AuthProperties,
    private val quotaRepository: MailProviderQuotaRedisRepository,
    private val objectMapper: ObjectMapper,
    webClientBuilder: WebClient.Builder,
    private val javaMailSenderProvider: ObjectProvider<JavaMailSender>,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val webClient: WebClient = webClientBuilder.build()

    suspend fun send(message: AuthTransactionalEmailMessage) {
        for (providerId in authProperties.transactionalMail.providerPriority) {
            when (providerId.lowercase()) {
                "smtp" -> {
                    val cfg = authProperties.transactionalMail.smtp
                    val mailSender = javaMailSenderProvider.ifAvailable
                    if (mailSender == null) {
                        logger.debug("tx-mail provider=smtp action=skip reason=no_java_mail_sender")
                        continue
                    }
                    val reserved = quotaRepository.tryReserveOne("smtp", cfg.dailyQuota).awaitSingle()
                    if (!reserved) {
                        logger.info("tx-mail provider=smtp action=skip reason=no_quota")
                        continue
                    }
                    try {
                        sendSmtp(mailSender, cfg.fromAddress, cfg.fromName, message)
                        logger.info("tx-mail provider=smtp action=send status=ok to={}", message.toAddress)
                        return
                    } catch (ex: Exception) {
                        quotaRepository.rollbackOne("smtp").awaitSingle()
                        logger.warn("tx-mail provider=smtp action=send status=fail", ex)
                    }
                }
                "brevo" -> {
                    val cfg = authProperties.transactionalMail.brevo
                    if (cfg.apiKey.isBlank()) {
                        logger.debug("tx-mail provider=brevo action=skip reason=missing_api_key")
                        continue
                    }
                    val reserved = quotaRepository.tryReserveOne("brevo", cfg.dailyQuota).awaitSingle()
                    if (!reserved) {
                        logger.info("tx-mail provider=brevo action=skip reason=no_quota")
                        continue
                    }
                    try {
                        sendBrevo(cfg, message)
                        logger.info("tx-mail provider=brevo action=send status=ok to={}", message.toAddress)
                        return
                    } catch (ex: Exception) {
                        quotaRepository.rollbackOne("brevo").awaitSingle()
                        logger.warn("tx-mail provider=brevo action=send status=fail", ex)
                    }
                }
                else -> logger.warn("tx-mail provider={} action=skip reason=unknown_id", providerId)
            }
        }
        throw NoTransactionalEmailProviderAvailableException()
    }

    private suspend fun sendSmtp(
        mailSender: JavaMailSender,
        fromAddress: String,
        fromName: String,
        message: AuthTransactionalEmailMessage,
    ) {
        withContext(Dispatchers.IO) {
            val mime = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(mime, true, Charsets.UTF_8.name())
            helper.setFrom(fromAddress, fromName)
            helper.setTo(message.toAddress)
            helper.setSubject(message.subject)
            if (message.htmlBody != null) {
                helper.setText(message.textBody, message.htmlBody)
            } else {
                helper.setText(message.textBody, false)
            }
            mailSender.send(mime)
        }
    }

    private suspend fun sendBrevo(
        cfg: AuthBrevoMailProperties,
        message: AuthTransactionalEmailMessage,
    ) {
        val fromEmail = cfg.fromAddress.ifBlank { authProperties.transactionalMail.smtp.fromAddress }
        val fromName = cfg.fromName.ifBlank { authProperties.transactionalMail.smtp.fromName }
        val root = objectMapper.createObjectNode()
        root.put("subject", message.subject)
        val sender = objectMapper.createObjectNode()
        sender.put("email", fromEmail)
        sender.put("name", fromName)
        root.set("sender", sender)
        val toArr = objectMapper.createArrayNode()
        val to0 = objectMapper.createObjectNode()
        to0.put("email", message.toAddress)
        toArr.add(to0)
        root.set("to", toArr)
        root.put("textContent", message.textBody)
        message.htmlBody?.let { root.put("htmlContent", it) }
        val json = objectMapper.writeValueAsString(root)
        withContext(Dispatchers.IO) {
            webClient
                .post()
                .uri("https://api.brevo.com/v3/smtp/email")
                .header("api-key", cfg.apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(json))
                .retrieve()
                .toBodilessEntity()
                .awaitSingle()
        }
    }
}
