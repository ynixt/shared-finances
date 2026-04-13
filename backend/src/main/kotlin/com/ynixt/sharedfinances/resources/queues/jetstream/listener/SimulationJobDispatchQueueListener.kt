package com.ynixt.sharedfinances.resources.queues.jetstream.listener

import com.ynixt.sharedfinances.domain.models.simulation.SimulationJobDispatchMessage
import com.ynixt.sharedfinances.domain.services.simulation.SimulationJobService
import com.ynixt.sharedfinances.resources.queues.jetstream.JetStreamConstants.SIMULATION_JOB_DISPATCH_SUBJECT
import com.ynixt.sharedfinances.resources.queues.jetstream.JetStreamConstants.SIMULATION_JOB_WORKER_CONSUMER
import io.nats.client.Connection
import io.nats.client.JetStreamSubscription
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
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.r2dbc.BadSqlGrammarException
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.Duration

@Component
class SimulationJobDispatchQueueListener(
    private val natsConnection: Connection,
    private val objectMapper: ObjectMapper,
    private val simulationJobService: SimulationJobService,
) {
    private val logger = LoggerFactory.getLogger(SimulationJobDispatchQueueListener::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var subscription: JetStreamSubscription

    @EventListener(ApplicationReadyEvent::class)
    fun startListening() {
        val js = natsConnection.jetStream()

        val pullOptions =
            PullSubscribeOptions
                .builder()
                .durable(SIMULATION_JOB_WORKER_CONSUMER)
                .configuration(
                    ConsumerConfiguration
                        .builder()
                        .ackPolicy(AckPolicy.Explicit)
                        .ackWait(Duration.ofMinutes(2))
                        .build(),
                ).build()

        subscription = js.subscribe(SIMULATION_JOB_DISPATCH_SUBJECT, pullOptions)

        scope.launch {
            while (isActive) {
                try {
                    val messages = subscription.fetch(8, Duration.ofSeconds(1))
                    for (msg in messages) {
                        launch { processMessage(msg) }
                    }
                } catch (_: InterruptedException) {
                    break
                } catch (e: IllegalStateException) {
                    if (e.message?.contains("inactive", ignoreCase = true) == true) {
                        break
                    }
                    logger.error("Error in simulation job JetStream loop: ${e.message}", e)
                    delay(2000)
                } catch (e: Exception) {
                    logger.error("Error in simulation job JetStream loop: ${e.message}", e)
                    delay(2000)
                }
            }
        }
    }

    private suspend fun processMessage(msg: io.nats.client.Message) {
        try {
            val payload = objectMapper.readValue<SimulationJobDispatchMessage>(msg.data)
            simulationJobService.processDispatchMessage(payload.jobId)
            msg.ack()
        } catch (e: BadSqlGrammarException) {
            logger.warn("Discarding simulation job message due schema unavailability: ${e.message}")
            msg.ack()
        } catch (e: Exception) {
            logger.error("Simulation job dispatch processing failed: ${e.message}", e)
            msg.nakWithDelay(Duration.ofSeconds(5))
        }
    }

    @PreDestroy
    fun cleanup() {
        scope.cancel()
        try {
            if (::subscription.isInitialized && subscription.isActive) {
                subscription.unsubscribe()
            }
        } catch (e: Exception) {
            logger.warn("Error stopping simulation job listener: ${e.message}")
        }
    }
}
