package com.ynixt.sharedfinances.resources.queues.jetstream.listener

import com.ynixt.sharedfinances.application.web.dto.GenerateEntryRecurrenceRequestDto
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryCreateService
import com.ynixt.sharedfinances.resources.queues.jetstream.JetStreamConstants.GENERATE_ENTRY_RECURRENCE_DLQ_QUEUE
import com.ynixt.sharedfinances.resources.queues.jetstream.JetStreamConstants.GENERATE_ENTRY_RECURRENCE_QUEUE
import io.nats.client.Connection
import io.nats.client.JetStream
import io.nats.client.JetStreamSubscription
import io.nats.client.Message
import io.nats.client.PullSubscribeOptions
import io.nats.client.api.AckPolicy
import io.nats.client.api.ConsumerConfiguration
import io.nats.client.impl.Headers
import io.nats.client.impl.NatsMessage
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
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

@Component
class GenerateEntryRecurrenceQueueListener(
    private val natsConnection: Connection,
    private val walletEntryCreateService: WalletEntryCreateService,
    private val objectMapper: ObjectMapper,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val logger = LoggerFactory.getLogger(GenerateEntryRecurrenceQueueListener::class.java)

    private val maxAttempts = 3L
    private val delayBetweenAttempts = Duration.ofSeconds(10)

    private lateinit var subscription: JetStreamSubscription

    private var gracefulShutdownInProgress = false

    @EventListener(ApplicationReadyEvent::class)
    fun startListening() {
        val js = natsConnection.jetStream()

        val pullOptions =
            PullSubscribeOptions
                .builder()
                .durable("entry-recurrence-consumer")
                .configuration(
                    ConsumerConfiguration
                        .builder()
                        .ackWait(Duration.ofMinutes(1))
                        .maxDeliver(maxAttempts)
                        .ackPolicy(AckPolicy.Explicit)
                        .build(),
                ).build()

        subscription = js.subscribe(GENERATE_ENTRY_RECURRENCE_QUEUE, pullOptions)

        logger.info("JetStream Pull Consumer started for $GENERATE_ENTRY_RECURRENCE_QUEUE")

        startWorker(js)
    }

    private fun startWorker(js: JetStream) {
        scope.launch {
            while (isActive) {
                try {
                    val msgs: List<Message> = subscription.fetch(5, Duration.ofSeconds(1))

                    val jobs =
                        msgs.map { msg ->
                            launch {
                                processMessageSafe(js, msg)
                            }
                        }

                    jobs.joinAll()
                } catch (e: Exception) {
                    if (gracefulShutdownInProgress) {
                        break
                    }

                    logger.error("Error in JetStream consumer loop: ${e.message}", e)
                    delay(3000.milliseconds)
                }
            }
        }
    }

    private suspend fun processMessageSafe(
        js: JetStream,
        msg: Message,
    ) {
        try {
            if (shouldDiscardOrDlqBeforeProcess(js, msg)) {
                return
            }

            val request = objectMapper.readValue<GenerateEntryRecurrenceRequestDto>(msg.data)

            walletEntryCreateService
                .createFromRecurrenceConfig(
                    recurrenceConfigId = request.entryRecurrenceConfigId,
                    date = request.date,
                )

            msg.ack()
        } catch (e: Exception) {
            logger.error("Error processing message: ${e.message}", e)
            handleProcessingFailure(js, msg, e)
        }
    }

    private fun shouldDiscardOrDlqBeforeProcess(
        js: JetStream,
        msg: Message,
    ): Boolean {
        val deliveries = msg.metaData()?.deliveredCount() ?: 1

        if (deliveries > maxAttempts) {
            val e = Exception("Max delivery attempts exceeded ($deliveries)")
            handleProcessingFailure(js, msg, e)
            return true
        }

        return false
    }

    private fun handleProcessingFailure(
        js: JetStream,
        msg: Message,
        e: Exception,
    ) {
        val deliveries = msg.metaData()?.deliveredCount() ?: 1

        if (deliveries < maxAttempts) {
            logger.warn("Processing failed (attempt $deliveries/$maxAttempts). Retrying in ${delayBetweenAttempts.seconds}s: ${e.message}")
            msg.nakWithDelay(delayBetweenAttempts)
        } else {
            logger.error("Max attempts reached ($deliveries). Moving to DLQ: ${e.message}", e)
            try {
                publishToDlq(js, msg, deliveries)
                msg.ack()
            } catch (dlqErr: Exception) {
                logger.error("CRITICAL: Failed to publish to DLQ. Retrying...", dlqErr)
                msg.nakWithDelay(Duration.ofSeconds(5))
            }
        }
    }

    private fun publishToDlq(
        js: JetStream,
        originalMsg: Message,
        deliveries: Long,
    ) {
        val md = originalMsg.metaData()

        val headers = Headers()
        headers.add("x-original-subject", originalMsg.subject)
        headers.add("x-failed-at", Instant.now().toString())
        headers.add("x-deliveries", deliveries.toString())

        if (md != null) {
            headers.add("x-stream", md.stream)
            headers.add("x-consumer", md.consumer)
            headers.add("x-stream-seq", md.streamSequence().toString())
        }

        val dlqMsg =
            NatsMessage
                .builder()
                .subject(GENERATE_ENTRY_RECURRENCE_DLQ_QUEUE)
                .headers(headers)
                .data(originalMsg.data)
                .build()

        js.publish(dlqMsg)
    }

    @PreDestroy
    fun cleanup() {
        logger.info("Stopping JetStream consumer...")

        gracefulShutdownInProgress = true

        scope.cancel()

        try {
            if (::subscription.isInitialized && subscription.isActive) {
                subscription.unsubscribe()
            }
        } catch (e: Exception) {
            logger.warn("Error closing subscription: ${e.message}")
        }
    }
}
