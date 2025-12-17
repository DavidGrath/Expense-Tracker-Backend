package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetracker.generatedapi.api.TransactionsApi
import com.davidgrath.expensetracker.generatedapi.model.BasicTransactionCreateRequest
import com.davidgrath.expensetracker.generatedapi.model.Transaction
import com.davidgrath.expensetracker.generatedapi.model.TransactionDetails
import com.davidgrath.expensetracker.generatedapi.model.TransactionDraft
import com.davidgrath.expensetrackerbackend.configuration.CustomUserDetails
import com.davidgrath.expensetrackerbackend.controller.exception.CustomHttpException
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.service.AccountService
import com.davidgrath.expensetrackerbackend.service.CategoryService
import com.davidgrath.expensetrackerbackend.service.ProfileService
import com.davidgrath.expensetrackerbackend.service.TransactionService
import com.davidgrath.expensetrackerbackend.service.UserService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

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

    override fun transactionsIdDelete(
        id: Int?,
        profileId: @NotNull @Valid String?
    ): ResponseEntity<Void?>? {
        TODO("Not yet implemented")
    }

    override fun transactionsBasicPost(profileId: @NotNull @Valid String?, basicTransactionCreateRequest: @Valid BasicTransactionCreateRequest?): ResponseEntity<Long?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val userUuid = user!!.uuid!!
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!
        if(accountService.isAccountInProfile(profile.id!!, basicTransactionCreateRequest!!.accountId)) {
            val transactionId = transactionService.addTransaction(basicTransactionCreateRequest.accountId, basicTransactionCreateRequest.debitOrCredit, basicTransactionCreateRequest.amount, basicTransactionCreateRequest.description, basicTransactionCreateRequest.categoryId)
            return ResponseEntity.ok(transactionId)
        } else if(accountService.isAccountForUser(user!!.id!!, profile.id!!) || !categoryService.isCategoryForProfile(profile.id!!, basicTransactionCreateRequest.categoryId)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_IN_PROFILE, HttpStatus.BAD_REQUEST)
            LOGGER.error("Account not in profile", exception)
            throw exception
        } else {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Not found", exception)
            throw exception
        }
    }

    override fun transactionsGet(
        profileId: @NotNull @Valid String?,
        accountIds: @NotNull @Valid List<Long?>?,
        fromDate: @Valid LocalDate?,
        toDate: @Valid LocalDate?
    ): ResponseEntity<List<TransactionDetails?>?>? {
        TODO("Not yet implemented")
    }


    override fun transactionsIdGet(
        id: Int?,
        profileId: @NotNull @Valid String?
    ): ResponseEntity<Transaction?>? {
        TODO("Not yet implemented")
    }

    override fun transactionsIdPut(
        profileId: @NotNull @Valid String?,
        id: Int?,
        transactionDraft: @Valid TransactionDraft?
    ): ResponseEntity<Void?>? {
        TODO("Not yet implemented")
    }

    override fun transactionsPost(
        profileId: @NotNull @Valid String?,
        transactionDraft: @Valid TransactionDraft?
    ): ResponseEntity<Void?>? {
        TODO("Not yet implemented")
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionsController::class.java)
    }
}