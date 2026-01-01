package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetracker.generatedapi.api.StatisticsApi
import com.davidgrath.expensetracker.generatedapi.model.StatisticsFilter
import com.davidgrath.expensetracker.generatedapi.model.StatsTransactionAndItemCount
import com.davidgrath.expensetracker.generatedapi.model.TotalAmountByCategory
import com.davidgrath.expensetracker.generatedapi.model.TotalAmountByDate
import com.davidgrath.expensetracker.generatedapi.model.TransactionDetails
import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.service.AccountService
import com.davidgrath.expensetrackerbackend.service.ProfileService
import com.davidgrath.expensetrackerbackend.service.TransactionService
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.utils.toDbMode
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

    override fun statisticsTotalExpensesByCategoryPost(
        profileId: @NotNull String,
        statisticsFilter: @Valid StatisticsFilter?
    ): ResponseEntity<List<TotalAmountByCategory?>?>? {
        TODO("Not yet implemented")
    }

    override fun statisticsTotalExpensesByDatePost(
        profileId: @NotNull String,
        statisticsFilter: @Valid StatisticsFilter?
    ): ResponseEntity<List<TotalAmountByDate?>?>? {
        TODO("Not yet implemented")
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
            statisticsFilter.to, statisticsFilter.accounts, statisticsFilter.includeDates, statisticsFilter.categories, modes, emptyList())
        return ResponseEntity.ok(total)
    }

    override fun statisticsTotalIncomeByCategoryPost(
        profileId: @NotNull String,
        statisticsFilter: @Valid StatisticsFilter?
    ): ResponseEntity<List<TotalAmountByCategory?>?>? {
        TODO("Not yet implemented")
    }

    override fun statisticsTotalIncomeByDatePost(
        profileId: @NotNull String,
        statisticsFilter: @Valid StatisticsFilter?
    ): ResponseEntity<List<TotalAmountByDate?>?>? {
        TODO("Not yet implemented")
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
            statisticsFilter.to, statisticsFilter.accounts, statisticsFilter.includeDates, statisticsFilter.categories, modes, emptyList())
        return ResponseEntity.ok(total)
    }

    override fun statisticsTransactionsPost(
        profileId: @NotNull String,
        statisticsFilter: @Valid StatisticsFilter?
    ): ResponseEntity<List<TransactionDetails?>?>? {
        TODO("Not yet implemented")
    }

    override fun statisticsTransactionAndItemCountPost(
        profileId: @NotNull String,
        statisticsFilter: @Valid StatisticsFilter?
    ): ResponseEntity<StatsTransactionAndItemCount?>? {
        TODO("Not yet implemented")
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StatisticsController::class.java)
    }
}