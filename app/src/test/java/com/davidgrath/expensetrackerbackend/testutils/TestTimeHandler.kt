package com.davidgrath.expensetrackerbackend.testutils

import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

class TestTimeHandler: TimeHandler {

    private var zoneId: ZoneId = ZoneId.of("UTC")
    private var clock =
        Clock.fixed(LocalDateTime.parse("2025-06-30T08:00:00.000").toInstant(ZoneOffset.UTC), ZoneId.of("UTC"))

    override fun getClock(): Clock {
        return clock
    }

    override fun getZone(): ZoneId {
        return zoneId
    }


    override fun changeClock(clock: Clock) {
        this.clock = clock
    }

    override fun changeZone(zoneId: ZoneId) {
        this.zoneId = zoneId
    }

}