package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ImagePojo
import com.davidgrath.expensetrackerbackend.generatedapi.api.StatisticsApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.*
import com.davidgrath.expensetrackerbackend.service.*
import com.davidgrath.expensetrackerbackend.utils.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

//TODO General ownership check for all IDs in Statistics filter
@RestController
class StatisticsController: StatisticsApi {

    @Autowired
    lateinit var userService: UserService
    @Autowired
    lateinit var profileService: ProfileService
    @Autowired
    lateinit var transactionService: TransactionService
    @Autowired
    lateinit var accountService: AccountService
    @Autowired
    lateinit var categoryService: CategoryService
    @Autowired
    lateinit var imageService: ImageService
    @Autowired
    lateinit var documentService: DocumentService

    @Autowired
    lateinit var httpServletRequest: HttpServletRequest

    override fun statisticsTotalExpensesByCategoryPost(
        profileId: @NotNull String,
        statisticsFilter: @Valid StatisticsFilter?
    ): ResponseEntity<List<TotalAmountByCategory?>?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val userUuid = user!!.uuid!!
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        val modes = statisticsFilter!!.transactionModes.map { m -> m.toDbMode()?: TransactionMode.Other }
        val total = transactionService.getTransactionDebitSumByCategory(profile.id!!, statisticsFilter.from,
            statisticsFilter.to, statisticsFilter.accounts, statisticsFilter.includeDates, statisticsFilter.categories, modes, statisticsFilter.sellers)
        val mapped = total.map { TotalAmountByCategory(Category(it.categoryId, it.name, it.isCustom, it.stringId, it.categoryIcon), it.sum) }
        return ResponseEntity.ok(mapped)
    }

    override fun statisticsTotalExpensesByDatePost(
        profileId: @NotNull String,
        statisticsFilter: @Valid StatisticsFilter?
    ): ResponseEntity<List<TotalAmountByDate?>?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val userUuid = user!!.uuid!!
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        val modes = statisticsFilter!!.transactionModes.map { m -> m.toDbMode()?: TransactionMode.Other }
        val total = transactionService.getTotalAmountByDate(profile.id!!, true, statisticsFilter.from,
            statisticsFilter.to, statisticsFilter.accounts, statisticsFilter.includeDates, statisticsFilter.categories, modes, statisticsFilter.sellers)
        val mapped = total.map { TotalAmountByDate(it.aggregateDate, it.sum) }
        return ResponseEntity.ok(mapped)
    }

    override fun statisticsTotalExpensesPost(
        profileId: @NotNull String,
        statisticsFilter: @Valid StatisticsFilter?
    ): ResponseEntity<BigDecimal?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val userUuid = user!!.uuid!!
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        val modes = statisticsFilter!!.transactionModes.map { m -> m.toDbMode()?: TransactionMode.Other }
        val total = transactionService.getTransactionDebitSum(profile.id!!, statisticsFilter.from,
            statisticsFilter.to, statisticsFilter.accounts, statisticsFilter.includeDates, statisticsFilter.categories, modes, statisticsFilter.sellers)
        return ResponseEntity.ok(total)
    }

    override fun statisticsTotalIncomeByCategoryPost(
        profileId: @NotNull String,
        statisticsFilter: @Valid StatisticsFilter?
    ): ResponseEntity<List<TotalAmountByCategory?>?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val userUuid = user!!.uuid!!
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        val modes = statisticsFilter!!.transactionModes.map { m -> m.toDbMode()?: TransactionMode.Other }
        val total = transactionService.getTransactionCreditSumByCategory(profile.id!!, statisticsFilter.from,
            statisticsFilter.to, statisticsFilter.accounts, statisticsFilter.includeDates, statisticsFilter.categories, modes, statisticsFilter.sellers)
        val mapped = total.map { TotalAmountByCategory(Category(it.categoryId, it.name, it.isCustom, it.stringId, it.categoryIcon), it.sum) }
        return ResponseEntity.ok(mapped)
    }

    override fun statisticsTotalIncomeByDatePost(
        profileId: @NotNull String,
        statisticsFilter: @Valid StatisticsFilter?
    ): ResponseEntity<List<TotalAmountByDate?>?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val userUuid = user!!.uuid!!
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        val modes = statisticsFilter!!.transactionModes.map { m -> m.toDbMode()?: TransactionMode.Other }
        val total = transactionService.getTotalAmountByDate(profile.id!!, false, statisticsFilter.from,
            statisticsFilter.to, statisticsFilter.accounts, statisticsFilter.includeDates, statisticsFilter.categories, modes, statisticsFilter.sellers)
        val mapped = total.map { TotalAmountByDate(it.aggregateDate, it.sum) }
        return ResponseEntity.ok(mapped)
    }

    override fun statisticsTotalIncomePost(
        profileId: @NotNull String,
        statisticsFilter: @Valid StatisticsFilter?
    ): ResponseEntity<BigDecimal?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val userUuid = user!!.uuid!!
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        val modes = statisticsFilter!!.transactionModes.map { m -> m.toDbMode()?: TransactionMode.Other }
        val total = transactionService.getTransactionCreditSum(profile.id!!, statisticsFilter.from,
            statisticsFilter.to, statisticsFilter.accounts, statisticsFilter.includeDates, statisticsFilter.categories, modes, statisticsFilter.sellers)
        return ResponseEntity.ok(total)
    }

    override fun statisticsTransactionsPost(
        profileId: @NotNull String,
        statisticsFilter: @Valid StatisticsFilter?
    ): ResponseEntity<List<TransactionDetails?>?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        val modes = statisticsFilter!!.transactionModes.map { m -> m.toDbMode()?: TransactionMode.Other }
        val transactions = transactionService.getTransactionsWithItems(profile.id!!, statisticsFilter.from,
            statisticsFilter.to, statisticsFilter.accounts, statisticsFilter.includeDates, statisticsFilter.categories, modes, statisticsFilter.sellers)

        val documentPojos = documentService.getDocumentsFiltered(profile.id!!, statisticsFilter.from,
            statisticsFilter.to, statisticsFilter.accounts, statisticsFilter.includeDates, statisticsFilter.categories, modes, statisticsFilter.sellers)

        val documentsMap = documentPojos.groupBy { it.transactionId }

        val imagePojos = imageService.getImagesFiltered(profile.id!!, statisticsFilter.from,
            statisticsFilter.to, statisticsFilter.accounts, statisticsFilter.includeDates, statisticsFilter.categories, modes, statisticsFilter.sellers)
        val imagesMap = imagePojos.groupBy { it.itemId }

        val scheme = httpServletRequest.scheme
        val host = httpServletRequest.serverName
        val port = httpServletRequest.serverPort
        val triple = Triple(scheme, host, port)

        val details = arrayListOf<TransactionDetails>()

        val length = transactions.size
        var items = arrayListOf<TransactionItem>()
        transactions.forEachIndexed { i, t ->

            val account = accountPojoToAccount(t)



            val imagePojos = imagesMap[t.itemId]?:emptyList()
            /*val images = if(t.itemImageCount > 0) {
                imageService.getImagesForItem(profile.id!!, t.itemId!!)
            } else {
                emptyList()
            }*/
            val images = imagePojos.map { ImagePojo(it.id, it.profileId, it.sizeBytes, it.sha_256, it.mimeType, it.uri, it.path, it.createdAt) }

            items.add(transactionWithItemToTransactionItem(t, triple, images))

            val mode = t.transactionMode.toApiMode()
            /*val documentsPojo = if(t.documentCount > 0) {
                documentService.getDocumentsByTransactionId(profile.id!!, t.transactionId!!)
            } else {
                emptyList()
            }*/

            val documentsPojo = documentsMap[t.transactionId]?: emptyList()
            val documents = documentsPojo.map { documentPojoToDocument(it, triple) }
            val time = t.transactionTime?.toString()
            val next = i + 1
            if(next >= length) {
                val detail = TransactionDetails(t.transactionId, t.transactionAmount, account, items, documents, t.transactionDatedAt, time, t.transactionDebitOrCredit.toBoolean(), t.transactionNote, mode)
                details.add(detail)
                items = arrayListOf()
            } else {
                if(transactions[next].transactionId != t.transactionId) {
                    val detail = TransactionDetails(t.transactionId, t.transactionAmount, account, items, documents, t.transactionDatedAt, time, t.transactionDebitOrCredit.toBoolean(), t.transactionNote, mode)
                    details.add(detail)
                    items = arrayListOf()
                }
            }
        }

        return ResponseEntity.ok(details)
    }

    override fun statisticsTransactionAndItemCountPost(
        profileId: @NotNull String,
        statisticsFilter: @Valid StatisticsFilter?
    ): ResponseEntity<StatsTransactionAndItemCount?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        val counts = transactionService.getTransactionAndItemCount(profile.id!!, statisticsFilterToStatisticsFilterDb(statisticsFilter!!))
        return ResponseEntity.ok(StatsTransactionAndItemCount(counts.transactionCount, counts.itemCount))
    }

    override fun statisticsCumSumExpensesByDatePost(
        profileId: @NotNull String,
        statisticsFilter: @Valid StatisticsFilter?
    ): ResponseEntity<List<TotalAmountByDate?>?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val userUuid = user!!.uuid!!
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        val modes = statisticsFilter!!.transactionModes.map { m -> m.toDbMode()?: TransactionMode.Other }
        val total = transactionService.getCumSumByDate(profile.id!!, true, statisticsFilter.from,
            statisticsFilter.to, statisticsFilter.accounts, statisticsFilter.includeDates, statisticsFilter.categories, modes, statisticsFilter.sellers)
        val mapped = total.map { TotalAmountByDate(it.aggregateDate, it.sum) }
        return ResponseEntity.ok(mapped)
    }

    override fun statisticsCumSumIncomeByDatePost(
        profileId: @NotNull String,
        statisticsFilter: @Valid StatisticsFilter?
    ): ResponseEntity<List<TotalAmountByDate?>?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val userUuid = user!!.uuid!!
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        val modes = statisticsFilter!!.transactionModes.map { m -> m.toDbMode()?: TransactionMode.Other }
        val total = transactionService.getCumSumByDate(profile.id!!, false, statisticsFilter.from,
            statisticsFilter.to, statisticsFilter.accounts, statisticsFilter.includeDates, statisticsFilter.categories, modes, statisticsFilter.sellers)
        val mapped = total.map { TotalAmountByDate(it.aggregateDate, it.sum) }
        return ResponseEntity.ok(mapped)
    }

    override fun statisticsTopNExpensesPerCategoryPost(
        profileId: @NotNull String,
        n: @NotNull @Min(
            value = 1
        ) @Max(value = 3) @Valid Int,
        statisticsFilter: @Valid StatisticsFilter?
    ): ResponseEntity<List<TotalAmountPerCategory>?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val userUuid = user!!.uuid!!
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        val modes = statisticsFilter!!.transactionModes.map { m -> m.toDbMode()?: TransactionMode.Other }
        val total = transactionService.getTopNDebitPerCategory(profile.id!!, n,statisticsFilter.from,
            statisticsFilter.to, statisticsFilter.accounts, statisticsFilter.includeDates, statisticsFilter.categories, modes, statisticsFilter.sellers)
        val mapped = total.groupBy {it.categoryId}.map { (id, t) ->
            val c = t[0]
            TotalAmountPerCategory(Category(id, c.name, c.isCustom, c.stringId, c.categoryIcon), c.sum, t.map {
                TotalAmountPerCategoryItemSummary(it.itemId, it.itemAmount, it.itemDescription) }) }
        return ResponseEntity.ok(mapped)
    }

    override fun statisticsTopNIncomePerCategoryPost(
        profileId: @NotNull String,
        n: @NotNull @Min(
            value = 1
        ) @Max(value = 3) @Valid Int,
        statisticsFilter: @Valid StatisticsFilter?
    ): ResponseEntity<List<TotalAmountPerCategory>?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val userUuid = user!!.uuid!!
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        val modes = statisticsFilter!!.transactionModes.map { m -> m.toDbMode()?: TransactionMode.Other }
        val total = transactionService.getTopNCreditPerCategory(profile.id!!, n,statisticsFilter.from,
            statisticsFilter.to, statisticsFilter.accounts, statisticsFilter.includeDates, statisticsFilter.categories, modes, statisticsFilter.sellers)
        val mapped = total.groupBy {it.categoryId}.map { (id, t) ->
            val c = t[0]
            TotalAmountPerCategory(Category(id, c.name, c.isCustom, c.stringId, c.categoryIcon), c.sum, t.map {
                TotalAmountPerCategoryItemSummary(it.itemId, it.itemAmount, it.itemDescription) }) }
        return ResponseEntity.ok(mapped)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StatisticsController::class.java)
    }
}