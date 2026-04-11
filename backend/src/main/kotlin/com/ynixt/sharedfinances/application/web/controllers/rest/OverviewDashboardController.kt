package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.dashboard.OverviewDashboardDto
import com.ynixt.sharedfinances.application.web.mapper.OverviewDashboardDtoMapper
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidOverviewMonthException
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.dashboard.OverviewDashboardService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@RestController
@RequestMapping("/dashboard")
@Tag(
    name = "Dashboard",
    description = "Operations related to finances dashboard overview",
)
class OverviewDashboardController(
    private val overviewDashboardService: OverviewDashboardService,
    private val overviewDashboardDtoMapper: OverviewDashboardDtoMapper,
    private val clock: Clock,
) {
    private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-yyyy")

    @Operation(summary = "Get overview dashboard")
    @GetMapping("/overview")
    suspend fun getOverview(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestParam(required = false) month: String?,
    ): OverviewDashboardDto {
        val selectedMonth = parseSelectedMonth(month)

        return overviewDashboardService
            .getOverview(
                userId = principalToken.principal.id,
                defaultCurrency = principalToken.principal.defaultCurrency,
                selectedMonth = selectedMonth,
            ).let(overviewDashboardDtoMapper::toDto)
    }

    private fun parseSelectedMonth(value: String?): YearMonth {
        if (value.isNullOrBlank()) {
            return YearMonth.now(clock)
        }

        return try {
            YearMonth.parse(value, monthFormatter)
        } catch (_: DateTimeParseException) {
            throw InvalidOverviewMonthException(value)
        }
    }
}
