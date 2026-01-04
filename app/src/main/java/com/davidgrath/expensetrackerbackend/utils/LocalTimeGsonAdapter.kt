package com.davidgrath.expensetrackerbackend.utils

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

class LocalTimeGsonAdapter: TypeAdapter<LocalTime>() {
    override fun write(out: JsonWriter?, value: LocalTime?) {
        out!!.value(value?.toString())
    }

    override fun read(`in`: JsonReader?): LocalTime? {
        return try {
            LocalTime.parse(`in`?.nextString()!!)
        } catch (e: DateTimeParseException) {
            LOGGER.warn("Could not parse time", e)
            null
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(LocalTimeGsonAdapter::class.java)
    }
}