package com.ynixt.sharedfinances.resources.services.dashboard

import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardCardKey
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardDetail
import com.ynixt.sharedfinances.domain.services.exchangerate.ConversionRequest
import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateService
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
internal class OverviewDashboardAssemblyServiceImpl(
    private val exchangeRateService: ExchangeRateService,
) {
    internal suspend fun convertRawValues(
        rawValues: List<RawValue>,
        targetCurrency: String,
    ): Map<String, BigDecimal> {
        if (rawValues.isEmpty()) {
            return emptyMap()
        }

        val conversionRequestByKey = linkedMapOf<String, ConversionRequest>()

        rawValues.forEach { rawValue ->
            val fromCurrency = rawValue.currency.uppercase()
            if (fromCurrency == targetCurrency || rawValue.value.compareTo(BigDecimal.ZERO) == 0) {
                return@forEach
            }

            conversionRequestByKey[rawValue.key] =
                ConversionRequest(
                    value = rawValue.value,
                    fromCurrency = fromCurrency,
                    toCurrency = targetCurrency,
                    referenceDate = rawValue.referenceDate,
                )
        }

        val convertedByRequest =
            if (conversionRequestByKey.isEmpty()) {
                emptyMap()
            } else {
                exchangeRateService.convertBatch(conversionRequestByKey.values)
            }

        return rawValues.associate { rawValue ->
            val request = conversionRequestByKey[rawValue.key]
            val converted =
                if (request == null) {
                    rawValue.value
                } else {
                    convertedByRequest.getValue(request)
                }

            rawValue.key to converted.asMoney()
        }
    }

    internal fun buildConvertedDetails(
        rawDetailByCardKey: Map<OverviewDashboardCardKey, List<RawDetail>>,
        convertedValueByKey: Map<String, BigDecimal>,
    ): Map<OverviewDashboardCardKey, List<OverviewDashboardDetail>> =
        rawDetailByCardKey.mapValues { (_, details) ->
            details.map { detail ->
                OverviewDashboardDetail(
                    sourceId = detail.sourceId,
                    sourceType = detail.sourceType,
                    label = detail.label,
                    value = convertedValueByKey.getOrDefault(detail.key, BigDecimal.ZERO).asMoney(),
                )
            }
        }

    internal fun sumDetails(details: List<OverviewDashboardDetail>?): BigDecimal =
        details
            .orEmpty()
            .fold(BigDecimal.ZERO) { acc, detail -> acc.add(detail.value) }
            .asMoney()
}
