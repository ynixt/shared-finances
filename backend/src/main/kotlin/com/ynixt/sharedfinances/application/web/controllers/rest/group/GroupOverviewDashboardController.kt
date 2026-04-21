package com.ynixt.sharedfinances.application.web.controllers.rest.group

import com.ynixt.sharedfinances.application.web.dto.dashboard.GroupOverviewDashboardDto
import com.ynixt.sharedfinances.application.web.mapper.GroupOverviewDashboardDtoMapper
import com.ynixt.sharedfinances.domain.exceptions.http.GroupNotFoundException
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidOverviewMonthException
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.dashboard.OverviewDashboardService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

@RestController
@RequestMapping("/groups/{groupId}/dashboard")
@Tag(name = "Group dashboard", description = "Operations related to group finances dashboard overview")
class GroupOverviewDashboardController(
    private val overviewDashboardService: OverviewDashboardService,
    private val groupOverviewDashboardDtoMapper: GroupOverviewDashboardDtoMapper,
    private val groupService: GroupService,
    private val clock: Clock,
) {
    private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-yyyy")

    @Operation(summary = "Get group overview dashboard")
    @GetMapping("/overview")
    suspend fun getOverview(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        @RequestParam(required = false) month: String?,
    ): GroupOverviewDashboardDto {
        val userId = principalToken.principal.id
        val selectedMonth = parseSelectedMonth(month)

        groupService.findGroup(userId = userId, id = groupId) ?: throw GroupNotFoundException(groupId)

        return overviewDashboardService
            .getGroupOverview(
                userId = userId,
                groupId = groupId,
                defaultCurrency = principalToken.principal.defaultCurrency,
                selectedMonth = selectedMonth,
            ).let(groupOverviewDashboardDtoMapper::toDto)
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
