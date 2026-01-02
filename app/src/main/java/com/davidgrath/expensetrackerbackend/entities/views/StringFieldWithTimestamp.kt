package com.davidgrath.expensetrackerbackend.entities.views

import java.time.LocalDateTime

data class StringFieldWithTimestamp(
    val stringField: String,
    val createdAt: LocalDateTime
)
