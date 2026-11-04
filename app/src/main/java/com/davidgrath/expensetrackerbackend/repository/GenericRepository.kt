package com.davidgrath.expensetrackerbackend.repository

import org.jooq.DSLContext
import org.jooq.TransactionalRunnable
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
}
