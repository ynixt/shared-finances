package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.wallet.WalletItemSearchResponseDto
import com.ynixt.sharedfinances.application.web.mapper.UserDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.WalletItemDtoMapper
import com.ynixt.sharedfinances.domain.models.WalletItem
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
class WalletItemDtoMapperImpl(
    userDtoMapper: UserDtoMapper,
) : WalletItemDtoMapper {
    private val searchResponseToDtoMapper = WalletItemSearchResponseToDtoMapper(userDtoMapper)

    override fun searchResponseToDto(from: WalletItem): WalletItemSearchResponseDto = searchResponseToDtoMapper.map(from)

    private class WalletItemSearchResponseToDtoMapper(
        private val userDtoMapper: UserDtoMapper,
    ) : ObjectMappie<WalletItem, WalletItemSearchResponseDto>() {
        override fun map(from: WalletItem) =
            mapping {
                to::id fromPropertyNotNull from::id
                to::user fromProperty from::user transform { it?.let { userDtoMapper.tSimpleDto(it) } }
            }
    }
}
