package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.wallet.WalletItemSearchResponseDto
import com.ynixt.sharedfinances.application.web.mapper.UserDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.WalletItemDtoMapper
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
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
        override fun map(from: WalletItem): WalletItemSearchResponseDto =
            mapping {
                to::id fromPropertyNotNull from::id
                to::user fromProperty from::user transform { it?.let { userDtoMapper.tSimpleDto(it) } }
                to::dueDay fromExpression { from ->
                    if (from is CreditCard) from.dueDay else null
                }
                to::dueOnNextBusinessDay fromExpression { from ->
                    if (from is CreditCard) from.dueOnNextBusinessDay else null
                }
            }
    }
}
