package com.davidgrath.expensetrackerbackend.utils

import com.davidgrath.expensetrackerbackend.generatedapi.model.Account
import com.davidgrath.expensetrackerbackend.generatedapi.model.AccountDetails
import com.davidgrath.expensetrackerbackend.generatedapi.model.Category
import com.davidgrath.expensetrackerbackend.generatedapi.model.CategoryDetails
import com.davidgrath.expensetrackerbackend.generatedapi.model.Image
import com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionDraftFile
import com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionItem
import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.entities.views.AccountWithStats
import com.davidgrath.expensetrackerbackend.entities.views.CategoryWithStats
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.AccountPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.CategoryPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionItemPojo
import java.math.BigDecimal

fun Boolean.toByte(): Byte {
    return if(this) {
        1
    } else {
        0
    }
}

fun Byte?.toBoolean(): Boolean {
    return if(this == null) {
        false
    } else if (this == 0.toByte()){
        false
    } else {
        true
    }
}

fun TransactionMode?.toApiMode(): com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionMode? {
    return when(this) {
        TransactionMode.Card -> com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionMode.CARD
        TransactionMode.Transfer -> com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionMode.TRANSFER
        TransactionMode.Cash -> com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionMode.CASH
        TransactionMode.Other -> com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionMode.OTHER
        null -> null
    }
}

fun com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionMode?.toDbMode(): TransactionMode? {
    return when(this) {
        com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionMode.CARD -> TransactionMode.Card
        com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionMode.TRANSFER -> TransactionMode.Transfer
        com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionMode.CASH -> TransactionMode.Cash
        com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionMode.OTHER -> TransactionMode.Other
        null -> null
    }
}

fun accountWithStatsToAccountDetails(accountWithStats: AccountWithStats): AccountDetails {
    return AccountDetails(accountWithStats.id, accountWithStats.profileId, accountWithStats.currencyCode, accountWithStats.name, accountWithStats.transactionCount, accountWithStats.itemCount, accountWithStats.income, accountWithStats.expenses)
}

fun accountPojoToAccount(accountPojo: AccountPojo): Account {
    return Account(accountPojo.id, accountPojo.profileId, accountPojo.currencyCode, accountPojo.name)

}
fun transactionItemPojoToTransactionItem(transactionItemPojo: TransactionItemPojo, categoryPojo: CategoryPojo): TransactionItem {
    return TransactionItem(
        transactionItemPojo.id, transactionItemPojo.amount, transactionItemPojo.description, categoryPojoToCategory(categoryPojo),
        transactionItemPojo.isReduction.toBoolean(), emptyList<Image>()
        )
}

fun categoryPojoToCategory(categoryPojo: CategoryPojo): Category {
    return Category(categoryPojo.id, categoryPojo.name, categoryPojo.isCustom.toBoolean(), categoryPojo.stringId, categoryPojo.icon)
}
fun categoryWithStatsToApiCategory(categoryWithStats: CategoryWithStats): CategoryDetails {
    return CategoryDetails(categoryWithStats.id, categoryWithStats.name, categoryWithStats.isCustom, categoryWithStats.stringId, categoryWithStats.icon, categoryWithStats.transactionCount, categoryWithStats.itemCount)
}