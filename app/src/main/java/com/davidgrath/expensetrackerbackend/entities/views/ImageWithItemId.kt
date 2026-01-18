package com.davidgrath.expensetrackerbackend.entities.views

import java.time.LocalDateTime

data class ImageWithItemId(
    val itemId: Long,
    val id: Long,
    val profileId: Long,
    val sizeBytes: Long,
    val sha_256: String,
    val mimeType: String,
    val uri: String,
    val path: String,
    val createdAt: LocalDateTime
)
