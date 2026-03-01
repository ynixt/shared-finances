package com.ynixt.sharedfinances.domain.extensions

import java.time.LocalDate
import java.time.YearMonth

object LocalDateExtensions {
    fun LocalDate.isSameMonthYear(other: LocalDate): Boolean = YearMonth.from(this) == YearMonth.from(other)

    fun LocalDate.withStartOfMonth(): LocalDate = this.withDayOfMonth(1)
}
