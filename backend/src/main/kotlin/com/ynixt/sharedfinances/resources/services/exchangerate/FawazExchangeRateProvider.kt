package com.ynixt.sharedfinances.resources.services.exchangerate

import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateProvider
import feign.Feign
import feign.Param
import feign.RequestLine
import feign.jackson.JacksonDecoder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

@Component
class FawazExchangeRateProvider(
    @param:Value("\${app.exchangeRates.providerUrl}")
    private val providerUrl: String,
    private val clock: Clock,
) : ExchangeRateProvider {
    override val source: String = "fawazahmed0"

    // jsdelivr npm URL: https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@{tag}/v1/...
    // app.exchangeRates.providerUrl is the package root ending with "@" (no tag in config), e.g. ...currency-api@
    // fetchLatest -> ...@{latest} ; fetchForDate -> ...@{yyyy-MM-dd}
    // Plain base URLs without "@" (e.g. local mock) are used as-is for both.
    private val packageRootUrl: String by lazy {
        val at = providerUrl.lastIndexOf('@')
        if (at < 0) providerUrl else providerUrl.substring(0, at)
    }

    private val usesNpmTaggedBase: Boolean by lazy {
        providerUrl.lastIndexOf('@') >= 0
    }

    private val latestClient: FawazApiClient by lazy {
        val target =
            if (usesNpmTaggedBase) {
                "$packageRootUrl@latest"
            } else {
                providerUrl
            }
        Feign
            .builder()
            .decoder(JacksonDecoder())
            .target(FawazApiClient::class.java, target)
    }

    private fun historicalClient(date: LocalDate): FawazApiClient {
        val target =
            if (usesNpmTaggedBase) {
                "$packageRootUrl@$date"
            } else {
                providerUrl
            }
        return Feign
            .builder()
            .decoder(JacksonDecoder())
            .target(FawazApiClient::class.java, target)
    }

    override suspend fun fetchLatest(
        baseCurrency: String,
        quoteCurrencies: Set<String>,
    ): List<ExchangeRateProvider.Quote> {
        if (quoteCurrencies.isEmpty()) return emptyList()

        val normalizedBase = baseCurrency.lowercase()
        val payload = latestClient.getRates(normalizedBase)
        return parsePayload(normalizedBase, quoteCurrencies, payload)
    }

    override suspend fun fetchForDate(
        baseCurrency: String,
        quoteCurrencies: Set<String>,
        date: LocalDate,
    ): List<ExchangeRateProvider.Quote> {
        if (quoteCurrencies.isEmpty()) return emptyList()

        val normalizedBase = baseCurrency.lowercase()
        val payload = historicalClient(date).getRates(normalizedBase)
        return parsePayload(normalizedBase, quoteCurrencies, payload)
    }

    private fun parsePayload(
        normalizedBase: String,
        quoteCurrencies: Set<String>,
        payload: Map<String, Any?>,
    ): List<ExchangeRateProvider.Quote> {
        val quoteDate = payload["date"]?.toString()?.let(LocalDate::parse) ?: LocalDate.now(clock)
        val quotedAt = quoteDate.atStartOfDay().atOffset(ZoneOffset.UTC)

        val baseRatesMap =
            (payload[normalizedBase] as? Map<*, *>)
                ?.mapNotNull { (k, v) ->
                    val key = k?.toString() ?: return@mapNotNull null
                    key to toBigDecimal(v)
                }?.toMap() ?: emptyMap()

        return quoteCurrencies
            .map { it.uppercase() }
            .mapNotNull { quoteCurrency ->
                val rate = baseRatesMap[quoteCurrency.lowercase()] ?: return@mapNotNull null
                ExchangeRateProvider.Quote(
                    baseCurrency = normalizedBase.uppercase(),
                    quoteCurrency = quoteCurrency,
                    quoteDate = quoteDate,
                    quotedAt = quotedAt,
                    rate = rate,
                )
            }
    }

    private fun toBigDecimal(value: Any?): BigDecimal? =
        when (value) {
            is BigDecimal -> value
            is Number -> value.toString().toBigDecimalOrNull()
            is String -> value.toBigDecimalOrNull()
            else -> null
        }
}

private interface FawazApiClient {
    @RequestLine("GET /v1/currencies/{base}.json")
    fun getRates(
        @Param("base") base: String,
    ): Map<String, Any?>
}
