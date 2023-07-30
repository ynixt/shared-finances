package com.ynixt.sharedfinances.service.impl

import com.ynixt.sharedfinances.entity.CreditCard
import com.ynixt.sharedfinances.entity.User
import com.ynixt.sharedfinances.mapper.CreditCardMapper
import com.ynixt.sharedfinances.model.dto.TransactionValuesAndDateDto
import com.ynixt.sharedfinances.model.dto.creditcard.*
import com.ynixt.sharedfinances.model.exceptions.SFException
import com.ynixt.sharedfinances.repository.CreditCardRepository
import com.ynixt.sharedfinances.repository.TransactionRepository
import com.ynixt.sharedfinances.service.CreditCardService
import jakarta.transaction.Transactional
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class CreditCardServiceImpl(
    private val transactionRepository: TransactionRepository,
    private val creditCardRepository: CreditCardRepository,
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val creditCardMapper: CreditCardMapper
) : CreditCardService {
    override fun getSummary(
        user: User, creditCardId: Long, maxCreditCardBillDate: LocalDate
    ): CreditCardSummaryDto {
        return transactionRepository.getCreditCardSummary(
            userId = user.id!!, creditCardId = creditCardId, maxCreditCardBillDate = maxCreditCardBillDate
        )
    }

    override fun getOne(user: User, id: Long): CreditCard? {
        return creditCardRepository.findOneByIdAndUserIdIncludeBillDates(userId = user.id!!, id = id)
    }

    override fun listCreditCard(user: User): List<CreditCard> {
        return creditCardRepository.findAllByUserIdIncludeBillDates(user.id!!)
    }

    override fun listCreditCardAsCreditCardDto(user: User): List<CreditCardDto> {
        return creditCardMapper.toDtoList(listCreditCard(user))
    }

    @Transactional
    override fun newCreditCard(user: User, newDto: NewCreditCardDto): CreditCard {
        var creditCard = CreditCard(
            name = newDto.name,
            limit = newDto.limit,
            closingDay = newDto.closingDay,
            paymentDay = newDto.paymentDay,
            enabled = newDto.enabled,
            displayOnGroup = newDto.displayOnGroup,
            availableLimit = newDto.limit,
            user = user
        )

        creditCard = creditCardRepository.save(creditCard)

        creditCardsWasUpdated(user)

        return creditCard
    }

    @Transactional
    override fun updateCreditCard(user: User, id: Long, updateDto: UpdateCreditCardDto): CreditCard {
        var creditCard = getOne(user, id) ?: throw SFException(
            reason = "Credit card not found"
        )

        creditCardMapper.update(creditCard, updateDto)
        creditCard = creditCardRepository.save(creditCard)
        creditCardsWasUpdated(user)
        return creditCard
    }

    @Transactional
    override fun delete(user: User, id: Long) {
        creditCardRepository.deleteByIdAndUserId(userId = user.id!!, id = id)
        creditCardsWasUpdated(user)
    }

    override fun getChartByCreditCardId(
        user: User, creditCardId: Long, minCreditCardBillDate: LocalDate?, maxCreditCardBillDate: LocalDate?
    ): List<TransactionValuesAndDateDto> {
        return transactionRepository.findAllByCreditCardIdGroupedByDate(
            userId = user.id!!,
            creditCardId = creditCardId,
            minCreditCardBillDate = minCreditCardBillDate ?: LocalDate.now(),
            maxCreditCardBillDate = maxCreditCardBillDate ?: LocalDate.now().plusDays(1)
        )
    }

    @Transactional
    override fun addToAvailableLimit(user: User, creditCardId: Long, delta: BigDecimal) {
        creditCardRepository.addToAvailableLimit(
            id = creditCardId, userId = user.id!!, delta = delta
        )
    }

    override fun getCurrentLimit(user: User, creditCardId: Long): CreditCardLimitDto? {
        return creditCardRepository.getCurrentAvailableLimit(
            id = creditCardId, userId = user.id!!
        )
    }

    private fun creditCardsWasUpdated(user: User) {
        simpMessagingTemplate.convertAndSendToUser(
            user.email, "/queue/credit-card", listCreditCardAsCreditCardDto(user)
        )
    }
}
