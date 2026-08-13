package com.ynixt.sharedfinances.resources.services.exchangerate

import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateProvider
import feign.Feign
import feign.Param
import feign.RequestLine
import feign.jackson.JacksonDecoder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate

@Component
class FawazExchangeRateProvider(
    @param:Value("\${app.exchangeRates.providerUrl}")
    private val providerUrl: String,
    private val clock: Clock,
) : ExchangeRateProvider {
    override val source: String = "fawazahmed0"
    private val logger = LoggerFactory.getLogger(javaClass)

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

    override suspend fun fetchUsdRates(date: LocalDate?): List<ExchangeRateProvider.Quote> =
        parsePayload(
            payload =
                if (date == null) {
                    latestClient.getRates(USD)
                } else {
                    historicalClient(date).getRates(USD)
                },
        )

    private fun parsePayload(payload: Map<String, Any?>): List<ExchangeRateProvider.Quote> {
        val quoteDate = payload["date"]?.toString()?.let(LocalDate::parse) ?: LocalDate.now(clock)

        val rates = payload[USD] as? Map<*, *> ?: return emptyList()
        return rates.mapNotNull { (rawCurrency, rawRate) ->
            val currency = rawCurrency?.toString()?.uppercase()
            val rate = toBigDecimal(rawRate)
            if (currency.isNullOrBlank() || rate == null) {
                logger.error("Ignoring malformed USD exchange rate entry: currency={}, rate={}", rawCurrency, rawRate)
                null
            } else {
                ExchangeRateProvider.Quote(
                    currency = currency,
                    quoteDate = quoteDate,
                    rate = rate,
                )
            }
        }
    }

    private fun toBigDecimal(value: Any?): BigDecimal? =
        when (value) {
            is BigDecimal -> value
            is Number -> value.toString().toBigDecimalOrNull()
            is String -> value.toBigDecimalOrNull()
            else -> null
        }

    private companion object {
        const val USD = "usd"
    }
}

private interface FawazApiClient {
    @RequestLine("GET /v1/currencies/{base}.json")
    fun getRates(
        @Param("base") base: String,
    ): Map<String, Any?>
}
