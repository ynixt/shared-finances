package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.wallet.WalletItemSearchResponseDto
import com.ynixt.sharedfinances.application.web.mapper.UserDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.WalletItemDtoMapper
import com.ynixt.sharedfinances.domain.models.WalletItemSearchResponse
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
class WalletItemDtoMapperImpl(
    userDtoMapper: UserDtoMapper,
) : WalletItemDtoMapper {
    private val searchResponseToDtoMapper = WalletItemSearchResponseToDtoMapper(userDtoMapper)

    override fun searchResponseToDto(from: WalletItemSearchResponse): WalletItemSearchResponseDto = searchResponseToDtoMapper.map(from)

    private class WalletItemSearchResponseToDtoMapper(
        private val userDtoMapper: UserDtoMapper,
    ) : ObjectMappie<WalletItemSearchResponse, WalletItemSearchResponseDto>() {
        override fun map(from: WalletItemSearchResponse) =
            mapping {
                to::id fromPropertyNotNull from::id
                to::user fromProperty from::user transform { it?.let { userDtoMapper.tSimpleDto(it) } }
            }
    }
}
