package com.davidgrath.expensetrackerbackend.entities.views

import java.math.BigDecimal
import java.time.LocalDate

data class DateAmountSummary(
    val aggregateDate: LocalDate,
    val sum: BigDecimal
)