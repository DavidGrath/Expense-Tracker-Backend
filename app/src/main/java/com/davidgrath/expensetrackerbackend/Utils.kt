package com.davidgrath.expensetrackerbackend

fun Boolean.toByte(): Byte {
    return if(this) {
        1
    } else {
        0
    }
}

val CORE_CATEGORIES = setOf(
    "food",
    "rent",
    "utilities",
    "transportation",
    "healthcare",
    "debt",
    "childcare",
    "household",
    "entertainment",
    "self_care",
    "clothing",
    "education",
    "gifts_and_donations",
    "emergency",
    "savings",
    "fitness",
    "miscellaneous",
)