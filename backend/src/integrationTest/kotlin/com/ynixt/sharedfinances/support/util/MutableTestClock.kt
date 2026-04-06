package com.ynixt.sharedfinances.support.util

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MutableTestClock(
    private var currentInstant: Instant = Instant.now(),
    private var currentZone: ZoneId = ZoneId.systemDefault(),
) : Clock() {
    override fun getZone(): ZoneId = currentZone

    override fun withZone(zone: ZoneId): Clock = MutableTestClock(currentInstant, zone)

    override fun instant(): Instant = currentInstant

    fun setDate(date: LocalDate) {
        currentInstant = date.atStartOfDay(currentZone).toInstant()
    }

    fun setInstant(instant: Instant) {
        currentInstant = instant
    }

    fun today(): LocalDate = LocalDate.now(this)
}
