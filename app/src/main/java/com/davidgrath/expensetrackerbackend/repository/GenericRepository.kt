package com.davidgrath.expensetrackerbackend.repository

import org.jooq.DSLContext
import org.jooq.TransactionalRunnable
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

/**
 * Basically exclusively to provide a jooq transaction
 */
@Repository
class GenericRepository {

    @Autowired
    lateinit var mainContext: DSLContext


    fun transactionContext(runnable: TransactionalRunnable) {
        mainContext.transaction(runnable)
    }

    /**
     * TODO For some reason, this also returns 1 even though I believe it should return 0. Ignoring for now
     */
    fun affectedCount(trxContext: DSLContext): Int {
        val affectedCount = trxContext.select(DSL.function("row_count", SQLDataType.INTEGER)).fetchOne()!!.value1()!!
        return affectedCount
    }
}
