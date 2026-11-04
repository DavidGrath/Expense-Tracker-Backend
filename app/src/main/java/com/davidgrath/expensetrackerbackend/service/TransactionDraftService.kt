package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.entities.*
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.*
import com.davidgrath.expensetrackerbackend.repository.*
import com.davidgrath.expensetrackerbackend.utils.*
import com.google.gson.GsonBuilder
import org.jooq.JSON
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

@Service
class TransactionDraftService {


    val gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateGsonAdapter())
        .registerTypeAdapter(LocalTime::class.java, LocalTimeGsonAdapter())
        .create()

    @Autowired
    lateinit var fileService: FileService

    @Autowired
    lateinit var userDraftRepository: UserDraftRepository

    @Autowired
    lateinit var categoryRepository: CategoryRepository

    @Autowired
    lateinit var accountRepository: AccountRepository

    @Autowired
    lateinit var transactionRepository: TransactionRepository

    @Autowired
    lateinit var transactionItemRepository: TransactionItemRepository

    @Autowired
    lateinit var itemImagesRepository: ItemImagesRepository

    @Autowired
    lateinit var sellerLocationRepository: SellerLocationRepository

    @Autowired
    lateinit var imageRepository: ImageRepository

    @Autowired
    lateinit var documentRepository: DocumentRepository
    @Autowired
    lateinit var genericRepository: GenericRepository

    @Autowired
    lateinit var timeHandler: TimeHandler

    fun createDraft(profileId: Long, accountId: Long) { //TODO Default account
        if (!doesDraftExistForProfile(profileId)) {
            val misc = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!
            val item = TransactionDraftItem(
                UUID.randomUUID(),
                null,
                misc.id!!,
                BigDecimal.ZERO,
                "",
                null,
                null,
                1,
                null,
                null,
                null,
                null,
                false,
                1_000
            )
            var draft = TransactionDraft(listOf(item), accountId)
            val jsonString = gson.toJson(draft)
            val date = ZonedDateTime.now(timeHandler.getClock())
            val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()
            val json = JSON.json(jsonString)
            val userDraft = UserDraftPojo(null, profileId, null, UserDraftType.Create, json, utcDate)
            userDraftRepository.insertDraft(userDraft)
        } else {
            LOGGER.info("Draft already exists")
        }
    }

    fun addItemToDraft(profileId: Long): UUID? {
        var uuidRet: UUID? = null
        genericRepository.transactionContext { trx ->
            val trxContext = trx.dsl()
            val userDraft = userDraftRepository.getDraftByProfileForUpdateTrx(trxContext, profileId)
            LOGGER.info("Acquired lock for profile draft")

            if (userDraft != null) {
                var transactionDraft = gson.fromJson(userDraft.draft.toString(), TransactionDraft::class.java)
                if (transactionDraft.items.size >= Constants.Constraints.MAX_ITEMS_PER_TRANSACTION) {
                    LOGGER.info("Too many items for draft")
                    return@transactionContext
                } else {
                    val uuid = UUID.randomUUID()
                    val misc = categoryRepository.findByProfileIdAndStringIdTrx(trxContext, profileId, "miscellaneous")!!
                    val item = TransactionDraftItem(uuid, null, misc.id!!, BigDecimal.ZERO, "")
                    transactionDraft = transactionDraft.copy(items = transactionDraft.items + item)
                    val jsonString = gson.toJson(transactionDraft)
                    val json = JSON.json(jsonString)
                    userDraftRepository.updateDraftTrx(trxContext, profileId, userDraft!!.id!!, json)
                    uuidRet = uuid
                }
            }
        }
        return uuidRet
    }

    fun getDraft(profileId: Long): TransactionDraft? {
        val userDraft = userDraftRepository.getDraftByProfile(profileId)
        if (userDraft == null) {
            return null
        }
        val transactionDraft = gson.fromJson(userDraft.draft.toString(), TransactionDraft::class.java)
        return transactionDraft
    }

    fun doesDraftExistForProfile(profileId: Long): Boolean {
        val draft = userDraftRepository.getDraftByProfile(profileId)
        return draft != null
    }

    fun modifyDraftItem(profileId: Long, uuid: UUID, item: TransactionDraftItem) {
        val userDraft = userDraftRepository.getDraftByProfile(profileId)
        if (userDraft == null) {
            //TODO
        }
        var transactionDraft = gson.fromJson(userDraft!!.draft.toString(), TransactionDraft::class.java)
        val itPos = transactionDraft.items.indexOfFirst { it.uuid == uuid }
        if (itPos == -1) {
            //TODO
        }
        val mutable = transactionDraft.items.toMutableList()
        mutable[itPos] = item
        transactionDraft = transactionDraft.copy(items = mutable)

        val jsonString = gson.toJson(transactionDraft)
        val json = JSON.json(jsonString)
        userDraftRepository.updateDraft(profileId, userDraft!!.id!!, json)
    }

    fun finishDraft(profileId: Long, userUuid: ByteArray, profileUuid: ByteArray, zoneId: ZoneId): Long {
        val start = System.currentTimeMillis()
        var transactionIdRet: Long = -1
        transactionRepository.transactionContext { trx ->
            val trxContext = trx.dsl()
        val userDraft = userDraftRepository.getDraftByProfileForUpdateTrx(trxContext, profileId)


        val userUuidString = bytesToUUID(userUuid).toString()
        val profileUuidString = bytesToUUID(profileUuid).toString()
        if (userDraft == null) {
            //TODO
        }
            validateDraft(userDraft!!)
        val draft = gson.fromJson(userDraft!!.draft.toString(), TransactionDraft::class.java)
        val total = draft.items.map {
            if (it.isReduction) {
                it.amount!!.times(BigDecimal(-1))
            } else {
                it.amount!!
            }
        }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }
        //TODO Dates, Times, and Ordinals
        val (datedDate, datedTime) = customDateTimeForDraft(zoneId, draft)
        val date = ZonedDateTime.now(timeHandler.getClock())
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC"))


        val actuallyUsedNewImages =
            draft.items.map { it.images }.fold(emptyList<TransactionDraftFile>()) { acc, list -> acc + list }
                .filter { it.dbId == null }.toSet()
        val imagesMap = mutableMapOf<String, Long>()



            for (image in actuallyUsedNewImages) {
                if (imagesMap[image.sha256!!] != null) {
                    continue
                }

                val draftFile =
                    File(timeHandler.getRoot(), Constants.FOLDER_NAME_USERS + "/${userUuidString}" + image.path)

                val path = fileService.moveFileToMain(
                    timeHandler.getRoot(),
                    userUuidString,
                    profileUuidString,
                    draftFile,
                    Constants.SUBFOLDER_NAME_IMAGES,
                    null
                )
                val mainFile = File(timeHandler.getRoot(), Constants.FOLDER_NAME_USERS + "/${userUuidString}" + path)

                val length = mainFile.length()
                val imagePojo = ImagePojo(
                    null,
                    profileId,
                    length,
                    HexFormat.of().parseHex(image.sha256),
                    image.mimeType,
                    "",
                    path,
                    utcDate.toLocalDateTime()
                )
                val imageId = imageRepository.insertImageTrx(trxContext, imagePojo)
                imagesMap[image.sha256] = imageId
            }

            if (actuallyUsedNewImages.isNotEmpty()) {
                LOGGER.info("Saved {} new images", actuallyUsedNewImages.size)
            }
            val note = if (draft.note.isNullOrBlank()) {
                null
            } else {
                draft.note
            }
            val account = accountRepository.getByProfileIdAndIdTrx(trxContext, profileId, draft.accountId!!)!!
            val maxSortKey =
                transactionRepository.getMaxSortKeyInDayForAccountTrx(trxContext, profileId, draft.accountId, datedDate) ?: 0
            var transactionSortKey = maxSortKey + 1000

            /* TODO Fix ordinals later
            if(datedTime != null) {
                val nextTransaction = transactionRepository.getLowestTimeAfterTimeInDayForAccount(draft.accountId, datedDate, datedTime).blockingGet()
                LOGGER.debug("nextTransaction: {}, datedTime: {}", nextTransaction, datedTime)
                val prevTransaction = transactionRepository.getHighestTimeBeforeTimeInDayForAccount(draft.accountId, datedDate, datedTime).blockingGet()
                if(nextTransaction != null) {
                    LOGGER.debug("prevTransaction: {}", prevTransaction)
    //                        if(prevTransaction != null) {
    //                            LOGGER.debug("Shift ordinals")
    //                        } else {
                    transactionOrdinal = nextTransaction.ordinal
                    val _ordinals = transactionRepository.getIdsAndOrdinalsFromDateForAccountSingle(draft.accountId, datedDate, transactionOrdinal).blockingGet()
                    val _ordinalsSize = _ordinals.size
                    val max = _ordinals.maxByOrNull { it.transactionOrdinal }?.transactionOrdinal?: 0
                    LOGGER.debug("newOrdinal: {}, max: {}", _ordinals, max)
                    _ordinals.forEachIndexed { index, transactionIdAndOrdinal ->

                        val newOrdinal = transactionOrdinal + 1 + _ordinalsSize + index + max
                        LOGGER.debug("newOrdinal: {}", newOrdinal)
                        transactionRepository.updateOrdinal(transactionIdAndOrdinal.transactionId, newOrdinal).blockingSubscribe()
                    }
    //                        }
                } else if(prevTransaction != null) {
                    transactionOrdinal = prevTransaction.ordinal + 1
                    val _ordinals = transactionRepository.getIdsAndOrdinalsFromDateForAccountSingle(draft.accountId, datedDate, transactionOrdinal).blockingGet()
                    val _ordinalsSize = _ordinals.size
                    val max = _ordinals.maxByOrNull { it.transactionOrdinal }?.transactionOrdinal?: 0

                    _ordinals.forEachIndexed { index, transactionIdAndOrdinal ->
                        val newOrdinal = transactionOrdinal + 2 + _ordinalsSize + index + max

                        transactionRepository.updateOrdinal(transactionIdAndOrdinal.transactionId, newOrdinal).blockingSubscribe()
                    }
                } else {
                    val maxTransactionOrdinal = transactionRepository.getMaxOrdinalInDayForAccount(draft.accountId, datedDate).blockingGet()?: 0
                    transactionOrdinal = maxTransactionOrdinal + 1
                }
            } else {
                val maxTransactionOrdinal = transactionRepository.getMaxOrdinalInDayForAccount(draft.accountId, datedDate).blockingGet()?: 0
                transactionOrdinal = maxTransactionOrdinal + 1
            }*/

            val transaction = TransactionPojo(
                null, account.id!!, total,
                draft.debitOrCredit.toByte(), datedDate, datedTime, transactionSortKey,
                draft.mode, note, draft.sellerId, draft.sellerLocationOrdinal, utcDate.toLocalDateTime()
            )



            val transactionId = transactionRepository.insertTransactionTrx(trxContext, transaction)
            LOGGER.info("Created new transaction")

            draft.documents.mapIndexed { i, document ->
                val occupancySlot = i + 1
                val draftFile =
                    File(timeHandler.getRoot(), Constants.FOLDER_NAME_USERS + "/${userUuidString}" + document.path)
                val localDate = draft.customDate ?: LocalDate.now(timeHandler.getClock())
                val path = fileService.moveFileToMain(
                    timeHandler.getRoot(),
                    userUuidString,
                    profileUuidString,
                    draftFile,
                    Constants.SUBFOLDER_NAME_DOCUMENTS,
                    localDate
                ) //TODO Don't delete the files until after the transaction is successful. Maybe write a test for that
                val mainFile = File(timeHandler.getRoot(), Constants.FOLDER_NAME_USERS + "/${userUuidString}" + path)

                val length = mainFile.length()
                val sortKey = i * 1000
                val document = DocumentPojo(
                    null,
                    transactionId,
                    document.type!!,
                    length,
                    HexFormat.of().parseHex(document.sha256),
                    document.mimeType,
                    "",
                    path,
                    sortKey,
                    occupancySlot,
                    utcDate.toLocalDateTime()
                )
                documentRepository.insertDocumentTrx(trxContext, document)
            }


            if (draft.documents.isNotEmpty()) {
                LOGGER.info("Saved {} documents to transaction", draft.documents.size)
            }

            draft.items.forEachIndexed { i, draftItem ->
                val occupancySlot = i + 1
                val sortKey = i * 1_000
                val item = if (draftItem.isReduction) {
                    TransactionItemPojo(
                        null,
                        transactionId,
                        draftItem.description!!,
                        draftItem.amount!!,
                        draftItem.category,
                        sortKey,
                        occupancySlot,
                        1,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        draftItem.isReduction.toByte(),
                        utcDate.toLocalDateTime()
                    )
                } else {
                    TransactionItemPojo(
                        null,
                        transactionId,
                        draftItem.description,
                        draftItem.amount!!,
                        draftItem.category,
                        sortKey,
                        occupancySlot,
                        draftItem.quantity,
                        draftItem.brand,
                        draftItem.variation,
                        draftItem.weight,
                        draftItem.weightUnit,
                        draftItem.volume,
                        draftItem.volumeUnit,
                        draftItem.isReduction.toByte(),
                        utcDate.toLocalDateTime()
                    )
                }
                val itemId = transactionItemRepository.insertTransactionItemTrx(trxContext, item)

                draftItem.images.forEachIndexed { i, image ->
                    val occupancySlot = i + 1
                    val sortKey = i * 1_000
                    val imageId = imagesMap[image.sha256] ?: image.dbId!!
                    val itemImage = TransactionItemImagesPojo(
                        null,
                        itemId,
                        imageId,
                        sortKey,
                        occupancySlot,
                        utcDate.toLocalDateTime()
                    )
                    itemImagesRepository.insertItemImageTrx(trxContext, itemImage)
                }


            }


            LOGGER.info("Saved {} items to transaction", draft.items.size)

            val res = userDraftRepository.deleteDraftTrx(trxContext, profileId, userDraft!!.id!!)
            LOGGER.info("Delete draft file result: {}", res!!)

            fileService.deleteDraftFiles(timeHandler.getRoot(), userUuidString, profileUuidString)

            transactionIdRet = transactionId


        }

        val end = System.currentTimeMillis()

        val time = end - start
        LOGGER.info("took {} ms", time)
        return transactionIdRet
    }

    private fun customDateTimeForDraft(zoneId: ZoneId, draft: TransactionDraft): Pair<LocalDate, LocalTime?> {
        val date = ZonedDateTime.now(timeHandler.getClock())

        val userDateTime = date.withZoneSameInstant(zoneId)
        val userDate = userDateTime.toLocalDate()
        val customDate = draft.customDate ?: userDateTime.toLocalDate()
        val customTime: LocalTime?
        if (customDate.isBefore(userDate)) {
            customTime = if (draft.customTime == null) {
                null
            } else {
                draft.customTime
            }
        } else {
            customTime = if (draft.customTime == null) {
                userDateTime.toLocalTime()
            } else {
                draft.customTime
            }
        }
        return customDate to customTime
    }

    fun validateDraft(userDraftPojo: UserDraftPojo): Boolean {
        val draft = gson.fromJson(userDraftPojo.draft.toString(), TransactionDraft::class.java)
        val items = draft.items
        var valid = true
        if (items.isEmpty()) {
            LOGGER.info("Draft is empty, nothing to save")
            return false
        }
        for (item in items) {
            if (item.amount == null) {
                LOGGER.info("An item has a null amount")
                valid = false
                break
            }
            if (item.amount.compareTo(BigDecimal.ZERO) == 0) {
                LOGGER.info("An item has a zero amount")
                valid = false
                break
            }
            if (item.description == null) {
                LOGGER.info("An item has a null description")
                valid = false
                break
            }
            if (item.description.isBlank()) {
                LOGGER.info("An item has a blank description")
                valid = false
                break
            }
        }
        if (valid) {
            val reductionSum = draft.items.map {
                if (it.isReduction) {
                    it.amount!!
                } else {
                    BigDecimal.ZERO //This implicitly covers the case of only reductions
                }
            }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }
            val regularSum = draft.items.map {
                if (!it.isReduction) {
                    it.amount!!
                } else {
                    BigDecimal.ZERO
                }
            }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }
            if (regularSum.compareTo(reductionSum) <= 0) {
                LOGGER.info("Sum of reductions cannot be greater than or equal to the sum of regular items")
                valid = false
            }
        }
        return valid
    }
    fun validateDraft(profileId: Long): Boolean {
        val userDraft = userDraftRepository.getDraftByProfile(profileId)
        if (userDraft == null) {
            //TODO
        }
        return validateDraft(userDraft!!)
    }

    fun setDate(profileId: Long, localDate: LocalDate?) {
        val userDraft = userDraftRepository.getDraftByProfile(profileId)
        if (userDraft == null) {
            //TODO
        }

        var transactionDraft = gson.fromJson(userDraft!!.draft.toString(), TransactionDraft::class.java)
        transactionDraft = transactionDraft.copy(customDate = localDate)

        val jsonString = gson.toJson(transactionDraft)
        val json = JSON.json(jsonString)
        userDraftRepository.updateDraft(profileId, userDraft!!.id!!, json)
    }

    fun modifyDraft(profileId: Long, accountId: Long, note: String?, mode: TransactionMode, debitOrCredit: Boolean) {
        val userDraft = userDraftRepository.getDraftByProfile(profileId)
        if (userDraft == null) {
            //TODO
        }

        var transactionDraft = gson.fromJson(userDraft!!.draft.toString(), TransactionDraft::class.java)
        transactionDraft = transactionDraft.copy(
            accountId = accountId, debitOrCredit = debitOrCredit!!,
            note = note,
            mode = mode
        )

        val jsonString = gson.toJson(transactionDraft)
        val json = JSON.json(jsonString)
        userDraftRepository.updateDraft(profileId, userDraft!!.id!!, json)
    }

    fun setTime(profileId: Long, localTime: LocalTime?) {
        val userDraft = userDraftRepository.getDraftByProfile(profileId)
        if (userDraft == null) {
            //TODO
        }

        var transactionDraft = gson.fromJson(userDraft!!.draft.toString(), TransactionDraft::class.java)
        transactionDraft = transactionDraft.copy(customTime = localTime)

        val jsonString = gson.toJson(transactionDraft)
        val json = JSON.json(jsonString)
        userDraftRepository.updateDraft(profileId, userDraft!!.id!!, json)
    }


    fun setSeller(profileId: Long, sellerId: Long?) {

        val userDraft = userDraftRepository.getDraftByProfile(profileId)
        if (userDraft == null) {
            //TODO
        }

        var transactionDraft = gson.fromJson(userDraft!!.draft.toString(), TransactionDraft::class.java)
        transactionDraft = transactionDraft.copy(sellerId = sellerId)



        if (sellerId == null) {
            LOGGER.info("Removed seller")
            transactionDraft = transactionDraft.copy(sellerLocationOrdinal = null)
            LOGGER.info("Removed seller location")
        } else {
            LOGGER.info("Changed seller")
        }

        val jsonString = gson.toJson(transactionDraft)
        val json = JSON.json(jsonString)
        userDraftRepository.updateDraft(profileId, userDraft!!.id!!, json)
    }

    fun setSellerLocation(profileId: Long, sellerLocationId: Long?) {

        val userDraft = userDraftRepository.getDraftByProfile(profileId)
        if (userDraft == null) {
            //TODO
        }

        var transactionDraft = gson.fromJson(userDraft!!.draft.toString(), TransactionDraft::class.java)


        if (sellerLocationId != null) {
            val sellerLocation = sellerLocationRepository.getByProfileIdAndId(profileId, sellerLocationId)
            if (transactionDraft.sellerId == sellerLocation!!.sellerId) {
                LOGGER.info("Changed seller location")
                transactionDraft = transactionDraft.copy(sellerLocationId = sellerLocation.id)
            } else {
                LOGGER.info("Seller location does not match seller")
            }
        } else {
            transactionDraft = transactionDraft.copy(sellerLocationId = sellerLocationId)
        }


        val jsonString = gson.toJson(transactionDraft)
        val json = JSON.json(jsonString)
        userDraftRepository.updateDraft(profileId, userDraft!!.id!!, json)
    }

    fun addImageToItem(
        profileId: Long,
        uuid: UUID,
        tempFile: File,
        mimeType: String,
        userUuid: ByteArray,
        profileUuid: ByteArray
    ): Boolean {
        val userDraft = userDraftRepository.getDraftByProfile(profileId)
        if (userDraft == null) {
            //TODO
            return false
        }
        var transactionDraft = gson.fromJson(userDraft!!.draft.toString(), TransactionDraft::class.java)
        val items = transactionDraft.items
        val itemIndex = items.indexOfFirst { it.uuid == uuid }
        if (itemIndex == -1) {
            // TODO
            return false
        }
        val item = items[itemIndex]
        val inputStream = tempFile.inputStream()
        val hashBytes = getSha256AsBytes(inputStream)
        val hash = sha256ToString(hashBytes)
        inputStream.close()
        val image = item!!.images.find { it.sha256 == hash }
        if (image != null) {
            LOGGER.info("Image already exists for item {}", uuid)
            return false
        }
        if (item.images.size >= Constants.Constraints.MAX_IMAGES_PER_ITEM) {
            LOGGER.info("Max image count reached for item {}", uuid)
            return false
        }
        val im = imageRepository.findBySha256(profileId!!, hashBytes)
        val draftPath = transactionDraft.imageHashes[hash]
        val draftFile: TransactionDraftFile
        if (im == null) {
            LOGGER.info("File does not exist for image")
            if (draftPath == null) {
                val userUuidString = bytesToUUID(userUuid).toString()
                val profileUuidString = bytesToUUID(profileUuid).toString()
                val filename = copyFileToDraft(
                    tempFile,
                    mimeType,
                    Constants.SUBFOLDER_NAME_IMAGES,
                    userUuidString,
                    profileUuidString
                )
                val path =
                    "/profiles/${profileUuidString}/${Constants.FOLDER_NAME_DRAFT}/${Constants.SUBFOLDER_NAME_IMAGES}/${filename}"
                draftFile = TransactionDraftFile(null, path, mimeType, hash, tempFile.length(), null, false)
            } else {
                LOGGER.info("Image already exists in draft. Adding to item {}", uuid)
                draftFile = TransactionDraftFile(null, draftPath, mimeType, hash, tempFile.length(), null, false)
            }
        } else {
            LOGGER.info("File already exists in DB for image")
//            val uri = URI.create(im.uri!!)
            draftFile = TransactionDraftFile(
                im.id!!,
                im.path!!,
                im.mimeType!!,
                sha256ToString(im.sha_256!!),
                im.sizeBytes!!,
                null,
                false
            )
        }
        val newImages = item.images + draftFile
        val newItem = item.copy(images = newImages)
        val newItems = items.toMutableList()
        newItems[itemIndex] = newItem
        val existingPath = transactionDraft.imageHashes[hash]
        val newHashes = if (existingPath == null) {
            LOGGER.info("Image path added to HashMap")
            transactionDraft.imageHashes + (hash to draftFile.path)
        } else {
            transactionDraft.imageHashes
        }
        transactionDraft = transactionDraft.copy(items = newItems, imageHashes = newHashes)

        val jsonString = gson.toJson(transactionDraft)
        val json = JSON.json(jsonString)
        userDraftRepository.updateDraft(profileId, userDraft!!.id!!, json)
        return true
    }

    fun addDocumentToDraft(
        profileId: Long,
        type: DocumentType,
        tempFile: File,
        mimeType: String,
        userUuid: String,
        profileStringId: String
    ): String? {

        val userDraft = userDraftRepository.getDraftByProfile(profileId)
        if (userDraft == null) {
            //TODO
            return null
        }
        val transactionDraft = gson.fromJson(userDraft!!.draft.toString(), TransactionDraft::class.java)

        val newHashes = transactionDraft.documentHashes.toMutableMap()
        val documents = transactionDraft.documents
        val newDocuments = documents.toMutableList()

        val inputStream = tempFile.inputStream()
        val hash = getSha256(inputStream)
        inputStream.close()
        val image = documents.find { it.sha256 == hash }
        if (image != null) {
            LOGGER.info("Document already exists for draft")
            return null
        }
        if (documents.size >= Constants.Constraints.MAX_DOCUMENTS_PER_TRANSACTION) {
            LOGGER.info("Max document count reached for draft")
            return null
        }

        val draftFile: TransactionDraftFile
        val path: String
        var draftPath = newHashes[hash]

        if (draftPath == null) {
            LOGGER.info("File does not exist in draft for document")
            val filename =
                copyFileToDraft(tempFile, mimeType, Constants.SUBFOLDER_NAME_DOCUMENTS, userUuid, profileStringId)
            path =
                "/profiles/${profileStringId}/${Constants.FOLDER_NAME_DRAFT}/${Constants.SUBFOLDER_NAME_DOCUMENTS}/${filename}"
            draftFile = TransactionDraftFile(null, path, mimeType, hash, tempFile.length(), type, false)

            newHashes[hash] = path
        } else {
            LOGGER.info("File already exists in draft for document")
            path = draftPath
            draftFile = TransactionDraftFile(null, draftPath, mimeType, hash, tempFile.length(), type, false)
        }

        newDocuments.add(draftFile)
        val updatedDraft = transactionDraft.copy(documents = newDocuments, documentHashes = newHashes)

        val jsonString = gson.toJson(updatedDraft)
        val json = JSON.json(jsonString)
        userDraftRepository.updateDraft(profileId, userDraft!!.id!!, json)
        return path
    }

    fun deleteItem(profileId: Long, uuid: UUID): Boolean {

        val userDraft = userDraftRepository.getDraftByProfile(profileId)
        if (userDraft == null) {
            //TODO
            return false
        }
        val transactionDraft = gson.fromJson(userDraft!!.draft.toString(), TransactionDraft::class.java)

        val currentList = transactionDraft.items

        val itemPos = currentList.indexOfFirst { it.uuid == uuid }
        if (itemPos == -1) {
            return false
        }
        if (currentList.size == 1) {
            LOGGER.info("Too few items")
            return false
        }

        val newItems = currentList.toMutableList().apply {
            removeAt(itemPos)
        }
        val updatedDraft = transactionDraft.copy(items = newItems)
        val jsonString = gson.toJson(updatedDraft)
        val json = JSON.json(jsonString)
        userDraftRepository.updateDraft(profileId, userDraft!!.id!!, json)
        return true

    }

    fun deleteItemImage(profileId: Long, uuid: UUID, imagePosition: Int): Boolean {
        val userDraft = userDraftRepository.getDraftByProfile(profileId)
        if (userDraft == null) {
            //TODO
            return false
        }
        val transactionDraft = gson.fromJson(userDraft!!.draft.toString(), TransactionDraft::class.java)

        val newItems = transactionDraft.items.toMutableList()

        val itemPos = newItems.indexOfFirst { it.uuid == uuid }
        if (itemPos == -1) {
            return false
        }

        val item = newItems[itemPos]

        val newImages = item.images.toMutableList()
        if (imagePosition >= newImages.size) {
            LOGGER.info("Image position out of bounds")
            return false
        }
        newImages.removeAt(imagePosition)
        LOGGER.info("Removed image at position {} for item {}", imagePosition, uuid)
        val newItem = item.copy(images = newImages)
        newItems[itemPos] = newItem
        val updatedDraft = transactionDraft.copy(items = newItems)
        val jsonString = gson.toJson(updatedDraft)
        val json = JSON.json(jsonString)
        userDraftRepository.updateDraft(profileId, userDraft!!.id!!, json)
        return true
    }

    fun deleteDocument(profileId: Long, documentPosition: Int): Boolean { //TODO Place an SFU lock
        val userDraft = userDraftRepository.getDraftByProfile(profileId)
        if (userDraft == null) {
            //TODO
            return false
        }
        val transactionDraft = gson.fromJson(userDraft!!.draft.toString(), TransactionDraft::class.java)

        val newDocuments = transactionDraft.documents.toMutableList()

        if (documentPosition >= newDocuments.size) {
            LOGGER.info("Document position out of bounds")
            return false
        }
        newDocuments.removeAt(documentPosition)
        LOGGER.info("Removed document at position {}", documentPosition)

        val updatedDraft = transactionDraft.copy(documents = newDocuments)
        val jsonString = gson.toJson(updatedDraft)
        val json = JSON.json(jsonString)
        userDraftRepository.updateDraft(profileId, userDraft!!.id!!, json)
        return true
    }

    private fun copyFileToDraft(
        file: File,
        mimeType: String,
        subfolder: String,
        userUuid: String,
        profileUuid: String
    ): String {
        val folder = file(
            timeHandler.getRoot(),
            Constants.FOLDER_NAME_USERS, userUuid,
            Constants.FOLDER_NAME_PROFILES, profileUuid,
            Constants.FOLDER_NAME_DRAFT, subfolder
        )
        folder.mkdirs()
        val filename = UUID.randomUUID().toString()
        val extension = when (mimeType) {
            "image/jpeg" -> ".jpg"
            "image/png" -> ".png"
            "application/pdf" -> ".pdf"
            else -> ""
        }
        val filenameExt = "$filename$extension"

        val inputStream = file.inputStream()
        val destination = File(folder, filenameExt)
        val outputStream = destination.outputStream()
        inputStream.copyTo(outputStream) //TODO I plan on using bucket storage eventually
        inputStream.close()
        outputStream.close()
        LOGGER.info("Created file {}", destination.path)
        return filenameExt
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionDraftService::class.java)
    }
}