package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.walletentry.EntryForListDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.ListEntryRequestDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.NewEntryDto
import com.ynixt.sharedfinances.application.web.mapper.CategoryDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.GroupDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.UserDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.WalletEntryDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.WalletItemDtoMapper
import com.ynixt.sharedfinances.domain.models.CursorPageRequest
import com.ynixt.sharedfinances.domain.models.ListEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.EntryListResponse
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
class WalletEntryDtoMapperImpl(
    userDtoMapper: UserDtoMapper,
    categoryDtoMapper: CategoryDtoMapper,
    groupDtoMapper: GroupDtoMapper,
    walletItemDtoMapper: WalletItemDtoMapper,
) : WalletEntryDtoMapper {
    private val entryListMapper = EntryListMapper(userDtoMapper, categoryDtoMapper, groupDtoMapper, walletItemDtoMapper)

    override fun fromNewDtoToNewRequest(from: NewEntryDto): NewEntryRequest = FromNewDtoMapper.map(from)

    override fun fromListResponseToListDto(from: EntryListResponse): EntryForListDto = entryListMapper.map(from)

    override fun fromEntryListRequestDtoToModel(from: ListEntryRequestDto): ListEntryRequest =
        FromEntryListRequestDtoToModelMapper.map(from)

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
