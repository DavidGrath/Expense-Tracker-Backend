package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.controller.exception.CustomHttpException
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.generatedapi.api.TransactionsApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.*
import com.davidgrath.expensetrackerbackend.service.*
import com.davidgrath.expensetrackerbackend.utils.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.hibernate.validator.constraints.CodePointLength
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.zone.ZoneRulesException
import java.util.*

@RestController
open class TransactionsController: TransactionsApi {

    @Autowired
    lateinit var transactionService: TransactionService
    @Autowired
    lateinit var transactionDraftService: TransactionDraftService
    @Autowired
    lateinit var userService: UserService
    @Autowired
    lateinit var profileService: ProfileService
    @Autowired
    lateinit var accountService: AccountService
    @Autowired
    lateinit var categoryService: CategoryService
    @Autowired
    lateinit var sellerService: SellerService
    @Autowired
    lateinit var sellerLocationService: SellerLocationService
    @Autowired
    lateinit var imageService: ImageService
    @Autowired
    lateinit var documentService: DocumentService

    @Autowired
    lateinit var httpServletRequest: HttpServletRequest

    override fun transactionsIdDelete(id: @NotNull Long, profileId: @NotNull String): ResponseEntity<Void?>? {
        throw UnsupportedOperationException("Not implemented yet")
    }

    override fun transactionsBasicPost(profileId: @NotNull String, basicTransactionCreateRequest: @Valid BasicTransactionCreateRequest?): ResponseEntity<Long?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

        val zone = try {
            ZoneId.of(basicTransactionCreateRequest!!.timeZone)
        } catch (e: DateTimeException) {
            val exception = CustomHttpException(ErrorCode.TIME_ZONE_NOT_VALID, HttpStatus.BAD_REQUEST)
            LOGGER.error("Bad time zone", exception)
            throw exception
        } catch (e: ZoneRulesException) {
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

    override fun transactionsGet(profileId: @NotNull String, accountIds: @NotNull @Valid List<Long?>, fromDate: @Valid LocalDate?, toDate: @Valid LocalDate?): ResponseEntity<List<Transaction?>?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)


        val scheme = httpServletRequest.scheme
        val host = httpServletRequest.serverName
        val port = httpServletRequest.serverPort
        val triple = Triple(scheme, host, port)

