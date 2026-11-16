package com.davidgrath.expensetrackerbackend.entities.dtos

data class CategoryWithStats(
    val id: Long,
    val profileId: Long,
    val stringId: String?,
    val isCustom: Boolean,
    val name: String?,
    val icon: String,
    val transactionCount: Long,
    val itemCount: Long //TODO Account for reductions
)