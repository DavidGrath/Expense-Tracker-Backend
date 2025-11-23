package com.davidgrath.expensetrackerbackend.testutils

import java.math.BigDecimal

interface TransactionItemBuilder: Commit {
    fun mainDetails(description: String, category: String, amount: BigDecimal): TransactionItemBuilder
    fun otherDetails(brand: String?, quantity: Int, variation: String, referenceNumber: String?, isReduction: Boolean): TransactionItemBuilder
    fun withImages(vararg resource: TestData.Resource): TransactionItemBuilder
    fun build(): TransactionBuilder
}