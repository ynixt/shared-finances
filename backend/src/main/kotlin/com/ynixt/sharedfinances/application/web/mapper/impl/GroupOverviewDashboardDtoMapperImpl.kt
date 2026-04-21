package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.dashboard.GroupOverviewDashboardChartsDto
import com.ynixt.sharedfinances.application.web.dto.dashboard.GroupOverviewDashboardDto
import com.ynixt.sharedfinances.application.web.dto.dashboard.GroupOverviewDashboardMemberPieDto
import com.ynixt.sharedfinances.application.web.dto.dashboard.GroupOverviewDashboardMemberSeriesDto
import com.ynixt.sharedfinances.application.web.dto.dashboard.GroupOverviewDashboardSeriesDto
import com.ynixt.sharedfinances.application.web.dto.dashboard.GroupOverviewDebtPairDto
import com.ynixt.sharedfinances.application.web.dto.dashboard.OverviewDashboardCardDto
import com.ynixt.sharedfinances.application.web.dto.dashboard.OverviewDashboardChartPointDto
import com.ynixt.sharedfinances.application.web.dto.dashboard.OverviewDashboardDetailDto
import com.ynixt.sharedfinances.application.web.dto.dashboard.OverviewDashboardPieSliceDto
import com.ynixt.sharedfinances.application.web.mapper.GroupOverviewDashboardDtoMapper
import com.ynixt.sharedfinances.domain.models.dashboard.GroupOverviewDashboard
import com.ynixt.sharedfinances.domain.models.dashboard.GroupOverviewDashboardCharts
import com.ynixt.sharedfinances.domain.models.dashboard.GroupOverviewDashboardMemberPie
import com.ynixt.sharedfinances.domain.models.dashboard.GroupOverviewDashboardMemberSeries
import com.ynixt.sharedfinances.domain.models.dashboard.GroupOverviewDashboardSeries
import com.ynixt.sharedfinances.domain.models.dashboard.GroupOverviewDebtPair
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCard
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardChartPoint
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardDetail
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardPieSlice
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie
import tech.mappie.api.builtin.collections.IterableToListMapper
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Component
class GroupOverviewDashboardDtoMapperImpl : GroupOverviewDashboardDtoMapper {
    override fun toDto(model: GroupOverviewDashboard): GroupOverviewDashboardDto = ToDtoMapper.map(model)

    private object ToDtoMapper : ObjectMappie<GroupOverviewDashboard, GroupOverviewDashboardDto>() {
        override fun map(from: GroupOverviewDashboard) =
            mapping {
                to::selectedMonth fromProperty from::selectedMonth transform { formatMonth(it) }
                to::cards fromProperty from::cards via IterableToListMapper(CardMapper)
                to::charts fromProperty from::charts via ChartsMapper
                to::debtPairs fromProperty from::debtPairs via IterableToListMapper(DebtPairMapper)
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

    private object ChartsMapper : ObjectMappie<GroupOverviewDashboardCharts, GroupOverviewDashboardChartsDto>() {
        override fun map(from: GroupOverviewDashboardCharts) =
            mapping {
                to::cashIn fromProperty from::cashIn via SeriesMapper
                to::expense fromProperty from::expense via SeriesMapper
                to::cashInByCategoryTotal fromProperty from::cashInByCategoryTotal via IterableToListMapper(PieSliceMapper)
                to::cashInByCategoryByMember fromProperty from::cashInByCategoryByMember via IterableToListMapper(MemberPieMapper)
                to::expenseByCategory fromProperty from::expenseByCategory via IterableToListMapper(PieSliceMapper)
                to::expenseByCategoryByMember fromProperty from::expenseByCategoryByMember via IterableToListMapper(MemberPieMapper)
                to::expenseByMember fromProperty from::expenseByMember via IterableToListMapper(PieSliceMapper)
            }
    }

    private object SeriesMapper : ObjectMappie<GroupOverviewDashboardSeries, GroupOverviewDashboardSeriesDto>() {
        override fun map(from: GroupOverviewDashboardSeries) =
            mapping {
                to::total fromProperty from::total via IterableToListMapper(ChartPointMapper)
                to::byMember fromProperty from::byMember via IterableToListMapper(MemberSeriesMapper)
            }
    }

    private object MemberSeriesMapper : ObjectMappie<GroupOverviewDashboardMemberSeries, GroupOverviewDashboardMemberSeriesDto>() {
        override fun map(from: GroupOverviewDashboardMemberSeries) =
            mapping {
                to::points fromProperty from::points via IterableToListMapper(ChartPointMapper)
            }
    }

    private object MemberPieMapper : ObjectMappie<GroupOverviewDashboardMemberPie, GroupOverviewDashboardMemberPieDto>() {
        override fun map(from: GroupOverviewDashboardMemberPie) =
            mapping {
                to::slices fromProperty from::slices via IterableToListMapper(PieSliceMapper)
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
                to::executedValue fromProperty from::executedValue
                to::projectedValue fromProperty from::projectedValue
            }
    }

    private object DebtPairMapper : ObjectMappie<GroupOverviewDebtPair, GroupOverviewDebtPairDto>() {
        override fun map(from: GroupOverviewDebtPair) =
            mapping {
                to::details fromProperty from::details via IterableToListMapper(DetailMapper)
            }
    }

    companion object {
        private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-yyyy")

        private fun formatMonth(month: YearMonth): String = month.format(monthFormatter)
    }
}
