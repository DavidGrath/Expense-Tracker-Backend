package com.davidgrath.expensetrackerbackend.entities

import org.jooq.impl.AbstractConverter

class TransactionModeConverter: AbstractConverter<String, TransactionMode>(String::class.java, TransactionMode::class.java, ) {
    override fun from(databaseObject: String?): TransactionMode {
        return when(databaseObject) {
            "card" -> TransactionMode.Card
            "transfer" -> TransactionMode.Transfer
            "cash" -> TransactionMode.Cash
            "other" -> TransactionMode.Other
            else -> TransactionMode.Other
        }
    }

    override fun to(userObject: TransactionMode?): String {
        return when(userObject) {
            TransactionMode.Card -> "card"
            TransactionMode.Transfer -> "transfer"
            TransactionMode.Cash -> "cash"
            TransactionMode.Other -> "other"
            null -> "other"
        }
    }
}