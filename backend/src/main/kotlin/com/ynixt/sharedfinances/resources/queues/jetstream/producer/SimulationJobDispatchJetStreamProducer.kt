package com.ynixt.sharedfinances.resources.queues.jetstream.producer

import com.ynixt.sharedfinances.domain.models.simulation.SimulationJobDispatchMessage
import com.ynixt.sharedfinances.domain.queue.producer.SimulationJobDispatchQueueProducer
import com.ynixt.sharedfinances.resources.queues.jetstream.JetStreamConstants.SIMULATION_JOB_DISPATCH_STREAM
import com.ynixt.sharedfinances.resources.queues.jetstream.JetStreamConstants.SIMULATION_JOB_DISPATCH_SUBJECT
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
class SimulationJobDispatchJetStreamProducer(
    private val natsConnection: Connection,
    private val objectMapper: ObjectMapper,
) : SimulationJobDispatchQueueProducer {
    private val js: JetStream by lazy { natsConnection.jetStream() }

    @PostConstruct
    fun setupStream() {
        val jsm = natsConnection.jetStreamManagement()
        val streamConfig =
            StreamConfiguration
                .builder()
                .name(SIMULATION_JOB_DISPATCH_STREAM)
                .subjects(SIMULATION_JOB_DISPATCH_SUBJECT)
                .storageType(StorageType.File)
                .retentionPolicy(RetentionPolicy.WorkQueue)
                .build()

        if (!jsm.streamNames.contains(SIMULATION_JOB_DISPATCH_STREAM)) {
            jsm.addStream(streamConfig)
        }
    }

    override fun send(jobId: UUID) {
        val payload =
            objectMapper.writeValueAsBytes(
                SimulationJobDispatchMessage(jobId = jobId),
            )

        val msg =
            NatsMessage
                .builder()
                .subject(SIMULATION_JOB_DISPATCH_SUBJECT)
                .data(payload)
                .build()

        js.publish(msg)
    }
}
