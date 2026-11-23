package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.*

/**
 * In general, it's a good sign if the last portion of the path is based on profileId, a.k.a. you didn't omit the most important part
 */
class RepositoryUtils

val sellerToTransactionPath =
    SELLER.ORDINAL.eq(TRANSACTION.SELLER_ORDINAL)
        .and(SELLER.PROFILE_ID.eq(TRANSACTION.PROFILE_ID)) //Like this, for example

/**
 * CAUTION: DO NOT USE for statisticsRepository, it will cause weird duplication. TODO Phase out
 */
val sellerLocationToSellerPath =
    SELLER_LOCATION.SELLER_ORDINAL.eq(SELLER.ORDINAL)
    .and(SELLER_LOCATION.PROFILE_ID.eq(SELLER.PROFILE_ID))

val transactionToSellerLocationPath = TRANSACTION.SELLER_LOCATION_ORDINAL.eq(SELLER_LOCATION.ORDINAL)
    .and(TRANSACTION.SELLER_ORDINAL.eq(SELLER_LOCATION.SELLER_ORDINAL))
    .and(TRANSACTION.PROFILE_ID.eq(SELLER_LOCATION.PROFILE_ID))

val categoryToItemPath =
    CATEGORY.ORDINAL.eq(TRANSACTION_ITEM.PRIMARY_CATEGORY_ORDINAL)
        .and(CATEGORY.PROFILE_ID.eq(TRANSACTION_ITEM.PROFILE_ID))

val imageToItemImagesPath =
    TRANSACTION_ITEM_IMAGES.IMAGE_ORDINAL.eq(IMAGE.ORDINAL)
        .and(TRANSACTION_ITEM_IMAGES.PROFILE_ID.eq(IMAGE.PROFILE_ID))

val itemToItemImagesPath =
    TRANSACTION_ITEM.ORDINAL.eq(TRANSACTION_ITEM_IMAGES.TRANSACTION_ITEM_ORDINAL)
    .and(TRANSACTION_ITEM.TRANSACTION_ORDINAL.eq(TRANSACTION_ITEM_IMAGES.TRANSACTION_ORDINAL))
    .and(TRANSACTION_ITEM.ACCOUNT_ORDINAL.eq(TRANSACTION_ITEM_IMAGES.ACCOUNT_ORDINAL))
    .and(TRANSACTION_ITEM.PROFILE_ID.eq(TRANSACTION_ITEM_IMAGES.PROFILE_ID))

val itemToTransactionPath =
    TRANSACTION.ORDINAL.eq(TRANSACTION_ITEM.TRANSACTION_ORDINAL)
    .and(TRANSACTION.ACCOUNT_ORDINAL.eq(TRANSACTION_ITEM.ACCOUNT_ORDINAL))
    .and(TRANSACTION.PROFILE_ID.eq(TRANSACTION_ITEM.PROFILE_ID))  //(2026-11-09) Thank you, idorTest

val accountToTransactionPath =
    ACCOUNT.ORDINAL.eq(TRANSACTION.ACCOUNT_ORDINAL)
    .and(ACCOUNT.PROFILE_ID.eq(TRANSACTION.PROFILE_ID))
