package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.generatedapi.api.StatisticsApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.Category
import com.davidgrath.expensetrackerbackend.generatedapi.model.Document
import com.davidgrath.expensetrackerbackend.generatedapi.model.StatisticsFilter
import com.davidgrath.expensetrackerbackend.generatedapi.model.StatsTransactionAndItemCount
import com.davidgrath.expensetrackerbackend.generatedapi.model.TotalAmountByCategory
import com.davidgrath.expensetrackerbackend.generatedapi.model.TotalAmountByDate
import com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionDetails
import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.service.AccountService
import com.davidgrath.expensetrackerbackend.service.CategoryService
import com.davidgrath.expensetrackerbackend.service.ProfileService
import com.davidgrath.expensetrackerbackend.service.TransactionService
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.utils.accountPojoToAccount
import com.davidgrath.expensetrackerbackend.utils.toApiMode
import com.davidgrath.expensetrackerbackend.utils.toBoolean
import com.davidgrath.expensetrackerbackend.utils.toDbMode
import com.davidgrath.expensetrackerbackend.utils.transactionItemPojoToTransactionItem
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

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

    override fun statisticsTotalExpensesByCategoryPost(
        profileId: @NotNull String,
        statisticsFilter: @Valid StatisticsFilter?
    ): ResponseEntity<List<TotalAmountByCategory?>?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val userUuid = user!!.uuid!!
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!
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
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!
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
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!
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
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!
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
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!
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
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!
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
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!
        val modes = statisticsFilter!!.transactionModes.map { m -> m.toDbMode()?: TransactionMode.Other }
        val transactions = transactionService.getTransactions(profile.id!!, statisticsFilter.from,
            statisticsFilter.to, statisticsFilter.accounts, statisticsFilter.includeDates, statisticsFilter.categories, modes, statisticsFilter.sellers)
        val details = transactions.map { t ->
            val accountStats = accountService.getAccount(profile.id!!, t.accountId!!)!!
            val account = accountPojoToAccount(accountStats)
            val itemsPojo = transactionService.getItemsForTransaction(profile.id!!, t.id!!)
            val items = itemsPojo.map { item ->
                val category = categoryService.getByProfileIdAndId(profile.id!!, item.primaryCategoryId!!)
                transactionItemPojoToTransactionItem(item, category!!)
            }
            val mode = t.mode.toApiMode()
            val details = TransactionDetails(t.id, t.amount, account, items, emptyList<Document>(), t.datedAt, t.time?.toString(), t.debitOrCredit.toBoolean(), t.note, mode)
            details
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
        val userUuid = user!!.uuid!!
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!
        val modes = statisticsFilter!!.transactionModes.map { m -> m.toDbMode()?: TransactionMode.Other }
        val counts = transactionService.getTransactionAndItemCount(profile.id!!, statisticsFilter.from,
            statisticsFilter.to, statisticsFilter.accounts, statisticsFilter.includeDates, statisticsFilter.categories, modes, statisticsFilter.sellers)
        return ResponseEntity.ok(StatsTransactionAndItemCount(counts.transactionCount, counts.itemCount))
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StatisticsController::class.java)
    }
}