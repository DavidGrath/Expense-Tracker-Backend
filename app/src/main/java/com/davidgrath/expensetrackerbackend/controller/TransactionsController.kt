package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.generatedapi.api.TransactionsApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.BasicTransactionCreateRequest
import com.davidgrath.expensetrackerbackend.generatedapi.model.Document
import com.davidgrath.expensetrackerbackend.generatedapi.model.SuggestionsFieldType
import com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionDetails
import com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionDraft
import com.davidgrath.expensetrackerbackend.controller.exception.CustomHttpException
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.generatedapi.model.DocumentType
import com.davidgrath.expensetrackerbackend.generatedapi.model.FileUploadRequest
import com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionDraftItemUpdateRequest
import com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionDraftUpdateRequest
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
import org.hibernate.validator.constraints.CodePointLength
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.time.DateTimeException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

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
        if(!accountService.isAccountForUser( user.id!!, basicTransactionCreateRequest!!.accountId)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Account not for user", exception)
            throw exception
        }
        if(!categoryService.isCategoryForUser( user.id!!, basicTransactionCreateRequest!!.categoryId)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Category not for user", exception)
            throw exception
        }
        if(!accountService.isAccountInProfile(profile.id!!, basicTransactionCreateRequest!!.accountId)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_IN_PROFILE, HttpStatus.BAD_REQUEST)
            LOGGER.error("Account not in profile", exception)
            throw exception
        }
        if(!categoryService.isCategoryForProfile(profile.id!!, basicTransactionCreateRequest!!.categoryId)) {
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
        val t = transactionService.getByProfileIdAndId(profile.id!!, id)
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

    override fun transactionsPost(
        profileId: @NotNull String,
        timeZone: @NotNull @Valid String
    ): ResponseEntity<Void?>? {
        TODO("Not yet implemented")
    }

    override fun transactionsEarliestDateGet(
        profileId: @NotNull String,
        accountIds: @NotNull @Valid List<Long?>
    ): ResponseEntity<LocalDate?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val userUuid = user!!.uuid!!
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!

        if(!accountService.areAllAccountsForUser(user.id!!, accountIds.map { it!! })) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Account not for user", exception)
            throw exception
        }
        if(!accountService.areAllAccountsForProfile(profile.id!!, accountIds.map { it!! })) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Category not for user", exception)
            throw exception
        }
        val date = transactionService.getEarliestDate(profile.id!!, accountIds.map { it!! })
        return ResponseEntity.ok(date)
    }

    override fun transactionsSuggestionsGet(
        profileId: @NotNull String,
        fieldType: @NotNull @Valid SuggestionsFieldType,
        query: @NotNull @CodePointLength(
            normalizationStrategy = CodePointLength.NormalizationStrategy.NFC,
            max = 20
        ) @Valid String
    ): ResponseEntity<List<String?>?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt
        LOGGER.info("Username: {}", principal.subject)
        val user = userService.getByUsername(principal.subject!!)
        val userUuid = user!!.uuid!!
        val profile = profileService.getByUserIdAndStringId(user!!.id!!, profileId!!)!!

        val suggestions = transactionService.getGenericSuggestions(profile.id!!, fieldType, query)
        return ResponseEntity.ok(suggestions)
    }

    override fun transactionsDraftGet(profileId: @NotNull String): ResponseEntity<TransactionDraft?>? {
        TODO("Not yet implemented")
    }

    override fun transactionsDraftItemsPost(profileId: @NotNull String): ResponseEntity<UUID?>? {
        TODO("Not yet implemented")
    }

    override fun transactionsDraftItemsUuidPut(
        profileId: @NotNull String,
        uuid: @NotNull UUID,
        transactionDraftItemUpdateRequest: @Valid TransactionDraftItemUpdateRequest?
    ): ResponseEntity<TransactionDraft?>? {
        TODO("Not yet implemented")
    }

    override fun transactionsDraftPost(profileId: @NotNull String): ResponseEntity<TransactionDraft?>? {
        TODO("Not yet implemented")
    }

    override fun transactionsDraftPut(
        profileId: @NotNull String,
        transactionDraftUpdateRequest: @Valid TransactionDraftUpdateRequest?
    ): ResponseEntity<TransactionDraft?>? {
        TODO("Not yet implemented")
    }

    override fun transactionsDraftDocumentsPositionDelete(
        profileId: @NotNull String,
        position: @NotNull Int
    ): ResponseEntity<Void?>? {
        TODO("Not yet implemented")
    }

    override fun transactionsDraftDocumentsPost(
        profileId: @NotNull String,
        type: @Valid DocumentType?,
        document: @Valid FileUploadRequest?
    ): ResponseEntity<TransactionDraft?>? {
        TODO("Not yet implemented")
    }

    override fun transactionsDraftItemsUuidImagesPositionDelete(
        profileId: @NotNull String,
        uuid: @NotNull UUID,
        position: @NotNull Int
    ): ResponseEntity<Void?>? {
        TODO("Not yet implemented")
    }

    override fun transactionsDraftItemsUuidImagesPost(
        profileId: @NotNull String,
        uuid: @NotNull UUID,
        file: MultipartFile?
    ): ResponseEntity<TransactionDraft?>? {
        TODO("Not yet implemented")
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionsController::class.java)
    }
}