package com.ynixt.sharedfinances.resources.queues.jetstream.listener

import com.ynixt.sharedfinances.application.web.dto.GenerateEntryRecurrenceRequestDto
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryCreateService
import com.ynixt.sharedfinances.resources.queues.jetstream.JetStreamConstants.GENERATE_ENTRY_RECURRENCE_DLQ_QUEUE
import com.ynixt.sharedfinances.resources.queues.jetstream.JetStreamConstants.GENERATE_ENTRY_RECURRENCE_QUEUE
import io.nats.client.Connection
import io.nats.client.JetStream
import io.nats.client.Message
import io.nats.client.PushSubscribeOptions
import io.nats.client.api.AckPolicy
import io.nats.client.api.ConsumerConfiguration
import io.nats.client.impl.Headers
import io.nats.client.impl.NatsMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.Duration
import java.time.Instant

@org.springframework.stereotype.Component
class GenerateEntryRecurrenceQueueListener(
    private val natsConnection: Connection,
    private val walletEntryCreateService: WalletEntryCreateService,
    private val objectMapper: ObjectMapper,
) {
    private val scope =
        CoroutineScope(
            Dispatchers.IO + SupervisorJob(),
        )
    private val logger =
        LoggerFactory
            .getLogger(GenerateEntryRecurrenceQueueListener::class.java)

    private val maxAttempts = 3
    private val delayBetweenAttempts = Duration.ofSeconds(10)

    @EventListener(ApplicationReadyEvent::class)
    fun startListening() {
        val js = natsConnection.jetStream()
        val dispatcher = natsConnection.createDispatcher { }

        val options =
            PushSubscribeOptions
                .builder()
                .durable("entry-recurrence-consumer")
                .configuration(
                    ConsumerConfiguration
                        .builder()
                        .ackWait(
                            Duration.ofMinutes(1),
                        ).maxDeliver(maxAttempts.toLong())
                        .ackPolicy(AckPolicy.Explicit)
                        .build(),
                ).build()

        js.subscribe(GENERATE_ENTRY_RECURRENCE_QUEUE, dispatcher, { msg ->
            scope.launch {
                try {
                    processorMessage(msg)
                    msg.ack()
                } catch (e: Exception) {
                    handleFailure(js, msg, e)
                }
            }
        }, false, options)
    }

    private suspend fun processorMessage(msg: Message) {
        val request = objectMapper.readValue<GenerateEntryRecurrenceRequestDto>(msg.data)

        walletEntryCreateService
            .createFromRecurrenceConfig(
                recurrenceConfigId = request.entryRecurrenceConfigId,
                date = request.date,
            ).awaitSingle()
    }

    private fun handleFailure(
        js: JetStream,
        msg: Message,
        e: Exception,
    ) {
        val deliveries = runCatching { msg.metaData()?.deliveredCount() ?: 1 }.getOrDefault(1)

        if (deliveries < maxAttempts) {
            msg.nakWithDelay(delayBetweenAttempts)
            logger.error(
                "Error processing message (attempt $deliveries/$maxAttempts). Will retry in ${delayBetweenAttempts.seconds}s: ${e.message}",
                e,
            )
            return
        }

        try {
            publishToDlq(js, msg, e, deliveries)
            msg.ack()
            logger.error(
                "Error processing message (attempt $deliveries/$maxAttempts). Sent to DLQ and ACKed original: ${e.message}",
                e,
            )
        } catch (dlqErr: Exception) {
            msg.nakWithDelay(Duration.ofSeconds(5))
            logger.error("Failed to publish message to DLQ. Will retry DLQ publish: ${dlqErr.message}", dlqErr)
        }
    }

    private fun publishToDlq(
        js: JetStream,
        originalMsg: Message,
        e: Exception,
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
}
