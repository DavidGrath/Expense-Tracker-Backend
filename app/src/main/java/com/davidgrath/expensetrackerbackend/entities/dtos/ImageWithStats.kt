package com.davidgrath.expensetrackerbackend.entities.dtos

data class ImageWithStats(
    val id: Long,
    val profileId: Long,
    val sizeBytes: Long,
    val sha256: ByteArray,
    val mimeType: String,
    val path: String,
    val transactionCount: Long,
    val itemCount: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageWithStats

        if (id != other.id) return false
        if (profileId != other.profileId) return false
        if (sizeBytes != other.sizeBytes) return false
        if (transactionCount != other.transactionCount) return false
        if (itemCount != other.itemCount) return false
        if (!sha256.contentEquals(other.sha256)) return false
        if (mimeType != other.mimeType) return false
        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + profileId.hashCode()
        result = 31 * result + sizeBytes.hashCode()
        result = 31 * result + transactionCount.hashCode()
        result = 31 * result + itemCount.hashCode()
        result = 31 * result + sha256.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + path.hashCode()
        return result
    }
}
