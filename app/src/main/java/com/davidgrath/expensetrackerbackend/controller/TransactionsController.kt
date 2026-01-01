package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetracker.generatedapi.api.TransactionsApi
import com.davidgrath.expensetracker.generatedapi.model.BasicTransactionCreateRequest
import com.davidgrath.expensetracker.generatedapi.model.Document
import com.davidgrath.expensetracker.generatedapi.model.SuggestionsFieldType
import com.davidgrath.expensetracker.generatedapi.model.Transaction
import com.davidgrath.expensetracker.generatedapi.model.TransactionDetails
import com.davidgrath.expensetracker.generatedapi.model.TransactionDraft
import com.davidgrath.expensetracker.generatedapi.model.TransactionItem1
import com.davidgrath.expensetrackerbackend.controller.exception.CustomHttpException
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.service.AccountService
import com.davidgrath.expensetrackerbackend.service.CategoryService
import com.davidgrath.expensetrackerbackend.service.ProfileService
import com.davidgrath.expensetrackerbackend.service.TransactionService
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.utils.accountPojoToAccount
import com.davidgrath.expensetrackerbackend.utils.toApiMode
import com.davidgrath.expensetrackerbackend.utils.toBoolean
import com.davidgrath.expensetrackerbackend.utils.transactionItemPojoToTransactionItem
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.RestController
import java.time.DateTimeException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

@RestController
open class TransactionsController: TransactionsApi {

    @Autowired
    lateinit var transactionService: TransactionService
    @Autowired
    lateinit var userService: UserService
    @Autowired
    lateinit var profileService: ProfileService
    @Autowired
    lateinit var accountService: AccountService
    @Autowired
    lateinit var categoryService: CategoryService

    override fun transactionsIdDelete(id: @NotNull Long, profileId: @NotNull String): ResponseEntity<Void?>? {
        throw UnsupportedOperationException("Not implemented yet")
    }

    override fun transactionsBasicPost(profileId: @NotNull String, basicTransactionCreateRequest: @Valid BasicTransactionCreateRequest?): ResponseEntity<Long?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val userUuid = user!!.uuid!!
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!
        val zone = try {
            ZoneId.of(basicTransactionCreateRequest!!.timeZone)
        } catch (e: DateTimeException) {
            val exception = CustomHttpException(ErrorCode.TIME_ZONE_NOT_VALID, HttpStatus.BAD_REQUEST)
            LOGGER.error("Bad time zone", exception)
            throw exception
        }
        if(!accountService.isAccountForUser(profile.id!!, user.id!!)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Account not for user", exception)
            throw exception
        }
        if(!categoryService.isCategoryForUser(profile.id!!, user.id!!)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Category not for user", exception)
            throw exception
        }
        if(!accountService.isAccountInProfile(profile.id!!, basicTransactionCreateRequest!!.accountId)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_IN_PROFILE, HttpStatus.BAD_REQUEST)
            LOGGER.error("Account not in profile", exception)
            throw exception
        }
        if(!categoryService.isCategoryForProfile(profile.id!!, basicTransactionCreateRequest!!.accountId)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_IN_PROFILE, HttpStatus.BAD_REQUEST)
            LOGGER.error("Category not in profile", exception)
            throw exception
        }
        val transactionId = transactionService.addTransaction(profile.id!!, basicTransactionCreateRequest.accountId, basicTransactionCreateRequest.debitOrCredit, basicTransactionCreateRequest.amount, basicTransactionCreateRequest.description, basicTransactionCreateRequest.categoryId, zone)
        return ResponseEntity.ok(transactionId)
    }

    override fun transactionsGet(profileId: @NotNull String, accountIds: @NotNull @Valid List<Long?>, fromDate: @Valid LocalDate?, toDate: @Valid LocalDate?): ResponseEntity<List<TransactionDetails?>?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!
        val transactions = transactionService.getTransactions(profile.id!!, fromDate, toDate,
            accountIds as List<Long>, emptyList(), emptyList(), emptyList(), emptyList())
        val details = transactions.map { t ->
            val accountStats = accountService.getAccount(profile.id!!, t.accountId!!)!!
            val account = accountPojoToAccount(accountStats)
            val itemsPojo = transactionService.getItemsForTransaction(profile.id!!, t.id!!)
            val items = itemsPojo.map { item ->
                val category = categoryService.getByProfileIdAndId(profile.id!!, item.primaryCategoryId!!)
                transactionItemPojoToTransactionItem(item, category!!)
            }
            val mode = t.mode.toApiMode()
            val details = TransactionDetails(t.id, t.amount, account, items, emptyList<Document>(), t.datedAt, null, t.debitOrCredit.toBoolean(), t.note, mode)
            details
        }
        return ResponseEntity.ok(details)
    }


    override fun transactionsIdGet(id: @NotNull Long, profileId: @NotNull String): ResponseEntity<TransactionDetails?>? {

        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!
        val t = transactionService.getByUserIdAndId(user!!.id!!, id)
        if(t == null) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Not found", exception)
            throw exception
        }
        val accountStats = accountService.getAccount(profile.id!!,t.accountId!!)!!
        val account = accountPojoToAccount(accountStats)
        val itemsPojo = transactionService.getItemsForTransaction(profile.id!!,t.id!!)
        val items = itemsPojo.map { item ->
            val category = categoryService.getByProfileIdAndId(profile.id!!, item.primaryCategoryId!!)
            transactionItemPojoToTransactionItem(item, category!!)
        }
        val mode = t.mode.toApiMode()
        val details = TransactionDetails(t.id, t.amount, account, items, emptyList<Document>(), t.datedAt, null, t.debitOrCredit.toBoolean(), t.note, mode)
        return ResponseEntity.ok(details)
    }

    override fun transactionsIdPut(profileId: @NotNull String, id: @NotNull Long, transactionDraft: @Valid TransactionDraft?): ResponseEntity<Void?>? {
        throw UnsupportedOperationException("Not implemented yet")
    }

    override fun transactionsPost(profileId: @NotNull String, transactionDraft: @Valid TransactionDraft?): ResponseEntity<Void?>? {
        throw UnsupportedOperationException("Not implemented yet")
    }

    override fun transactionsEarliestDateGet(
        profileId: @NotNull String,
        accountIds: @NotNull @Valid List<Long?>
    ): ResponseEntity<OffsetDateTime?>? {
        TODO("Not yet implemented")
    }

    override fun transactionsIdItemsGet(
        id: @NotNull Long,
        profileId: @NotNull String
    ): ResponseEntity<List<TransactionItem1?>?>? {
        TODO("Not yet implemented")
    }

    override fun transactionsSuggestionsGet(
        profileId: @NotNull String,
        fieldType: @NotNull @Valid SuggestionsFieldType
    ): ResponseEntity<List<String?>?>? {
        TODO("Not yet implemented")
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionsController::class.java)
    }
}