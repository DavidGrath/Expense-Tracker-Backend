package com.davidgrath.expensetrackerbackend.entities.dtos

import java.time.LocalDateTime

data class StringFieldWithTimestamp(
    val stringField: String,
    val createdAt: LocalDateTime
)
