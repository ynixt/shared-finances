package com.ynixt.sharedfinances.application.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("app.exports.retention")
data class ExportRetentionProperties(
    val afterDownload: Policy = Policy(delay = Duration.ofMinutes(5), cron = "0 */1 * * * *"),
    val absoluteAge: Policy = Policy(delay = Duration.ofHours(24), cron = "0 */10 * * * *"),
) {
    data class Policy(
        val enabled: Boolean = true,
        val delay: Duration,
        val cron: String,
    )
}
