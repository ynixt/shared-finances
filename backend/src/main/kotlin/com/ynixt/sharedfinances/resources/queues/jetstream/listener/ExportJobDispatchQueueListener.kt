package com.ynixt.sharedfinances.resources.queues.jetstream.listener

import com.ynixt.sharedfinances.domain.models.exports.ExportJobDispatchMessage
import com.ynixt.sharedfinances.domain.services.exports.ExportJobService
import com.ynixt.sharedfinances.resources.queues.jetstream.JetStreamConstants.EXPORT_JOB_DISPATCH_SUBJECT
import com.ynixt.sharedfinances.resources.queues.jetstream.JetStreamConstants.EXPORT_JOB_WORKER_CONSUMER
import io.nats.client.Connection
import io.nats.client.JetStreamSubscription
import io.nats.client.Message
import io.nats.client.PullSubscribeOptions
import io.nats.client.api.AckPolicy
import io.nats.client.api.ConsumerConfiguration
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.Duration

@Component
@ConditionalOnProperty(name = ["app.exports.worker.enabled"], havingValue = "true", matchIfMissing = true)
class ExportJobDispatchQueueListener(
    private val connection: Connection,
    private val objectMapper: ObjectMapper,
    private val exportJobService: ExportJobService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var subscription: JetStreamSubscription

    @EventListener(ApplicationReadyEvent::class)
    fun startListening() {
        val options =
            PullSubscribeOptions
                .builder()
                .durable(EXPORT_JOB_WORKER_CONSUMER)
                .configuration(
                    ConsumerConfiguration
                        .builder()
                        .ackPolicy(AckPolicy.Explicit)
                        .ackWait(Duration.ofMinutes(2))
                        .build(),
                ).build()
        subscription = connection.jetStream().subscribe(EXPORT_JOB_DISPATCH_SUBJECT, options)
        scope.launch {
            while (isActive) {
                runCatching {
                    subscription.fetch(5, Duration.ofSeconds(1)).map { message -> launch { process(message) } }.joinAll()
                }.onFailure {
                    logger.error("Error in export job JetStream loop", it)
                    delay(3_000)
                }
            }
        }
    }

    private suspend fun process(message: Message) {
        runCatching {
            exportJobService.processDispatchMessage(objectMapper.readValue<ExportJobDispatchMessage>(message.data).batchId)
            message.ack()
        }.onFailure {
            logger.error("Export job dispatch processing failed", it)
            message.nakWithDelay(Duration.ofSeconds(5))
        }
    }

    @PreDestroy
    fun cleanup() {
        scope.cancel()
        if (::subscription.isInitialized && subscription.isActive) subscription.unsubscribe()
    }
}
