package com.davidgrath.expensetrackerbackend.configuration

import java.io.File
import java.time.Clock
import java.time.ZoneId

interface TimeHandler {
    fun getClock(): Clock
    fun getZone(): ZoneId


    fun changeClock(clock: Clock)

    /**
     * For testing. Should no-op in regular code
     */
    fun changeZone(zoneId: ZoneId)
    fun getRoot(): File
    fun changeRoot(file: File)
}
