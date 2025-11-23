package com.davidgrath.expensetrackerbackend.configuration

import java.time.Clock
import java.time.ZoneId
import java.util.*

interface TimeAndLocaleHandler {
    fun getClock(): Clock
    fun getZone(): ZoneId

    fun getLocale(): Locale

    fun changeClock(clock: Clock)

    /**
     * For testing. Should no-op in regular code
     */
    fun changeZone(zoneId: ZoneId)

    /**
     * For testing. Should no-op in regular code
     */
    fun changeLocale(locale: Locale)
}
