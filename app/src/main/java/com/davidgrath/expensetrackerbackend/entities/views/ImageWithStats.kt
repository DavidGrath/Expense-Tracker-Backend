package com.davidgrath.expensetrackerbackend.entities.views

data class ImageWithStats(
    val id: Long,
    val profileId: Long,
    val sizeBytes: Long,
    val sha256: String,
    val mimeType: String,
    val path: String,
    val transactionCount: Long,
    val itemCount: Long
)
