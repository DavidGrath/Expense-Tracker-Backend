package com.davidgrath.expensetrackerbackend.entities.dtos

import java.math.BigDecimal
import java.time.LocalDate

data class DateAmountSummary(
    val aggregateDate: LocalDate,
    val sum: BigDecimal
)