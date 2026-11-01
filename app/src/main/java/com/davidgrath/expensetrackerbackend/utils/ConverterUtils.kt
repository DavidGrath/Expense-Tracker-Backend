package com.davidgrath.expensetrackerbackend.utils

import com.davidgrath.expensetrackerbackend.entities.DocumentType
import com.davidgrath.expensetrackerbackend.entities.TransactionDraft
import com.davidgrath.expensetrackerbackend.entities.TransactionDraftItem
import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.entities.views.*
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.*
import com.davidgrath.expensetrackerbackend.generatedapi.model.*
import java.net.URI
import java.util.*

fun Boolean.toByte(): Byte {
    return if(this) {
        1
    } else {
        0
    }
}

fun Byte?.toBoolean(): Boolean {
    return if(this == null) {
        false
    } else if (this == 0.toByte()){
        false
    } else {
        true
    }
}

fun TransactionMode?.toApiMode(): com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionMode? {
    return when(this) {
        TransactionMode.Card -> com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionMode.CARD
        TransactionMode.Transfer -> com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionMode.TRANSFER
        TransactionMode.Cash -> com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionMode.CASH
        TransactionMode.Other -> com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionMode.OTHER
        null -> null
    }
}

fun com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionMode?.toDbMode(): TransactionMode? {
    return when(this) {
        com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionMode.CARD -> TransactionMode.Card
        com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionMode.TRANSFER -> TransactionMode.Transfer
        com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionMode.CASH -> TransactionMode.Cash
        com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionMode.OTHER -> TransactionMode.Other
        null -> null
    }
}

fun DocumentType.toApiType(): com.davidgrath.expensetrackerbackend.generatedapi.model.DocumentType {
    return when(this) {
        DocumentType.BankStatement -> com.davidgrath.expensetrackerbackend.generatedapi.model.DocumentType.BANK_STATEMENT
        DocumentType.Receipt -> com.davidgrath.expensetrackerbackend.generatedapi.model.DocumentType.RECEIPT
        DocumentType.Invoice -> com.davidgrath.expensetrackerbackend.generatedapi.model.DocumentType.INVOICE
        DocumentType.Other -> com.davidgrath.expensetrackerbackend.generatedapi.model.DocumentType.OTHER
    }
}

fun com.davidgrath.expensetrackerbackend.generatedapi.model.DocumentType.toDbType(): DocumentType {
    return when(this) {
        com.davidgrath.expensetrackerbackend.generatedapi.model.DocumentType.BANK_STATEMENT -> DocumentType.BankStatement
        com.davidgrath.expensetrackerbackend.generatedapi.model.DocumentType.RECEIPT -> DocumentType.Receipt
        com.davidgrath.expensetrackerbackend.generatedapi.model.DocumentType.INVOICE -> DocumentType.Invoice
        com.davidgrath.expensetrackerbackend.generatedapi.model.DocumentType.OTHER -> DocumentType.Other
    }
}

fun accountWithStatsToAccountDetails(accountWithStats: AccountWithStats): AccountDetails {
    return AccountDetails(accountWithStats.id, accountWithStats.profileId, accountWithStats.currencyCode, accountWithStats.name, accountWithStats.transactionCount, accountWithStats.itemCount, accountWithStats.income, accountWithStats.expenses)
}

fun accountPojoToAccount(accountPojo: AccountPojo): Account {
    return Account(accountPojo.id, accountPojo.profileId, accountPojo.currencyCode, accountPojo.name)
}
fun accountPojoToAccount(accountPojo: TransactionWithItemAndAccount): Account {
    return Account(accountPojo.accountId, accountPojo.accountProfileId, accountPojo.accountCurrencyCode, accountPojo.accountName)
}
fun transactionWithItemToTransactionItem(transactionWithItem: TransactionWithItemAndAccount, baseUrl: Triple<String, String, Int>, images: List<ImagePojo>): TransactionItem {
    val imagesApi = images.map {
        val imageUrl = URI(baseUrl.first, null, baseUrl.second, baseUrl.third, "/files" + it.path, null, null)
        Image(it.id, it.sizeBytes, imageUrl, it.mimeType)
    }
    return TransactionItem(
        transactionWithItem.itemId, transactionWithItem.itemAmount, transactionWithItem.itemDescription, transactionWithItemToCategory(transactionWithItem),
        transactionWithItem.itemQuantity, transactionWithItem.itemIsReduction.toBoolean(), transactionWithItem.itemBrand, transactionWithItem.itemVariation, imagesApi
        )
}

