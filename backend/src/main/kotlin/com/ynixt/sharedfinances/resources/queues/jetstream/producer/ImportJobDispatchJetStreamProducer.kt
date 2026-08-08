package com.ynixt.sharedfinances.resources.queues.jetstream.producer

import com.ynixt.sharedfinances.domain.models.imports.ImportJobDispatchMessage
import com.ynixt.sharedfinances.domain.queue.producer.ImportJobDispatchQueueProducer
import com.ynixt.sharedfinances.resources.queues.jetstream.JetStreamConstants.IMPORT_JOB_DISPATCH_STREAM
import com.ynixt.sharedfinances.resources.queues.jetstream.JetStreamConstants.IMPORT_JOB_DISPATCH_SUBJECT
import io.nats.client.Connection
import io.nats.client.JetStream
import io.nats.client.api.RetentionPolicy
import io.nats.client.api.StorageType
import io.nats.client.api.StreamConfiguration
import io.nats.client.impl.NatsMessage
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Component
class ImportJobDispatchJetStreamProducer(
    private val natsConnection: Connection,
    private val objectMapper: ObjectMapper,
) : ImportJobDispatchQueueProducer {
    private val jetStream: JetStream by lazy { natsConnection.jetStream() }

    @PostConstruct
    fun setupStream() {
        val management = natsConnection.jetStreamManagement()
        val configuration =
            StreamConfiguration
                .builder()
                .name(IMPORT_JOB_DISPATCH_STREAM)
                .subjects(IMPORT_JOB_DISPATCH_SUBJECT)
                .storageType(StorageType.File)
                .retentionPolicy(RetentionPolicy.WorkQueue)
                .build()
        if (!management.streamNames.contains(IMPORT_JOB_DISPATCH_STREAM)) {
            management.addStream(configuration)
        }
    }

    override fun send(batchId: UUID) {
        val message =
            NatsMessage
                .builder()
                .subject(IMPORT_JOB_DISPATCH_SUBJECT)
                .data(objectMapper.writeValueAsBytes(ImportJobDispatchMessage(batchId)))
                .build()
        jetStream.publish(message)
    }
}
