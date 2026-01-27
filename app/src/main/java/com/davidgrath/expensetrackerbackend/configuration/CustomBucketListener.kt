package com.davidgrath.expensetrackerbackend.configuration

import io.github.bucket4j.BucketListener
import org.slf4j.LoggerFactory

class CustomBucketListener: BucketListener {

    override fun onConsumed(tokens: Long) {

    }

    override fun onRejected(tokens: Long) {
        LOGGER.info("Tokens rejected: {}", tokens)
    }

    override fun onParked(nanos: Long) {

    }

    override fun onInterrupted(e: InterruptedException?) {

    }

    override fun onDelayed(nanos: Long) {

    }

    private val LOGGER = LoggerFactory.getLogger(CustomBucketListener::class.java)
}