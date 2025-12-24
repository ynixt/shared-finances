package com.ynixt.sharedfinances.application.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

@Configuration
class S3Config(
    @param:Value("\${app.s3.endpoint}") private val endpoint: String,
    @param:Value("\${app.s3.accessKey}") private val accessKey: String,
    @param:Value("\${app.s3.secretKey}") private val secretKey: String,
    @param:Value("\${app.s3.region}") private val region: String,
    @param:Value("\${app.s3.pathStyleAccessEnabled}") private val pathStyleAccessEnabled: Boolean,
) {
    @Bean
    fun s3AsyncClient(): S3AsyncClient {
        val creds = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))

        return S3AsyncClient
            .builder()
            .credentialsProvider(creds)
            .region(Region.of(region))
            .endpointOverride(URI.create(endpoint))
            .serviceConfiguration(
                S3Configuration
                    .builder()
                    .pathStyleAccessEnabled(pathStyleAccessEnabled)
                    .build(),
            ).build()
    }

    @Configuration
    class S3PresignerConfig(
        @param:Value("\${app.s3.endpoint}") private val endpoint: String,
        @param:Value("\${app.s3.accessKey}") private val accessKey: String,
        @param:Value("\${app.s3.secretKey}") private val secretKey: String,
        @param:Value("\${app.s3.region}") private val region: String,
        @param:Value("\${app.s3.pathStyleAccessEnabled}") private val pathStyleAccessEnabled: Boolean,
    ) {
        @Bean
        fun s3Presigner(): S3Presigner {
            val creds =
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey),
                )

            return S3Presigner
                .builder()
                .credentialsProvider(creds)
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .serviceConfiguration(
                    S3Configuration
                        .builder()
                        .pathStyleAccessEnabled(pathStyleAccessEnabled)
                        .build(),
                ).build()
        }
    }
}
