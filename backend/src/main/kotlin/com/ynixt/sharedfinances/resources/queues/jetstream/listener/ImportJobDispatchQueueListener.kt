package com.ynixt.sharedfinances.resources.queues.jetstream.listener

import com.ynixt.sharedfinances.domain.models.imports.ImportJobDispatchMessage
import com.ynixt.sharedfinances.domain.services.imports.ImportJobService
import com.ynixt.sharedfinances.resources.queues.jetstream.JetStreamConstants.IMPORT_JOB_DISPATCH_SUBJECT
import com.ynixt.sharedfinances.resources.queues.jetstream.JetStreamConstants.IMPORT_JOB_WORKER_CONSUMER
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
import kotlin.time.Duration.Companion.seconds

@Component
@ConditionalOnProperty(name = ["app.imports.worker.enabled"], havingValue = "true", matchIfMissing = true)
class ImportJobDispatchQueueListener(
    private val natsConnection: Connection,
    private val objectMapper: ObjectMapper,
    private val importJobService: ImportJobService,
) {
    private val logger = LoggerFactory.getLogger(ImportJobDispatchQueueListener::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var subscription: JetStreamSubscription
    private var gracefulShutdownInProgress = false

    @EventListener(ApplicationReadyEvent::class)
    fun startListening() {
        val options =
            PullSubscribeOptions
                .builder()
                .durable(IMPORT_JOB_WORKER_CONSUMER)
                .configuration(
                    ConsumerConfiguration
                        .builder()
                        .ackPolicy(AckPolicy.Explicit)
                        .ackWait(Duration.ofMinutes(2))
                        .build(),
                ).build()
        subscription = natsConnection.jetStream().subscribe(IMPORT_JOB_DISPATCH_SUBJECT, options)

        scope.launch {
            while (isActive) {
                try {
                    subscription
                        .fetch(5, Duration.ofSeconds(1))
                        .map { message -> launch { processMessage(message) } }
                        .joinAll()
                } catch (error: Exception) {
                    if (gracefulShutdownInProgress) break
                    logger.error("Error in import job JetStream loop", error)
                    delay(3.seconds)
                }
            }
        }
    }

    private suspend fun processMessage(message: Message) {
        try {
            val payload = objectMapper.readValue<ImportJobDispatchMessage>(message.data)
            importJobService.processDispatchMessage(payload.batchId)
            message.ack()
        } catch (error: Exception) {
            logger.error("Import job dispatch processing failed", error)
            message.nakWithDelay(Duration.ofSeconds(5))
        }
    }

    @PreDestroy
    fun cleanup() {
        gracefulShutdownInProgress = true
        scope.cancel()
        runCatching {
            if (::subscription.isInitialized && subscription.isActive) subscription.unsubscribe()
        }.onFailure { error -> logger.warn("Error stopping import job listener", error) }
    }
}
