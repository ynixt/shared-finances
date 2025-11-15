package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.wallet.bankAccount.BankAccountDto
import com.ynixt.sharedfinances.application.web.dto.wallet.bankAccount.BankAccountForGroupAssociateDto
import com.ynixt.sharedfinances.application.web.dto.wallet.bankAccount.EditBankAccountDto
import com.ynixt.sharedfinances.application.web.dto.wallet.bankAccount.NewBankAccountDto
import com.ynixt.sharedfinances.application.web.mapper.BankAccountDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.UserDtoMapper
import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import com.ynixt.sharedfinances.domain.models.bankaccount.EditBankAccountRequest
import com.ynixt.sharedfinances.domain.models.bankaccount.NewBankAccountRequest
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie
import java.math.BigDecimal

@Component
class BankAccountDtoMapperImpl(
    userDtoMapper: UserDtoMapper,
) : BankAccountDtoMapper {
    private val associateMapper = BankAccountToAssociateDtoMapper(userDtoMapper)

    override fun toDto(from: BankAccount): BankAccountDto = BankAccountToDtoMapper.map(from)

    override fun fromDto(from: BankAccountDto): BankAccount = BankAccountFromDtoMapper.map(from)

    override fun fromNewDtoToNewRequest(from: NewBankAccountDto): NewBankAccountRequest = BankAccountFromNewDtoMapper.map(from)

    override fun fromEditDtoToEditRequest(from: EditBankAccountDto): EditBankAccountRequest = BankAccountFromEditDtoMapper.map(from)

    override fun toAssociateDto(from: BankAccount): BankAccountForGroupAssociateDto = associateMapper.map(from)

    private object BankAccountToDtoMapper : ObjectMappie<BankAccount, BankAccountDto>() {
        override fun map(from: BankAccount) =
            mapping {
                to::id fromPropertyNotNull from::id
            }
    }

    private object BankAccountFromDtoMapper : ObjectMappie<BankAccountDto, BankAccount>() {
        override fun map(from: BankAccountDto) = mapping {}
    }

    private object BankAccountFromNewDtoMapper : ObjectMappie<NewBankAccountDto, NewBankAccountRequest>() {
        override fun map(from: NewBankAccountDto) =
            mapping {
                to::balance fromProperty NewBankAccountDto::balance transform { it ?: BigDecimal.ZERO }
            }
    }

    private object BankAccountFromEditDtoMapper : ObjectMappie<EditBankAccountDto, EditBankAccountRequest>() {
        override fun map(from: EditBankAccountDto) = mapping {}
    }

    private class BankAccountToAssociateDtoMapper(
        private val userDtoMapper: UserDtoMapper,
    ) : ObjectMappie<BankAccount, BankAccountForGroupAssociateDto>() {
        override fun map(from: BankAccount) =
            mapping {
                to::id fromPropertyNotNull from::id
                to::user fromPropertyNotNull from::user transform { userDtoMapper.tSimpleDto(it) }
            }
    }
}
