package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.walletentry.EntryForListDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.EntrySumDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.EntrySummaryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.EntrySummaryGroupedDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.EntrySummaryGroupedResultDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.ListEntryRequestDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.NewEntryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.SummaryEntryRequestDto
import com.ynixt.sharedfinances.application.web.mapper.CategoryDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.GroupDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.UserDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.WalletEntryDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.WalletItemDtoMapper
import com.ynixt.sharedfinances.domain.models.CursorPageRequest
import com.ynixt.sharedfinances.domain.models.ListEntryRequest
import com.ynixt.sharedfinances.domain.models.SummaryEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.EntryListResponse
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySum
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySummary
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySummaryGrouped
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySummaryGroupedResult
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie
import tech.mappie.api.builtin.collections.IterableToListMapper

@Component
class WalletEntryDtoMapperImpl(
    userDtoMapper: UserDtoMapper,
    categoryDtoMapper: CategoryDtoMapper,
    groupDtoMapper: GroupDtoMapper,
    walletItemDtoMapper: WalletItemDtoMapper,
) : WalletEntryDtoMapper {
    private val entryListMapper = EntryListMapper(userDtoMapper, categoryDtoMapper, groupDtoMapper, walletItemDtoMapper)
    private val entrySummaryGroupedDtoMapper = EntrySummaryGroupedDtoMapper(walletItemDtoMapper)
    private val fromSummaryModelToDtoMapper = FromSummaryModelToDtoMapper(entrySummaryGroupedDtoMapper)

    override fun fromNewDtoToNewRequest(from: NewEntryDto): NewEntryRequest = FromNewDtoMapper.map(from)

    override fun fromListResponseToListDto(from: EntryListResponse): EntryForListDto = entryListMapper.map(from)

    override fun fromEntryListRequestDtoToModel(from: ListEntryRequestDto): ListEntryRequest =
        FromEntryListRequestDtoToModelMapper.map(from)

    override fun fromEntrySummaryRequestDtoToModel(from: SummaryEntryRequestDto): SummaryEntryRequest =
        FromEntrySummaryRequestDtoToModelMapper.map(from)

    override fun fromSummaryModelToDto(from: EntrySummary): EntrySummaryDto = fromSummaryModelToDtoMapper.map(from)

    private object FromNewDtoMapper : ObjectMappie<NewEntryDto, NewEntryRequest>() {
        override fun map(from: NewEntryDto) =
            mapping {
            }
    }

    private object FromEntryListRequestDtoToModelMapper : ObjectMappie<ListEntryRequestDto, ListEntryRequest>() {
        override fun map(from: ListEntryRequestDto) =
            mapping {
                to::pageRequest fromProperty from::pageRequest transform { it ?: CursorPageRequest() }
            }
    }

    private object FromEntrySummaryRequestDtoToModelMapper : ObjectMappie<SummaryEntryRequestDto, SummaryEntryRequest>() {
        override fun map(from: SummaryEntryRequestDto) =
            mapping {
            }
    }

    private class FromSummaryModelToDtoMapper(
        private val entrySummaryGroupedDtoMapper: EntrySummaryGroupedDtoMapper,
    ) : ObjectMappie<EntrySummary, EntrySummaryDto>() {
        override fun map(from: EntrySummary) =
            mapping {
                to::total fromProperty from::total via EntrySumDtoMapper
                to::totalProjected fromProperty from::totalProjected via EntrySumDtoMapper
                to::totalPeriod fromProperty from::totalPeriod via EntrySumDtoMapper
                to::grouped fromProperty from::grouped via IterableToListMapper(entrySummaryGroupedDtoMapper)
            }
    }

    private object EntrySumDtoMapper : ObjectMappie<EntrySum, EntrySumDto>() {
        override fun map(from: EntrySum) =
            mapping {
            }
    }

    private class EntrySummaryGroupedDtoMapper(
        private val walletItemDtoMapper: WalletItemDtoMapper,
    ) : ObjectMappie<EntrySummaryGrouped, EntrySummaryGroupedDto>() {
        override fun map(from: EntrySummaryGrouped) =
            mapping {
                to::walletItem fromProperty from::walletItem transform { walletItemDtoMapper.entityToWalletItemForEntryListDto(it) }
                to::entries fromProperty from::entries via IterableToListMapper(EntrySummaryGroupedResultDtoMapper)
            }
    }

    private object EntrySummaryGroupedResultDtoMapper : ObjectMappie<EntrySummaryGroupedResult, EntrySummaryGroupedResultDto>() {
        override fun map(from: EntrySummaryGroupedResult) =
            mapping {
                to::sum fromProperty from::sum via EntrySumDtoMapper
                to::projected fromProperty from::projected via EntrySumDtoMapper
                to::period fromProperty from::period via EntrySumDtoMapper
            }
    }

    private class EntryListMapper(
        private val userDtoMapper: UserDtoMapper,
        private val categoryDtoMapper: CategoryDtoMapper,
        private val groupDtoMapper: GroupDtoMapper,
        private val walletItemDtoMapper: WalletItemDtoMapper,
    ) : ObjectMappie<EntryListResponse, EntryForListDto>() {
        override fun map(from: EntryListResponse) =
            mapping {
                to::category fromProperty from::category transform { it?.let { categoryDtoMapper.toDto(it) } }
                to::user fromProperty from::user transform { it?.let { userDtoMapper.tSimpleDto(it) } }
                to::group fromProperty from::group transform { it?.let { groupDtoMapper.toDto(it) } }
                to::origin fromProperty from::origin transform { walletItemDtoMapper.entityToWalletItemForEntryListDto(it) }
                to::target fromProperty from::target transform { it?.let { walletItemDtoMapper.entityToWalletItemForEntryListDto(it) } }
            }
    }
}
