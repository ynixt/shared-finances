package com.ynixt.sharedfinances.application.config

import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties("app.plan")
data class PlanProperties(
    val defaultRole: UserPlanRole = UserPlanRole.PRO,
    val limitCacheTtl: Duration = Duration.ofMinutes(5),
    val enabled: Boolean = false,
)
