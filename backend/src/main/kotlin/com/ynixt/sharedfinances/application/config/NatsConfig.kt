package com.ynixt.sharedfinances.application.config

import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class NatsConfig {
    @Value($$"${app.nats.url}")
    private lateinit var natsUrl: String

    @Bean(destroyMethod = "close")
    fun natsConnection(): Connection {
        val options =
            Options
                .Builder()
                .server(natsUrl)
                .userInfo(System.getenv("NATS_APP_USER"), System.getenv("NATS_APP_PASSWORD"))
                .maxReconnects(-1)
                .reconnectWait(Duration.ofSeconds(2))
                .build()
        return Nats.connect(options)
    }
}
