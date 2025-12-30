package com.davidgrath.expensetrackerbackend.entities.views

data class CategoryWithStats(
    val id: Long,
    val profileId: Long,
    val stringId: String?,
    val isCustom: Boolean,
    val name: String?,
    val icon: String,
    val transactionCount: Int,
    val itemCount: Int //TODO Account for reductions
)