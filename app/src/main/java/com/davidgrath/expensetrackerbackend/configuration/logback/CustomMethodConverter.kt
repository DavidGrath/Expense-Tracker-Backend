package com.davidgrath.expensetrackerbackend.configuration.logback

import ch.qos.logback.classic.pattern.MethodOfCallerConverter
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.CoreConstants


class CustomMethodConverter : MethodOfCallerConverter() {
    override fun convert(le: ILoggingEvent): String {
        return if (le.loggerName.contains("com.davidgrath")) {
            "::" + super.convert(le)
        } else {
            CoreConstants.EMPTY_STRING
        }
    }
}