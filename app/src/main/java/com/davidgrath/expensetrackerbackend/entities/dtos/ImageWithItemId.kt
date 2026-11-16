package com.davidgrath.expensetrackerbackend.entities.dtos

import java.time.LocalDateTime

data class ImageWithItemId(
    val itemId: Long,
    val id: Long,
    val profileId: Long,
    val sizeBytes: Long,
    val sha_256: ByteArray, //TODO Consider using a jooq converter
    val mimeType: String,
    val uri: String,
    val path: String,
    val createdAt: LocalDateTime
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageWithItemId

        if (itemId != other.itemId) return false
        if (id != other.id) return false
        if (profileId != other.profileId) return false
        if (sizeBytes != other.sizeBytes) return false
        if (!sha_256.contentEquals(other.sha_256)) return false
        if (mimeType != other.mimeType) return false
        if (uri != other.uri) return false
        if (path != other.path) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = itemId.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + profileId.hashCode()
        result = 31 * result + sizeBytes.hashCode()
        result = 31 * result + sha_256.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + uri.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