        val transactions = transactionService.getTransactionBasics(profile.id!!, fromDate, toDate, accountIds as List<Long>, triple)
        return ResponseEntity.ok(transactions)
    }


    override fun transactionsIdGet(id: @NotNull Long, profileId: @NotNull String): ResponseEntity<TransactionDetails?>? {

        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

        val t = transactionService.getByProfileIdAndId(profile.id!!, id)
        if(t == null) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Not found", exception)
            throw exception
        }
        val accountStats = accountService.getAccountByOrdinal(profile.id!!,t.accountOrdinal!!)!!
        val account = accountPojoToAccount(accountStats)
        val itemsPojo = transactionService.getItemsForTransaction(profile.id!!,t.id!!)
        val scheme = httpServletRequest.scheme
        val host = httpServletRequest.serverName
        val port = httpServletRequest.serverPort
        val triple = Triple(scheme, host, port)
        val items = itemsPojo.map { item ->
            val images = imageService.getImagesForItem(profile.id!!, item.id!!)
            val category = categoryService.getByProfileIdAndOrdinal(profile.id!!, item.primaryCategoryOrdinal!!)
            transactionItemPojoToTransactionItem(item, triple,category!!, images)
        }
        val mode = t.mode.toApiMode()
        val documentsPojo = documentService.getDocumentsByTransactionId(profile.id!!, t.id!!)
        val documents = documentsPojo.map { documentPojoToDocument(it, triple) }
        val details = TransactionDetails(t.id, t.amount, account, items, documents, t.datedAt, null, t.debitOrCredit.toBoolean(), t.note, mode)
        return ResponseEntity.ok(details)
    }

    override fun transactionsIdPut(profileId: @NotNull String, id: @NotNull Long, transactionDraft: @Valid TransactionDraft?): ResponseEntity<Void?>? {
        throw UnsupportedOperationException("Not implemented yet")
    }

    override fun transactionsPost(
        profileId: @NotNull String,
        timeZone: @NotNull @Valid String
    ): ResponseEntity<Void?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val userUuid = user!!.uuid!!
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

        if(!transactionDraftService.doesDraftExistForProfile(profile.id!!)) {
            val exception = CustomHttpException(ErrorCode.DRAFT_DOES_NOT_EXIST, HttpStatus.NOT_FOUND)
            LOGGER.error("Draft does not exist ", exception)
            throw exception

        }
        if(!transactionDraftService.validateDraft(profile.id!!)) {
            val exception = CustomHttpException(ErrorCode.DRAFT_NOT_VALID, HttpStatus.BAD_REQUEST)
            LOGGER.error("Invalid draft ", exception)
            throw exception
        }
        val zone = try {
            ZoneId.of(timeZone)
        } catch (e: DateTimeException) {
            val exception = CustomHttpException(ErrorCode.TIME_ZONE_NOT_VALID, HttpStatus.BAD_REQUEST)
            LOGGER.error("Bad time zone", exception)
            throw exception
        } catch (e: ZoneRulesException) {
            val exception = CustomHttpException(ErrorCode.TIME_ZONE_NOT_VALID, HttpStatus.BAD_REQUEST)
            LOGGER.error("Bad time zone", exception)
            throw exception
        }
        transactionDraftService.finishDraft(profile.id!!, userUuid, uuidToBytes(UUID.fromString(profileId)), zone)
        return ResponseEntity.ok().build()
    }

    override fun transactionsEarliestDateGet(
        profileId: @NotNull String,
        accountIds: @NotNull @Valid List<Long?>
    ): ResponseEntity<LocalDate?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

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

        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

        val suggestions = transactionService.getGenericSuggestions(profile.id!!, fieldType, query)
        return ResponseEntity.ok(suggestions)
    }

    override fun transactionsDraftGet(profileId: @NotNull String): ResponseEntity<TransactionDraft?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

        if(!transactionDraftService.doesDraftExistForProfile(profile.id!!)) {
            val exception = CustomHttpException(ErrorCode.DRAFT_DOES_NOT_EXIST, HttpStatus.NOT_FOUND)
            LOGGER.error("Draft does not exist ", exception)
            throw exception

        }
        val scheme = httpServletRequest.scheme
        val host = httpServletRequest.serverName
        val port = httpServletRequest.serverPort
        val triple = Triple(scheme, host, port)
        val draft = transactionDraftService.getDraft(profile.id!!)!!
        val apiDraft = transactionDraftToApiDraft(draft, triple)
        return ResponseEntity.ok(apiDraft)
    }

    override fun transactionsDraftItemsPost(profileId: @NotNull String): ResponseEntity<UUID?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

        if(!transactionDraftService.doesDraftExistForProfile(profile.id!!)) {
            val exception = CustomHttpException(ErrorCode.DRAFT_DOES_NOT_EXIST, HttpStatus.NOT_FOUND)
            LOGGER.error("Draft does not exist ", exception)
            throw exception
        }
        val uuid = transactionDraftService.addItemToDraft(profile.id!!)
        if(uuid == null) {
            val exception = CustomHttpException(ErrorCode.LIMITS_EXCEEDED, HttpStatus.BAD_REQUEST)
            LOGGER.error("Too many draft items ", exception)
            throw exception
        }
        return ResponseEntity.ok(uuid)
    }

    override fun transactionsDraftItemsUuidPut(
        profileId: @NotNull String,
        uuid: @NotNull UUID,
        transactionDraftItemUpdateRequest: @Valid TransactionDraftItemUpdateRequest?
    ): ResponseEntity<TransactionDraft?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

        if(!transactionDraftService.doesDraftExistForProfile(profile.id!!)) {
            val exception = CustomHttpException(ErrorCode.DRAFT_DOES_NOT_EXIST, HttpStatus.NOT_FOUND)
            LOGGER.error("Draft does not exist ", exception)
            throw exception

        }
        if(!categoryService.isCategoryForUser( user.id!!, transactionDraftItemUpdateRequest!!.categoryId)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Category not for user", exception)
            throw exception
        }
        if(!categoryService.isCategoryForProfile(profile.id!!, transactionDraftItemUpdateRequest!!.categoryId)) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_IN_PROFILE, HttpStatus.BAD_REQUEST)
            LOGGER.error("Category not in profile", exception)
            throw exception
        }
        transactionDraftService.modifyDraftItem(profile.id!!, uuid, draftItemUpdateRequestToDraftItem(uuid, transactionDraftItemUpdateRequest))
        val scheme = httpServletRequest.scheme
        val host = httpServletRequest.serverName
        val port = httpServletRequest.serverPort
        val triple = Triple(scheme, host, port)
        val draft = transactionDraftService.getDraft(profile.id!!)!!
        val apiDraft = transactionDraftToApiDraft(draft, triple)
        return ResponseEntity.ok(apiDraft)
    }

    override fun transactionsDraftPost(profileId: @NotNull String): ResponseEntity<TransactionDraft?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

        if(transactionDraftService.doesDraftExistForProfile(profile.id!!)) {
            val exception = CustomHttpException(ErrorCode.DRAFT_ALREADY_EXISTS, HttpStatus.BAD_REQUEST)
            LOGGER.error("Draft already exists ", exception)
            throw exception
        }
        val account = accountService.getAccountsForProfile(profile.id)[0]
        transactionDraftService.createDraft(profile.id!!, account.id!!)
        val scheme = httpServletRequest.scheme
        val host = httpServletRequest.serverName
        val port = httpServletRequest.serverPort
        val triple = Triple(scheme, host, port)
        val draft = transactionDraftService.getDraft(profile.id!!)!!
        val apiDraft = transactionDraftToApiDraft(draft, triple)
        return ResponseEntity.ok(apiDraft)
    }

    override fun transactionsDraftPut(
        profileId: @NotNull String,
        transactionDraftUpdateRequest: @Valid TransactionDraftUpdateRequest?
    ): ResponseEntity<TransactionDraft?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

        if(!transactionDraftService.doesDraftExistForProfile(profile.id!!)) {
            val exception = CustomHttpException(ErrorCode.DRAFT_DOES_NOT_EXIST, HttpStatus.NOT_FOUND)
            LOGGER.error("Draft does not exist ", exception)
            throw exception
        }
        val draft = transactionDraftService.getDraft(profile.id!!)!!
        if(draft.accountId != transactionDraftUpdateRequest!!.accountId) {

            if(!accountService.isAccountForUser(user.id!!, transactionDraftUpdateRequest.accountId)) {
                val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
                LOGGER.error("Account not for user", exception)
                throw exception
            }
            if(!accountService.isAccountInProfile(profile.id!!, transactionDraftUpdateRequest!!.accountId)) {
                val exception = CustomHttpException(ErrorCode.ENTITY_NOT_IN_PROFILE, HttpStatus.BAD_REQUEST)
                LOGGER.error("Account not in profile", exception)
                throw exception
            }
        }
        transactionDraftService.setDate(profile.id!!, transactionDraftUpdateRequest.date)
        transactionDraftService.setTime(profile.id!!, if(transactionDraftUpdateRequest.time == null) {
            null
        } else {
            LocalTime.parse(transactionDraftUpdateRequest.time)
        })
        if(transactionDraftUpdateRequest!!.sellerId != null && draft.sellerId != transactionDraftUpdateRequest!!.sellerId) {

            if(!sellerService.isSellerForUser(user.id!!, transactionDraftUpdateRequest.sellerId)) {
                val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
                LOGGER.error("Seller not for user", exception)
                throw exception
            }
            if(!sellerService.isSellerInProfile(profile.id!!, transactionDraftUpdateRequest!!.accountId)) {
                val exception = CustomHttpException(ErrorCode.ENTITY_NOT_IN_PROFILE, HttpStatus.BAD_REQUEST)
                LOGGER.error("Seller not in profile", exception)
                throw exception
            }
        }
        transactionDraftService.setSeller(profile.id!!, transactionDraftUpdateRequest.sellerId)

        if(transactionDraftUpdateRequest!!.sellerLocationId != null && draft.sellerLocationId != transactionDraftUpdateRequest!!.sellerLocationId) {

            if(!sellerLocationService.isSellerLocationForUser(user.id!!, transactionDraftUpdateRequest.sellerLocationId)) {
                val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
                LOGGER.error("Seller location not for user", exception)
                throw exception
            }
            if(!sellerLocationService.isSellerLocationInProfile(profile.id!!, transactionDraftUpdateRequest!!.sellerLocationId)) {
                val exception = CustomHttpException(ErrorCode.ENTITY_NOT_IN_PROFILE, HttpStatus.BAD_REQUEST)
                LOGGER.error("Seller location not in profile", exception)
                throw exception
            }
        }
        transactionDraftService.setSellerLocation(profile.id!!, transactionDraftUpdateRequest.sellerLocationId)

        transactionDraftService.modifyDraft(profile.id!!, transactionDraftUpdateRequest.accountId!!, transactionDraftUpdateRequest.note,
            mode = transactionDraftUpdateRequest.mode.toDbMode()?: TransactionMode.Other,
            transactionDraftUpdateRequest.debitOrCredit!!)
        val scheme = httpServletRequest.scheme
        val host = httpServletRequest.serverName
        val port = httpServletRequest.serverPort
        val triple = Triple(scheme, host, port)
        val newDraft = transactionDraftService.getDraft(profile.id!!)!!
        val apiDraft = transactionDraftToApiDraft(newDraft, triple)
        return ResponseEntity.ok(apiDraft)
    }

    override fun transactionsDraftDocumentsPositionDelete(
        profileId: @NotNull String,
        position: @NotNull Int
    ): ResponseEntity<Void?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

        if(!transactionDraftService.doesDraftExistForProfile(profile.id!!)) {
            val exception = CustomHttpException(ErrorCode.DRAFT_DOES_NOT_EXIST, HttpStatus.NOT_FOUND)
            LOGGER.error("Draft does not exist ", exception)
            throw exception

        }

        val draft = transactionDraftService.getDraft(profile.id!!)!!
        val documents = draft.documents
        if(position >= documents.size) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Document {} does not exist ", position, exception)
            throw exception
        }
        val ret = transactionDraftService.deleteDocument(profile.id, position)
        return ResponseEntity.ok().build()
    }

    override fun transactionsDraftDocumentsPost(
        profileId: @NotNull String,
        type: @Valid DocumentType?,
        document: MultipartFile?
    ): ResponseEntity<URI?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val userUuid = user!!.uuid!!
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

        LOGGER.debug("Document null: {}", document == null)
        LOGGER.debug("Resource type: {}", document!!.resource!!.javaClass.typeName)
        LOGGER.debug("Is open: {}", document!!.resource!!.isOpen)
        LOGGER.debug("File: {}", document!!.resource!!.isFile)

        if(document.size > Constants.Constraints.MAX_DOCUMENT_SIZE) {
            val exception = CustomHttpException(ErrorCode.FILE_TOO_LARGE, HttpStatus.BAD_REQUEST)
            LOGGER.error("File too large", exception)
            throw exception
        }
        val tempFolder = Files.createTempDirectory("addDocument").toFile()
        val tempFile = File(tempFolder, UUID.randomUUID().toString())
        val inputStream = document.resource.inputStream
        val outputStream = tempFile.outputStream()
        inputStream.transferTo(outputStream)
        inputStream.close()
        outputStream.close()

        val path = transactionDraftService.addDocumentToDraft(profile.id!!, type!!.toDbType(), tempFile, document.contentType?: MediaType.APPLICATION_OCTET_STREAM_VALUE,
            bytesToUUID(userUuid), UUID.fromString(profileId))

        tempFolder.deleteRecursively()
        if(path == null) {
            val exception = CustomHttpException(ErrorCode.INVALID_INPUT, HttpStatus.BAD_REQUEST)
            LOGGER.error("One of multiple possible issues adding the image", exception)
            throw exception
        }
        val scheme = httpServletRequest.scheme
        val host = httpServletRequest.serverName
        val port = httpServletRequest.serverPort
        val triple = Triple(scheme, host, port)
        LOGGER.debug("Baka: {}", triple)
        val uri = URI(triple.first, null, triple.second, triple.third, "/files" + path, null, null)
        return ResponseEntity.ok(uri)
    }

    override fun transactionsDraftItemsUuidImagesPositionDelete(
        profileId: @NotNull String,
        uuid: @NotNull UUID,
        position: @NotNull Int
    ): ResponseEntity<Void?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

        if(!transactionDraftService.doesDraftExistForProfile(profile.id!!)) {
            val exception = CustomHttpException(ErrorCode.DRAFT_DOES_NOT_EXIST, HttpStatus.NOT_FOUND)
            LOGGER.error("Draft does not exist ", exception)
            throw exception

        }

        val draft = transactionDraftService.getDraft(profile.id!!)!!
        val item = draft.items.find { it.uuid == uuid }
        if(item == null) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Item {} does not exist ", uuid, exception)
            throw exception
        }
        if(position >= item.images.size) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Image {} for item {} does not exist ", position, uuid, exception)
            throw exception
        }
        val ret = transactionDraftService.deleteItemImage(profile.id, uuid, position)
        return ResponseEntity.ok().build()
    }

    override fun transactionsDraftItemsUuidImagesPost(
        profileId: @NotNull String,
        uuid: @NotNull UUID,
        file: MultipartFile?
    ): ResponseEntity<TransactionDraft?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val userUuid = user!!.uuid!!
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

        LOGGER.debug("Resource type: {}", file!!.resource.javaClass.typeName)
        LOGGER.debug("Is open: {}", file!!.resource.isOpen)
        LOGGER.debug("File: {}", file!!.resource.isFile)

        if(file!!.size > Constants.Constraints.MAX_IMAGE_SIZE) {
            val exception = CustomHttpException(ErrorCode.FILE_TOO_LARGE, HttpStatus.BAD_REQUEST)
            LOGGER.error("File too large", exception)
            throw exception
        }
        val tempFolder = Files.createTempDirectory("addImages").toFile()
        val tempFile = File(tempFolder, UUID.randomUUID().toString())
        val inputStream = file.resource.inputStream
        val outputStream = tempFile.outputStream()
        inputStream.transferTo(outputStream)
        inputStream.close()
        outputStream.close()
        //TODO Validate for images or PDFs
        val added = transactionDraftService.addImageToItem(profile.id!!, uuid, tempFile, file.contentType?: MediaType.APPLICATION_OCTET_STREAM_VALUE,
            userUuid, uuidToBytes(UUID.fromString(profileId)))

        tempFolder.deleteRecursively()
        if(!added) {
            val exception = CustomHttpException(ErrorCode.INVALID_INPUT, HttpStatus.BAD_REQUEST)
            LOGGER.error("One of multiple possible issues adding the image", exception)
            throw exception
        }
        val scheme = httpServletRequest.scheme
        val host = httpServletRequest.serverName
        val port = httpServletRequest.serverPort
        val triple = Triple(scheme, host, port)
        LOGGER.debug("Baka: {}", triple)
        val draft = transactionDraftService.getDraft(profile.id!!)!!
        val apiDraft = transactionDraftToApiDraft(draft, triple)
        return ResponseEntity.ok(apiDraft)
    }

    override fun transactionsDraftItemsUuidDelete(
        profileId: @NotNull String,
        uuid: @NotNull UUID
    ): ResponseEntity<Void?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

        if(!transactionDraftService.doesDraftExistForProfile(profile.id!!)) {
            val exception = CustomHttpException(ErrorCode.DRAFT_DOES_NOT_EXIST, HttpStatus.NOT_FOUND)
            LOGGER.error("Draft does not exist ", exception)
            throw exception

        }

        val draft = transactionDraftService.getDraft(profile.id!!)!!
        val item = draft.items.find { it.uuid == uuid }
        if(item == null) {
            val exception = CustomHttpException(ErrorCode.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)
            LOGGER.error("Item {} does not exist ", uuid, exception)
            throw exception
        }
        if(draft.items.size == 1) {
            val exception = CustomHttpException(ErrorCode.DRAFT_MUST_HAVE_AT_LEAST_ONE_ITEM, HttpStatus.BAD_REQUEST)
            LOGGER.error("Only one item in draft", exception)
            throw exception
        }
        val ret = transactionDraftService.deleteItem(profile.id, uuid)
        return ResponseEntity.ok().build() //TODO all draft modification methods should return the new draft
    }

    override fun transactionsSearchPost(
        profileId: @NotNull String,
        query: @NotNull @Valid String,
        transactionSearchFilter: @Valid TransactionSearchFilter?
    ): ResponseEntity<List<TransactionDetails?>?>? {
        //TODO Remaining behaviour
//        doTheOtelThing(user.uuid!!, profile.uuid!!)
        if(!validateSearchString(query)) {
            val exception = CustomHttpException(ErrorCode.INVALID_SEARCH_QUERY, HttpStatus.BAD_REQUEST)
            LOGGER.error("Search query is invalid", exception)
            throw exception
        }
        return ResponseEntity.ok(emptyList())
    }

    override fun transactionsDraftItemsUuidVolumeDelete(
        profileId: @NotNull String,
        uuid: @NotNull UUID
    ): ResponseEntity<TransactionDraft?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

        val scheme = httpServletRequest.scheme
        val host = httpServletRequest.serverName
        val port = httpServletRequest.serverPort
        val triple = Triple(scheme, host, port)

        transactionDraftService.removeDraftItemVolume(profile.id!!, uuid)

        val draft = transactionDraftService.getDraft(profile.id!!)!!
        val apiDraft = transactionDraftToApiDraft(draft, triple)
        return ResponseEntity.ok(apiDraft)
    }

    override fun transactionsDraftItemsUuidVolumePatch(
        profileId: @NotNull String,
        uuid: @NotNull UUID,
        transactionDraftItemPatchVolumeRequest: @Valid TransactionDraftItemPatchVolumeRequest?
    ): ResponseEntity<TransactionDraft?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

        val scheme = httpServletRequest.scheme
        val host = httpServletRequest.serverName
        val port = httpServletRequest.serverPort
        val triple = Triple(scheme, host, port)

        transactionDraftService.modifyDraftItemVolume(profile.id!!, uuid,
            transactionDraftItemPatchVolumeRequest!!.amount!!, transactionDraftItemPatchVolumeRequest!!.prefix.value,
            transactionDraftItemPatchVolumeRequest.unit.value
        )

        val draft = transactionDraftService.getDraft(profile.id!!)!!
        val apiDraft = transactionDraftToApiDraft(draft, triple)
        return ResponseEntity.ok(apiDraft)
    }

    override fun transactionsDraftItemsUuidWeightDelete(
        profileId: @NotNull String,
        uuid: @NotNull UUID
    ): ResponseEntity<TransactionDraft?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

        val scheme = httpServletRequest.scheme
        val host = httpServletRequest.serverName
        val port = httpServletRequest.serverPort
        val triple = Triple(scheme, host, port)

        transactionDraftService.removeDraftItemWeight(profile.id!!, uuid)

        val draft = transactionDraftService.getDraft(profile.id!!)!!
        val apiDraft = transactionDraftToApiDraft(draft, triple)
        return ResponseEntity.ok(apiDraft)
    }

    override fun transactionsDraftItemsUuidWeightPatch(
        profileId: @NotNull String,
        uuid: @NotNull UUID,
        transactionDraftItemPatchWeightRequest: @Valid TransactionDraftItemPatchWeightRequest?
    ): ResponseEntity<TransactionDraft?>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as Jwt

        val user = userService.getByUsername(principal.subject!!)
        val profile = profileService.getByUserIdAndUuid(user!!.id!!, profileId!!)!!
        doTheOtelThing(user.uuid!!, profile.uuid!!)

        val scheme = httpServletRequest.scheme
        val host = httpServletRequest.serverName
        val port = httpServletRequest.serverPort
        val triple = Triple(scheme, host, port)

        transactionDraftService.modifyDraftItemWeight(profile.id!!, uuid,
            transactionDraftItemPatchWeightRequest!!.amount!!, transactionDraftItemPatchWeightRequest!!.prefix.value,
            transactionDraftItemPatchWeightRequest.unit.value
        )

        val draft = transactionDraftService.getDraft(profile.id!!)!!
        val apiDraft = transactionDraftToApiDraft(draft, triple)
        return ResponseEntity.ok(apiDraft)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionsController::class.java)
    }
}