fun transactionItemPojoToTransactionItem(transactionItemPojo: TransactionItemPojo, baseUrl: Triple<String, String, Int>, categoryPojo: CategoryPojo, images: List<ImagePojo>): TransactionItem {
    val imagesApi = images.map {
        val imageUrl = URI(baseUrl.first, null, baseUrl.second, baseUrl.third, "/files" + it.path, null, null)
        Image(it.id, it.sizeBytes, imageUrl, it.mimeType)
    }
    return TransactionItem(
        transactionItemPojo.id, transactionItemPojo.amount, transactionItemPojo.description, categoryPojoToCategory(categoryPojo),
        transactionItemPojo.quantity, transactionItemPojo.isReduction.toBoolean(), transactionItemPojo.brand, transactionItemPojo.variation, imagesApi
        )
}

fun documentPojoToDocument(document: DocumentPojo, baseUrl: Triple<String, String, Int>): Document {
    val documentUrl = URI(baseUrl.first, null, baseUrl.second, baseUrl.third, "/files" + document.path, null, null)
    return Document(document.id, document.transactionId, document.sizeBytes, documentUrl, document.mimeType, document.type!!.toApiType())
}
fun documentPojoToDocumentDetails(document: DocumentWithTransactionDateAndOrdinal, baseUrl: Triple<String, String, Int>): DocumentDetails {
    val documentUrl = URI(baseUrl.first, null, baseUrl.second, baseUrl.third, "/files" + document.path, null, null)
    return DocumentDetails(document.id, document.transactionId, document.sizeBytes, documentUrl, document.mimeType, document.type!!.toApiType(), document.transactionDatedAt)
}

fun imageWithStatsToImageDetails(image: ImageWithStats, baseUrl: Triple<String, String, Int>): ImageDetails {
    val imageUrl = URI(baseUrl.first, null, baseUrl.second, baseUrl.third, "/files" + image.path, null, null)
    return ImageDetails(image.id, image.sizeBytes, imageUrl, image.mimeType, image.transactionCount, image.itemCount)
}
fun transactionWithItemToCategory(categoryPojo: TransactionWithItemAndAccount): Category {
    return Category(categoryPojo.categoryId, categoryPojo.categoryName, categoryPojo.categoryIsCustom.toBoolean(), categoryPojo.categoryStringId, categoryPojo.categoryIcon)
}
fun categoryPojoToCategory(categoryPojo: CategoryPojo): Category {
    return Category(categoryPojo.id, categoryPojo.name, categoryPojo.isCustom.toBoolean(), categoryPojo.stringId, categoryPojo.icon)
}
fun categoryWithStatsToApiCategory(categoryWithStats: CategoryWithStats): CategoryDetails {
    return CategoryDetails(categoryWithStats.id, categoryWithStats.name, categoryWithStats.isCustom, categoryWithStats.stringId, categoryWithStats.icon, categoryWithStats.transactionCount, categoryWithStats.itemCount)
}

fun draftItemUpdateRequestToDraftItem(uuid: UUID, transactionDraftItemUpdateRequest: TransactionDraftItemUpdateRequest): TransactionDraftItem {
    val t = transactionDraftItemUpdateRequest
    return TransactionDraftItem(uuid, null, t.categoryId, t.amount, t.description, t.brand, t.variation, t.quantity,
        null, null, null, null, t.isReduction)
}

fun transactionDraftToApiDraft(draft: TransactionDraft, baseUrl: Triple<String, String, Int>): com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionDraft {
    val documents = draft.documents.map {
        val imageUrl = URI(baseUrl.first, null, baseUrl.second, baseUrl.third, "/files" + it.path, null, null)
        TransactionDraftFile(it.dbId, it.sizeBytes, imageUrl, it.mimeType, it.type?.toApiType(), it.sha256)
    }
    val items = draft.items.map { item ->
        val images = item.images.map {
            val imageUrl = URI(baseUrl.first, null, baseUrl.second, baseUrl.third, "/files" + it.path, null, null)
            TransactionDraftFile(it.dbId, it.sizeBytes, imageUrl, it.mimeType, null, it.sha256)
        }
        com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionDraftItem(item.dbId, item.uuid, item.amount, item.description, item.category, item.isReduction,
            item.quantity, item.brand, item.variation, images, emptyList())
    }
    val apiDraft = com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionDraft(
        items, emptyList(), documents, emptyList(), draft.customDate, draft.customTime?.toString(),
        draft.dbOriginalDate, draft.dbOriginalTime?.toString(), draft.accountId, draft.debitOrCredit, draft.note, draft.mode.toApiMode(), draft.sellerId, draft.sellerLocationId
    )
    return apiDraft
}