package com.davidgrath.expensetrackerbackend.configuration

import java.time.Clock
import java.time.ZoneId
import java.util.*

class MainTimeHandler: TimeHandler {

    override fun getClock(): Clock {
        return Clock.systemDefaultZone()
    }

    override fun getZone(): ZoneId {
        return ZoneId.systemDefault()
    }


    override fun changeClock(clock: Clock) {
        //Do Nothing
    }

    override fun changeZone(zoneId: ZoneId) {
        //Do Nothing
    }

}