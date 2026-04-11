package com.ynixt.sharedfinances.resources.services.walletentry

import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.exceptions.http.GroupNotFoundException
import com.ynixt.sharedfinances.domain.exceptions.http.OriginNotFoundException
import com.ynixt.sharedfinances.domain.exceptions.http.TargetNotFoundException
import com.ynixt.sharedfinances.domain.exceptions.http.UnauthorizedException
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.walletentry.TransferQuoteRequest
import com.ynixt.sharedfinances.domain.services.walletentry.TransferQuoteResult
import com.ynixt.sharedfinances.domain.services.walletentry.TransferRateRequest
import com.ynixt.sharedfinances.domain.services.walletentry.TransferRateResult
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryTransferQuoteService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Service
class WalletEntryTransferQuoteServiceImpl(
    private val walletItemService: WalletItemService,
    private val groupService: GroupService,
    private val exchangeRateService: ExchangeRateService,
) : WalletEntryTransferQuoteService {
    override suspend fun quote(
        userId: UUID,
        request: TransferQuoteRequest,
    ): TransferQuoteResult {
        val origin = walletItemService.findOne(request.originId) ?: throw OriginNotFoundException(request.originId)
        val target = walletItemService.findOne(request.targetId) ?: throw TargetNotFoundException(request.targetId)
        val group =
            request.groupId?.let { groupId ->
                groupService.findGroupWithAssociatedItems(userId = userId, id = groupId) ?: throw GroupNotFoundException(groupId)
            }

        val hasPermission =
            if (group == null) {
                origin.userId == userId && target.userId == userId
            } else {
                group.permissions.contains(GroupPermissions.SEND_ENTRIES) &&
                    group.itemsAssociatedIds.contains(origin.id!!) &&
                    group.itemsAssociatedIds.contains(target.id!!)
            }

        if (!hasPermission) {
            throw UnauthorizedException()
        }

        val normalizedOriginValue = request.originValue.abs().setScale(2, RoundingMode.HALF_UP)
        val targetValue =
            if (origin.currency == target.currency) {
                normalizedOriginValue
            } else {
                exchangeRateService.convert(
                    value = normalizedOriginValue,
                    fromCurrency = origin.currency,
                    toCurrency = target.currency,
                    referenceDate = request.date,
                )
            }

        return TransferQuoteResult(targetValue = targetValue)
    }

    override suspend fun transferRate(
        userId: UUID,
        request: TransferRateRequest,
    ): TransferRateResult {
        val origin = walletItemService.findOne(request.originId) ?: throw OriginNotFoundException(request.originId)
        val target = walletItemService.findOne(request.targetId) ?: throw TargetNotFoundException(request.targetId)
        val group =
            request.groupId?.let { groupId ->
                groupService.findGroupWithAssociatedItems(userId = userId, id = groupId) ?: throw GroupNotFoundException(groupId)
            }

        val hasPermission =
            if (group == null) {
                origin.userId == userId && target.userId == userId
            } else {
                group.permissions.contains(GroupPermissions.SEND_ENTRIES) &&
                    group.itemsAssociatedIds.contains(origin.id!!) &&
                    group.itemsAssociatedIds.contains(target.id!!)
            }

        if (!hasPermission) {
            throw UnauthorizedException()
        }

        val base = origin.currency.uppercase()
        val quote = target.currency.uppercase()

        if (base == quote) {
            return TransferRateResult(
                rate = BigDecimal.ONE,
                quoteDate = request.date,
                baseCurrency = base,
                quoteCurrency = quote,
            )
        }

        val resolved =
            exchangeRateService.resolveRate(
                fromCurrency = base,
                toCurrency = quote,
                referenceDate = request.date,
            )

        return TransferRateResult(
            rate = resolved.rate,
            quoteDate = resolved.quoteDate,
            baseCurrency = base,
            quoteCurrency = quote,
        )
    }
}
