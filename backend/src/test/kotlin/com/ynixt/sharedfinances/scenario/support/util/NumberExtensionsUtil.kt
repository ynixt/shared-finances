package com.ynixt.sharedfinances.scenario.support.util

import java.math.BigDecimal

internal fun Number.toBigDecimalSafe(): BigDecimal =
    when (this) {
        is BigDecimal -> this
        is Long -> BigDecimal.valueOf(this)
        is Int -> BigDecimal.valueOf(this.toLong())
        is Double -> BigDecimal.valueOf(this)
        is Float -> BigDecimal.valueOf(this.toDouble())
        else -> BigDecimal(this.toString())
    }
