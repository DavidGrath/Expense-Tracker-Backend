package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.entities.UserDraftType
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionItemImagesPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionItemPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.UserDraftPojo
import com.davidgrath.expensetrackerbackend.entities.TransactionDraft
import com.davidgrath.expensetrackerbackend.entities.TransactionDraftFile
import com.davidgrath.expensetrackerbackend.entities.TransactionDraftItem
import com.davidgrath.expensetrackerbackend.repository.CategoryRepository
import com.davidgrath.expensetrackerbackend.repository.ItemImagesRepository
import com.davidgrath.expensetrackerbackend.repository.SellerLocationRepository
import com.davidgrath.expensetrackerbackend.repository.TransactionItemRepository
import com.davidgrath.expensetrackerbackend.repository.TransactionRepository
import com.davidgrath.expensetrackerbackend.repository.UserDraftRepository
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.LocalDateGsonAdapter
import com.davidgrath.expensetrackerbackend.utils.LocalTimeGsonAdapter
import com.davidgrath.expensetrackerbackend.utils.toByte
import com.davidgrath.expensetrackerbackend.utils.toDbMode
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.jooq.JSON
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

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
    lateinit var accountService: AccountService
    @Autowired
    lateinit var transactionRepository: TransactionRepository
    @Autowired
    lateinit var transactionItemRepository: TransactionItemRepository
    @Autowired
    lateinit var itemImagesRepository: ItemImagesRepository
    @Autowired
    lateinit var sellerLocationRepository: SellerLocationRepository
    @Autowired
    lateinit var timeHandler: TimeHandler

    fun createDraft(profileId: Long, accountId: Long) { //TODO Default account
        if(!doesDraftExistForProfile(profileId)) {
            val misc = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!
            val item = TransactionDraftItem(UUID.randomUUID(), null, misc.id!!, BigDecimal.ZERO, "", null, null, null, 1, false, 1)
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

    fun addItemToDraft(profileId: Long): Int {
        val userDraft = userDraftRepository.getDraftByProfile(profileId)
        if(userDraft != null) {
            var transactionDraft = gson.fromJson(userDraft.draft.toString(), TransactionDraft::class.java)
            if(transactionDraft.items.size >= Constants.Constraints.MAX_ITEMS_PER_TRANSACTION) {
                LOGGER.info("Too many items for draft")
                return 0
            } else {
                val misc = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!
                val item = TransactionDraftItem(UUID.randomUUID(), null, misc.id!!,BigDecimal.ZERO, "")
                transactionDraft = transactionDraft.copy(items = transactionDraft.items + item)
                val jsonString = gson.toJson(transactionDraft)
                val json = JSON.json(jsonString)
                userDraftRepository.updateDraft(profileId, userDraft!!.id!!, json)
                return 1
            }
        } else {
            return 0
        }
    }

    fun getDraft(profileId: Long): TransactionDraft? {
        val userDraft = userDraftRepository.getDraftByProfile(profileId)
        if(userDraft == null) {
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
        if(userDraft == null) {
            //TODO
        }
        var transactionDraft = gson.fromJson(userDraft!!.draft.toString(), TransactionDraft::class.java)
        val itPos = transactionDraft.items.indexOfFirst { it.uuid == uuid }
        if(itPos == -1) {
            //TODO
        }
        val mutable = transactionDraft.items.toMutableList()
        mutable[itPos] = item
        transactionDraft = transactionDraft.copy(items = mutable)

        val jsonString = gson.toJson(transactionDraft)
        val json = JSON.json(jsonString)
        userDraftRepository.updateDraft(profileId, userDraft!!.id!!, json)
    }

    fun finishDraft(profileId: Long, zoneId: ZoneId): Long {
        val start = System.currentTimeMillis()
        val userDraft = userDraftRepository.getDraftByProfile(profileId)
        if(userDraft == null) {
            //TODO
        }
        val draft = gson.fromJson(userDraft!!.draft.toString(), TransactionDraft::class.java)
            val total = draft.items.map {
                if(it.isReduction) {
                    it.amount!!.times(BigDecimal(-1))
                } else {
                    it.amount!!
                }
            }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }
            //TODO Dates, Times, and Ordinals
            val (datedDate, datedTime) = customDateTimeForDraft(zoneId, draft)
            val date = ZonedDateTime.now(timeHandler.getClock())
            val utcDate = date.withZoneSameInstant(ZoneId.of("UTC"))


            val actuallyUsedNewImages = draft.items.map { it.images }.fold(emptyList<TransactionDraftFile>()) { acc, list -> acc + list }.filter { it.dbId == null }.toSet()
            val imagesMap = mutableMapOf<String, Long>()
            /* TODO Images now have 2 URIs - the one for the user to use, and the local filesystem URI - fix that
            for(image in actuallyUsedNewImages) {
                if(imagesMap[image.sha256!!] != null) {
                    continue
                }
                val file = File(image.uri!!)
                val hash = image.sha256
                val mimeType = image.mimeType
                val mainFile = fileHandler.moveFileToMain(file, Constants.SUBFOLDER_NAME_IMAGES).blockingGet()
                val finalUri = mainFile.toUri()

                val length = mainFile.length()
                val imageDb = ImageDb(null, profileId!!, length, hash, mimeType, finalUri.toString(), dateTimeString, offset, zone)
                val imageId = imageDao.insertImage(imageDb).blockingGet()
                imagesMap[image.sha256] = imageId
            }

             */
            if(actuallyUsedNewImages.isNotEmpty()) {
                LOGGER.info("saveDraft: Saved {} new images", actuallyUsedNewImages.size)
            }
            val note = if(draft.note.isNullOrBlank()) {
                null
            } else {
                draft.note
            }
            val account = accountService.getAccount(profileId, draft.accountId!!)!!
            val maxOrdinal = transactionRepository.getMaxOrdinalInDayForAccount(profileId, draft.accountId, datedDate)?: 0
            var transactionOrdinal = maxOrdinal + 1

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

            val transaction = TransactionPojo(null, account.id!!, total, account.currencyCode,
                null, draft.debitOrCredit.toByte(), datedDate, transactionOrdinal,
                draft.mode, note, draft.sellerId, draft.sellerLocationId, utcDate.toLocalDateTime(), datedTime)

            val transactionId = transactionRepository.insertTransaction(transaction)
                LOGGER.info("finishDraft: Created new transaction")
                    /*
                    draft.documents.map { evidence ->
                    val file = evidence.uri.toFile()
                    val localDate = draft.customDate ?: LocalDate.now(timeAndLocaleHandler.getClock())
                    val year = String.format("%04d", localDate.year)
                    val month = String.format("%02d", localDate.monthValue)
                    val day = String.format("%02d", localDate.dayOfMonth)
                    val mainFile = fileHandler.moveFileToMain(file, Constants.SUBFOLDER_NAME_DOCUMENTS + File.separator + year + File.separator + month + File.separator + day).blockingGet()
                    val finalUri = mainFile.toUri()

                    val length = mainFile.length()
                    val evidence = EvidenceDb(null, id, length, evidence.sha256, evidence.mimeType, finalUri.toString(), dateTimeString, offset, zone)
                    evidenceDao.insertEvidence(evidence).blockingGet()
                    }

                     */
                if(draft.documents.isNotEmpty()) {
                    LOGGER.info("saveDraft: Saved {} documents to transaction", draft.documents.size)
                }
                var itemOrdinal = 1
                draft.items.forEach { draftItem ->

                    val item = if(draftItem.isReduction) {
                        TransactionItemPojo(null, transactionId, draftItem.description!!, draftItem.amount!!, draftItem.category, itemOrdinal++,1, null,
                            null, null, draftItem.isReduction.toByte(), utcDate.toLocalDateTime())
                    } else {
                        TransactionItemPojo(null, transactionId, draftItem.description, draftItem.amount!!, draftItem.category, itemOrdinal++, draftItem.quantity, draftItem.brand,
                            draftItem.variation, draftItem.referenceNumber, draftItem.isReduction.toByte(),
                            utcDate.toLocalDateTime())
                    }
                    val itemId = transactionItemRepository.insertTransactionItem(item)

                            draftItem.images.forEach { image ->
                                val imageId = imagesMap[image.sha256]?: image.dbId!!
                                val itemImage = TransactionItemImagesPojo(null, itemId, imageId, utcDate.toLocalDateTime())
                                itemImagesRepository.insertItemImage(itemImage)
                            }


                }


                LOGGER.info("saveDraft: Saved {} items to transaction", draft.items.size)

                userDraftRepository.deleteDraft(profileId, userDraft!!.id!!)

                fileService.deleteDraftFiles(profileId)


        val end = System.currentTimeMillis()

            val time = end  - start
            LOGGER.info("took {} ms", time)
        return transactionId
    }

    private fun customDateTimeForDraft(zoneId: ZoneId, draft: TransactionDraft): Pair<LocalDate, LocalTime?> {
        val date = ZonedDateTime.now(timeHandler.getClock())

        val userDateTime = date.withZoneSameInstant(zoneId)
        val userDate = userDateTime.toLocalDate()
        val customDate = draft.customDate ?: userDateTime.toLocalDate()
        val customTime: LocalTime?
        if(customDate.isBefore(userDate)) {
            customTime = if(draft.customTime == null) {
                null
            } else {
                draft.customTime
            }
        } else {
            customTime = if(draft.customTime == null) {
                userDateTime.toLocalTime()
            } else {
                draft.customTime
            }
        }
        return customDate to customTime
    }

    fun validateDraft(profileId: Long): Boolean {
        val userDraft = userDraftRepository.getDraftByProfile(profileId)
        if(userDraft == null) {
            //TODO
        }
        val draft = gson.fromJson(userDraft!!.draft.toString(), TransactionDraft::class.java)
        val items = draft.items
        var valid = true
        if(items.isEmpty()) {
            LOGGER.info("validateDraft: Draft is empty, nothing to save")
            return false
        }
        for (item in items) {
            if (item.amount == null) {
                LOGGER.info("validateDraft: An item has a null amount")
                valid = false
                break
            }
            if (item.amount.compareTo(BigDecimal.ZERO) == 0) {
                LOGGER.info("validateDraft: An item has a zero amount")
                valid = false
                break
            }
            if (item.description == null) {
                LOGGER.info("validateDraft: An item has a null description")
                valid = false
                break
            }
            if (item.description.isBlank()) {
                LOGGER.info("validateDraft: An item has a blank description")
                valid = false
                break
            }
        }
        if(valid) {
            val reductionSum = draft.items.map {
                if(it.isReduction) {
                    it.amount!!
                } else {
                    BigDecimal.ZERO //This implicitly covers the case of only reductions
                }
            }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }
            val regularSum = draft.items.map {
                if(!it.isReduction) {
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

    fun setDate(profileId: Long, localDate: LocalDate) {
        val userDraft = userDraftRepository.getDraftByProfile(profileId)
        if(userDraft == null) {
            //TODO
        }

        var transactionDraft = gson.fromJson(userDraft!!.draft.toString(), TransactionDraft::class.java)
        transactionDraft = transactionDraft.copy(customDate = localDate)

        val jsonString = gson.toJson(transactionDraft)
        val json = JSON.json(jsonString)
        userDraftRepository.updateDraft(profileId, userDraft!!.id!!, json)
    }

    fun setTime(profileId: Long, localTime: LocalTime) {
        val userDraft = userDraftRepository.getDraftByProfile(profileId)
        if(userDraft == null) {
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
        if(userDraft == null) {
            //TODO
        }

        var transactionDraft = gson.fromJson(userDraft!!.draft.toString(), TransactionDraft::class.java)
        transactionDraft = transactionDraft.copy(sellerId = sellerId)



        if(sellerId == null) {
            LOGGER.info("Removed seller")
            transactionDraft = transactionDraft.copy(sellerLocationId = null)
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
        if(userDraft == null) {
            //TODO
        }

        var transactionDraft = gson.fromJson(userDraft!!.draft.toString(), TransactionDraft::class.java)


        if(sellerLocationId != null) {
            val sellerLocation = sellerLocationRepository.getByProfileIdAndId(profileId, sellerLocationId)
            if(transactionDraft.sellerId == sellerLocation!!.sellerId) {
                LOGGER.info("Changed seller location")
                transactionDraft = transactionDraft.copy(sellerLocationId = sellerLocationId)
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

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionDraftService::class.java)
    }
}