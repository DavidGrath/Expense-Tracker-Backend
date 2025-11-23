package com.davidgrath.expensetrackerbackend.testutils

interface Commit {
    /**
     * Returns the list of transaction IDs
     */
    fun commit(): List<Long>
}

