package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.walletentry.EventForListDto
import com.ynixt.sharedfinances.application.web.mapper.CategoryDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.GroupDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.RecurrenceEventDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.UserDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.WalletEntryDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.WalletEventDtoMapper
import com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
class WalletEventDtoMapperImpl(
    userDtoMapper: UserDtoMapper,
    categoryDtoMapper: CategoryDtoMapper,
    groupDtoMapper: GroupDtoMapper,
    walletEntryDtoMapper: WalletEntryDtoMapper,
    recurrenceEventDtoMapper: RecurrenceEventDtoMapper,
) : WalletEventDtoMapper {
    private val entryListMapper =
        EntryListMapper(
            userDtoMapper,
            categoryDtoMapper,
            groupDtoMapper,
            walletEntryDtoMapper,
            recurrenceEventDtoMapper,
        )

    override fun fromListResponseToListDto(from: EventListResponse): EventForListDto = entryListMapper.map(from)

    private class EntryListMapper(
        private val userDtoMapper: UserDtoMapper,
        private val categoryDtoMapper: CategoryDtoMapper,
        private val groupDtoMapper: GroupDtoMapper,
        private val walletEntryDtoMapper: WalletEntryDtoMapper,
        private val recurrenceEventDtoMapper: RecurrenceEventDtoMapper,
    ) : ObjectMappie<EventListResponse, EventForListDto>() {
        override fun map(from: EventListResponse) =
            mapping {
                to::category fromProperty from::category transform { it?.let { categoryDtoMapper.toDto(it) } }
                to::user fromProperty from::user transform { it?.let { userDtoMapper.tSimpleDto(it) } }
                to::group fromProperty from::group transform { it?.let { groupDtoMapper.toDto(it) } }
                to::entries fromProperty from::entries transform {
                    it.map { item -> walletEntryDtoMapper.fromEntryResponseToDto(item) }
                }
                to::recurrenceConfig fromProperty from::recurrenceConfig transform { it?.let { recurrenceEventDtoMapper.toDto(it) } }
            }
    }
}
