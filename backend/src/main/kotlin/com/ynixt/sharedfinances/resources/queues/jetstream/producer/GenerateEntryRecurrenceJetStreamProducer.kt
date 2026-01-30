package com.ynixt.sharedfinances.resources.queues.jetstream.producer

import com.ynixt.sharedfinances.application.web.dto.GenerateEntryRecurrenceRequestDto
import com.ynixt.sharedfinances.domain.queue.producer.GenerateEntryRecurrenceQueueProducer
import com.ynixt.sharedfinances.resources.queues.jetstream.JetStreamConstants.ENTRY_RECURRENCE_DLQ_STREAM
import com.ynixt.sharedfinances.resources.queues.jetstream.JetStreamConstants.ENTRY_RECURRENCE_STREAM
import com.ynixt.sharedfinances.resources.queues.jetstream.JetStreamConstants.GENERATE_ENTRY_RECURRENCE_DLQ_QUEUE
import com.ynixt.sharedfinances.resources.queues.jetstream.JetStreamConstants.GENERATE_ENTRY_RECURRENCE_QUEUE
import io.nats.client.Connection
import io.nats.client.JetStream
import io.nats.client.api.RetentionPolicy
import io.nats.client.api.StorageType
import io.nats.client.api.StreamConfiguration
import io.nats.client.impl.NatsMessage
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.Duration

@Component
class GenerateEntryRecurrenceJetStreamProducer(
    private val natsConnection: Connection,
    private val objectMapper: ObjectMapper,
) : GenerateEntryRecurrenceQueueProducer {
    private val js: JetStream by lazy { natsConnection.jetStream() }

    @PostConstruct
    fun setupStream() {
        val jsm = natsConnection.jetStreamManagement()

        val streamConfig =
            StreamConfiguration
                .builder()
                .name(ENTRY_RECURRENCE_STREAM)
                .subjects(GENERATE_ENTRY_RECURRENCE_QUEUE)
                .storageType(StorageType.File)
                .retentionPolicy(RetentionPolicy.WorkQueue)
                .build()

        val dlqStreamConfig =
            StreamConfiguration
                .builder()
                .name(ENTRY_RECURRENCE_DLQ_STREAM)
                .subjects(GENERATE_ENTRY_RECURRENCE_DLQ_QUEUE)
                .storageType(StorageType.File)
                .retentionPolicy(RetentionPolicy.WorkQueue)
                .maxAge(Duration.ofDays(7))
                .build()

        if (!jsm.streamNames.contains(ENTRY_RECURRENCE_STREAM)) {
            jsm.addStream(streamConfig)
        }

        if (!jsm.streamNames.contains(ENTRY_RECURRENCE_DLQ_STREAM)) {
            jsm.addStream(dlqStreamConfig)
        }
    }

    override fun send(request: GenerateEntryRecurrenceRequestDto) {
        val payload = objectMapper.writeValueAsBytes(request)

        val msg =
            NatsMessage
                .builder()
                .subject(GENERATE_ENTRY_RECURRENCE_QUEUE)
                .data(payload)
                .build()

        js.publish(msg)
    }
}
