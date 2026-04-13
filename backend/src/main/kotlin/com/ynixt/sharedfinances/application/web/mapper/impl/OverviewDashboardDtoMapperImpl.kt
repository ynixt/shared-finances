package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.dashboard.OverviewDashboardCardDto
import com.ynixt.sharedfinances.application.web.dto.dashboard.OverviewDashboardChartPointDto
import com.ynixt.sharedfinances.application.web.dto.dashboard.OverviewDashboardChartsDto
import com.ynixt.sharedfinances.application.web.dto.dashboard.OverviewDashboardDetailDto
import com.ynixt.sharedfinances.application.web.dto.dashboard.OverviewDashboardDto
import com.ynixt.sharedfinances.application.web.dto.dashboard.OverviewDashboardPieSliceDto
import com.ynixt.sharedfinances.application.web.mapper.OverviewDashboardDtoMapper
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboard
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCard
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardChartPoint
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCharts
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardDetail
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardPieSlice
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie
import tech.mappie.api.builtin.collections.IterableToListMapper
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Component
class OverviewDashboardDtoMapperImpl : OverviewDashboardDtoMapper {
    override fun toDto(model: OverviewDashboard): OverviewDashboardDto = ToDtoMapper.map(model)

    private object ToDtoMapper : ObjectMappie<OverviewDashboard, OverviewDashboardDto>() {
        override fun map(from: OverviewDashboard) =
            mapping {
                to::selectedMonth fromProperty from::selectedMonth transform { formatMonth(it) }
                to::cards fromProperty from::cards via IterableToListMapper(CardMapper)
                to::charts fromProperty from::charts via ChartsMapper
                to::goalCommittedTotal fromProperty from::goalCommittedTotal
                to::freeBalanceTotal fromProperty from::freeBalanceTotal
                to::goalOverCommittedWarning fromProperty from::goalOverCommittedWarning
            }
    }

    private object CardMapper : ObjectMappie<OverviewDashboardCard, OverviewDashboardCardDto>() {
        override fun map(from: OverviewDashboardCard) =
            mapping {
                to::key fromProperty from::key transform { it.name }
                to::details fromProperty from::details via IterableToListMapper(DetailMapper)
            }
    }

    private object DetailMapper : ObjectMappie<OverviewDashboardDetail, OverviewDashboardDetailDto>() {
        override fun map(from: OverviewDashboardDetail) =
            mapping {
                to::sourceType fromProperty from::sourceType transform { it.name }
                to::children fromProperty from::children via IterableToListMapper(DetailMapper)
            }
    }

    private object ChartsMapper : ObjectMappie<OverviewDashboardCharts, OverviewDashboardChartsDto>() {
        override fun map(from: OverviewDashboardCharts) =
            mapping {
                to::balance fromProperty from::balance via IterableToListMapper(ChartPointMapper)
                to::cashIn fromProperty from::cashIn via IterableToListMapper(ChartPointMapper)
                to::cashOut fromProperty from::cashOut via IterableToListMapper(ChartPointMapper)
                to::expense fromProperty from::expense via IterableToListMapper(ChartPointMapper)
                to::cashInByCategory fromProperty from::cashInByCategory via IterableToListMapper(PieSliceMapper)
                to::cashOutByCategory fromProperty from::cashOutByCategory via IterableToListMapper(PieSliceMapper)
                to::expenseByGroup fromProperty from::expenseByGroup via IterableToListMapper(PieSliceMapper)
                to::expenseByCategory fromProperty from::expenseByCategory via IterableToListMapper(PieSliceMapper)
            }
    }

    private object ChartPointMapper : ObjectMappie<OverviewDashboardChartPoint, OverviewDashboardChartPointDto>() {
        override fun map(from: OverviewDashboardChartPoint) =
            mapping {
                to::month fromProperty from::month transform { formatMonth(it) }
            }
    }

    private object PieSliceMapper : ObjectMappie<OverviewDashboardPieSlice, OverviewDashboardPieSliceDto>() {
        override fun map(from: OverviewDashboardPieSlice) =
            mapping {
                to::id fromProperty from::id
                to::label fromProperty from::label
                to::value fromProperty from::value
            }
    }

    companion object {
        private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-yyyy")

        private fun formatMonth(month: YearMonth): String = month.format(monthFormatter)
    }
}
