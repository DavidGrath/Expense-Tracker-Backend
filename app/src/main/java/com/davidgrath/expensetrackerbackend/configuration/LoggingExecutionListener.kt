package com.davidgrath.expensetrackerbackend.configuration

import org.jooq.ExecuteContext
import org.jooq.ExecuteListener
import org.jooq.ExecuteType
import org.slf4j.LoggerFactory

class LoggingExecutionListener: ExecuteListener {
    override fun resultEnd(ctx: ExecuteContext?) {
        when(ctx?.type()) {
            ExecuteType.READ -> {
                val result = ctx.result()
                if(result == null) {
                    LOGGER.warn("No results for READ execution")
                } else {
                    LOGGER.info("Result size: {}", result.size)
                }
            }
            ExecuteType.WRITE -> {
                val rows = ctx.rows()
                LOGGER.info("Rows for write: {}", rows)
            }
            ExecuteType.DDL, ExecuteType.BATCH, ExecuteType.ROUTINE, null -> {

            }
            ExecuteType.OTHER -> {

            }
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(LoggingExecutionListener::class.java)
    }
}