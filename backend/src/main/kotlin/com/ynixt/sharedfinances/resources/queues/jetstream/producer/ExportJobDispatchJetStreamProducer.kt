package com.ynixt.sharedfinances.resources.queues.jetstream.producer

import com.ynixt.sharedfinances.domain.models.exports.ExportJobDispatchMessage
import com.ynixt.sharedfinances.domain.queue.producer.ExportJobDispatchQueueProducer
import com.ynixt.sharedfinances.resources.queues.jetstream.JetStreamConstants.EXPORT_JOB_DISPATCH_STREAM
import com.ynixt.sharedfinances.resources.queues.jetstream.JetStreamConstants.EXPORT_JOB_DISPATCH_SUBJECT
import io.nats.client.Connection
import io.nats.client.api.RetentionPolicy
import io.nats.client.api.StorageType
import io.nats.client.api.StreamConfiguration
import io.nats.client.impl.NatsMessage
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Component
class ExportJobDispatchJetStreamProducer(
    private val natsConnection: Connection,
    private val objectMapper: ObjectMapper,
) : ExportJobDispatchQueueProducer {
    @PostConstruct
    fun setupStream() {
        val management = natsConnection.jetStreamManagement()
        if (!management.streamNames.contains(EXPORT_JOB_DISPATCH_STREAM)) {
            management.addStream(
                StreamConfiguration
                    .builder()
                    .name(EXPORT_JOB_DISPATCH_STREAM)
                    .subjects(EXPORT_JOB_DISPATCH_SUBJECT)
                    .storageType(StorageType.File)
                    .retentionPolicy(RetentionPolicy.WorkQueue)
                    .build(),
            )
        }
    }

    override fun send(batchId: UUID) {
        natsConnection.jetStream().publish(
            NatsMessage
                .builder()
                .subject(EXPORT_JOB_DISPATCH_SUBJECT)
                .data(objectMapper.writeValueAsBytes(ExportJobDispatchMessage(batchId)))
                .build(),
        )
    }
}
