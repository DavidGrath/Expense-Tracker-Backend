package com.davidgrath.expensetrackerbackend.utils

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeParseException

class LocalDateGsonAdapter: TypeAdapter<LocalDate>() {
    override fun write(out: JsonWriter?, value: LocalDate?) {
        out!!.value(value?.toString())
    }

    override fun read(`in`: JsonReader?): LocalDate? {
        return try {
            LocalDate.parse(`in`?.nextString()!!)
        } catch (e: DateTimeParseException) {
            LOGGER.warn("Could not parse date", e)
            null
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(LocalDateGsonAdapter::class.java)
    }
}