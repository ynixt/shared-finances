package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.walletentry.NewEntryDto
import com.ynixt.sharedfinances.application.web.mapper.WalletEntryDtoMapper
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
class WalletEntryDtoMapperImpl : WalletEntryDtoMapper {
    override fun fromNewDtoToNewRequest(from: NewEntryDto): NewEntryRequest = FromNewDtoMapper.map(from)

    private object FromNewDtoMapper : ObjectMappie<NewEntryDto, NewEntryRequest>() {
        override fun map(from: NewEntryDto) =
            mapping {
            }
    }
}
