package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.walletentry.DeleteScheduledEntryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.EditScheduledEntryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.EntrySumDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.EntrySummaryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.EntrySummaryGroupedDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.EntrySummaryGroupedResultDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.EventForListDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.ListEntryRequestDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.NewEntryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.ScheduledExecutionManagerRequestDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.SummaryEntryRequestDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.WalletBeneficiaryLegDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.WalletSourceLegDto
import com.ynixt.sharedfinances.application.web.mapper.WalletEntryDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.WalletItemDtoMapper
import com.ynixt.sharedfinances.domain.enums.TransferPurpose
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.models.CursorPageRequest
import com.ynixt.sharedfinances.domain.models.ListEntryRequest
import com.ynixt.sharedfinances.domain.models.SummaryEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.DeleteScheduledEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.EditScheduledEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySum
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySummary
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySummaryGrouped
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySummaryGroupedResult
import com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.NewWalletBeneficiaryLeg
import com.ynixt.sharedfinances.domain.models.walletentry.NewWalletSourceLeg
import com.ynixt.sharedfinances.domain.models.walletentry.ScheduledExecutionManagerRequest
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie
import tech.mappie.api.builtin.collections.IterableToListMapper

@Component
class WalletEntryDtoMapperImpl(
    walletItemDtoMapper: WalletItemDtoMapper,
) : WalletEntryDtoMapper {
    private val entrySummaryGroupedDtoMapper = EntrySummaryGroupedDtoMapper(walletItemDtoMapper)
    private val fromSummaryModelToDtoMapper = FromSummaryModelToDtoMapper(entrySummaryGroupedDtoMapper)
    private val fromEntryResponseToDtoMapper = FromEntryResponseToDtoMapper(walletItemDtoMapper)

    override fun fromNewDtoToNewRequest(from: NewEntryDto): NewEntryRequest =
        when (from.type) {
            WalletEntryType.TRANSFER -> {
                requireNotNull(from.originId) { "originId is required for transfer" }
                FromNewEntryDtoToRequestTransferMapper.map(from)
            }
            else -> {
                require(!from.sources.isNullOrEmpty()) { "sources is required for this entry type" }
                FromNewEntryDtoToRequestNonTransferMapper.map(from)
            }
        }

    override fun fromEntryListRequestDtoToModel(from: ListEntryRequestDto): ListEntryRequest =
        FromEntryListRequestDtoToModelMapper.map(from)

    override fun fromEntrySummaryRequestDtoToModel(from: SummaryEntryRequestDto): SummaryEntryRequest =
        FromEntrySummaryRequestDtoToModelMapper.map(from)

    override fun fromEditScheduledDtoToModel(from: EditScheduledEntryDto): EditScheduledEntryRequest =
        EditScheduledEntryRequest(
            occurrenceDate = from.occurrenceDate,
            scope = from.scope,
            entry = fromNewDtoToNewRequest(from.entry),
        )

    override fun fromDeleteScheduledDtoToModel(from: DeleteScheduledEntryDto): DeleteScheduledEntryRequest =
        FromDeleteScheduledDtoToModelMapper.map(from)

    override fun fromScheduledExecutionManagerRequestDtoToModel(
        from: ScheduledExecutionManagerRequestDto,
    ): ScheduledExecutionManagerRequest = FromScheduledExecutionManagerRequestDtoToModelMapper.map(from)

    override fun fromSummaryModelToDto(from: EntrySummary): EntrySummaryDto = fromSummaryModelToDtoMapper.map(from)

    override fun fromEntryResponseToDto(from: EventListResponse.EntryResponse): EventForListDto.EntryResponseDto =
        fromEntryResponseToDtoMapper.map(from)

    private object WalletSourceLegDtoToNewLegMapper : ObjectMappie<WalletSourceLegDto, NewWalletSourceLeg>() {
        override fun map(from: WalletSourceLegDto) =
            mapping {
            }
    }

    private object WalletBeneficiaryLegDtoToNewLegMapper : ObjectMappie<WalletBeneficiaryLegDto, NewWalletBeneficiaryLeg>() {
        override fun map(from: WalletBeneficiaryLegDto) =
            mapping {
            }
    }

    private object FromNewEntryDtoToRequestTransferMapper : ObjectMappie<NewEntryDto, NewEntryRequest>() {
        override fun map(from: NewEntryDto) =
            mapping {
                to::sources fromValue null
                to::beneficiaries fromValue null
                to::transferPurpose fromProperty from::transferPurpose transform { it ?: TransferPurpose.GENERAL }
            }
    }

    private object FromNewEntryDtoToRequestNonTransferMapper : ObjectMappie<NewEntryDto, NewEntryRequest>() {
        override fun map(from: NewEntryDto) =
            mapping {
                to::originId fromValue null
                to::sources fromProperty from::sources via IterableToListMapper(WalletSourceLegDtoToNewLegMapper)
                to::beneficiaries fromProperty from::beneficiaries via IterableToListMapper(WalletBeneficiaryLegDtoToNewLegMapper)
                to::transferPurpose fromValue TransferPurpose.GENERAL
            }
    }

    private object FromEntryListRequestDtoToModelMapper : ObjectMappie<ListEntryRequestDto, ListEntryRequest>() {
        override fun map(from: ListEntryRequestDto) =
            mapping {
                to::pageRequest fromProperty from::pageRequest transform { it ?: CursorPageRequest() }
                to::groupIds fromProperty from::groupIds transform { it?.toSet() ?: emptySet() }
                to::userIds fromProperty from::userIds transform { it?.toSet() ?: emptySet() }
                to::creditCardIds fromProperty from::creditCardIds transform { it?.toSet() ?: emptySet() }
                to::bankAccountIds fromProperty from::bankAccountIds transform { it?.toSet() ?: emptySet() }
                to::categoryIds fromProperty from::categoryIds transform { it?.toSet() ?: emptySet() }
                to::includeUncategorized fromProperty from::includeUncategorized transform { it ?: false }
                to::entryTypes fromProperty from::entryTypes transform { it?.toSet() ?: emptySet() }
            }
    }

    private object FromEntrySummaryRequestDtoToModelMapper : ObjectMappie<SummaryEntryRequestDto, SummaryEntryRequest>() {
        override fun map(from: SummaryEntryRequestDto) =
            mapping {
            }
    }

    private object FromDeleteScheduledDtoToModelMapper : ObjectMappie<DeleteScheduledEntryDto, DeleteScheduledEntryRequest>() {
        override fun map(from: DeleteScheduledEntryDto) =
            mapping {
            }
    }

    private object FromScheduledExecutionManagerRequestDtoToModelMapper :
        ObjectMappie<ScheduledExecutionManagerRequestDto, ScheduledExecutionManagerRequest>() {
        override fun map(from: ScheduledExecutionManagerRequestDto) =
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

    private class FromEntryResponseToDtoMapper(
        private val walletEntryDtoMapper: WalletItemDtoMapper,
    ) : ObjectMappie<EventListResponse.EntryResponse, EventForListDto.EntryResponseDto>() {
        override fun map(from: EventListResponse.EntryResponse) =
            mapping {
                to::walletItem fromProperty from::walletItem transform { walletEntryDtoMapper.entityToWalletItemForEntryListDto(it) }
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
}
