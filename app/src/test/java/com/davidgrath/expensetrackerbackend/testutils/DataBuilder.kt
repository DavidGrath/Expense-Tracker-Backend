package com.davidgrath.expensetrackerbackend.testutils

import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.entities.DocumentType
import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.*
import com.davidgrath.expensetrackerbackend.repository.*
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.file
import com.davidgrath.expensetrackerbackend.utils.toByte
import com.davidgrath.expensetrackerbackend.utils.uuidToBytes
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.HexFormat
import java.util.UUID

class DataBuilder(private val root: File, private val timeHandler: TimeHandler, private val userUuid: String?, private val profileUuid: String,
                  private val testUtilComponent: TestUtilComponent
    ) {
    fun createTransaction(): TransactionBuilder {
        val transactionBuilder = TransactionBuilderImpl(root, timeHandler, userUuid, profileUuid, testUtilComponent.transactionRepository, testUtilComponent.categoryRepository, testUtilComponent.profileRepository, testUtilComponent.accountRepository, testUtilComponent.itemRepository, testUtilComponent.imageRepository, testUtilComponent.itemImagesRepository, testUtilComponent.userRepository, testUtilComponent.documentRepository, testUtilComponent.sellerRepository, testUtilComponent.sellerLocationRepository)
        return transactionBuilder
    }

    fun createBasicTransaction(description: String, category: String, amount: BigDecimal): Long {
        val transactionBuilder = TransactionBuilderImpl(root, timeHandler, userUuid, profileUuid, testUtilComponent.transactionRepository, testUtilComponent.categoryRepository, testUtilComponent.profileRepository, testUtilComponent.accountRepository, testUtilComponent.itemRepository, testUtilComponent.imageRepository, testUtilComponent.itemImagesRepository, testUtilComponent.userRepository, testUtilComponent.documentRepository, testUtilComponent.sellerRepository, testUtilComponent.sellerLocationRepository)
            .withItem(description, category, amount)
        val id = transactionBuilder.commit().first()
        return id
    }

    private class TransactionBuilderImpl(
        private val root: File, private val timeHandler: TimeHandler, private val _userUuid: String?, private val profileUuid: String, private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository, private val profileRepository: ProfileRepository, private val accountRepository: AccountRepository,
        private val transactionItemRepository: TransactionItemRepository, private val imagesRepository: ImageRepository, private val itemImagesRepository: ItemImagesRepository,
        private val userRepository: UserRepository, val documentRepository: DocumentRepository, val sellerRepository: SellerRepository, private val sellerLocationRepository: SellerLocationRepository
        ): TransactionBuilder {
        private val items = arrayListOf<TransactionItemPojo>()
        private var account: AccountPojo
        private var newAccount: AccountPojo? = null
        private val repeatDates = arrayListOf<LocalDate>()
        private var defaultItemPrice: BigDecimal? = null
        private var debitOrCredit: Boolean = true
        private var date: LocalDate? = null
        private var time: LocalTime? = null
        private var timeSet = false
        private var useDatesAsCreationDates: Boolean = false
        private var customMode: TransactionMode? = null

        private var customSeller: SellerPojo? = null
        private var customSellerLocation: SellerLocationPojo? = null


        private var hasCommitted = false
        private var imagesMap = mutableMapOf<Int, List<TestData.Resource>>()
        private var documents = arrayListOf<TestData.Resource>()

        private val profile: ProfilePojo
        private val userUuid = _userUuid?: TestConstants.DEFAULT_USER_ID

        init {
            val user = userRepository.findByUuid(uuidToBytes(UUID.fromString(userUuid)))!!
            profile = profileRepository.findByUuid(user.id!!, uuidToBytes(UUID.fromString(profileUuid)))!!
            account = accountRepository.getAllByProfileId(profile.id!!).first()
        }

        override fun withDefaultItemPrice(defaultPrice: BigDecimal): TransactionBuilder {
            this.defaultItemPrice = defaultPrice
            return this
        }

        override fun withItem(description: String, category: String, amount: BigDecimal): TransactionBuilder {
            val categoryDb = categoryRepository.findByProfileIdAndStringId(profile.id!!, category)
            if(categoryDb == null) {
                throw IllegalStateException("Category does not exist")
            }
            items.add(TransactionItemPojo(null, profile.id, -1, -1, description, amount, categoryDb.ordinal, 1, 1_000, 1,1, null, null, null,
                null, null, null,false.toByte(), LocalDateTime.now()))
            return this
        }

        override fun withItem(description: String): TransactionBuilder {
            if(defaultItemPrice == null) {
                throw IllegalStateException("Default item price not set")
            }
            return withItem(description, "miscellaneous", defaultItemPrice!!)
        }

        override fun withNewAccount(currencyCode: String, accountName: String): TransactionBuilder {
            if(newAccount != null) {
                throw IllegalStateException("New account already added")
            }
            val dateTime = LocalDateTime.now(timeHandler.getClock())
            val ordinalSortKeyOccupancy = accountRepository.getMaxOrdinalSortKeyAndOccupancySlot(profile.id!!)
            val nextOrdinal = ((ordinalSortKeyOccupancy.ordinal?: 0) + 1).toShort()
            val nextSortKey = ((ordinalSortKeyOccupancy.sortKey?: 0) + 1_000).toShort()
            val nextOccupancy = ((ordinalSortKeyOccupancy.occupancySlot?: 0) + 1).toShort()
            val xAccount = AccountPojo(null, profile.id!!, currencyCode, null, "", accountName, nextOrdinal, nextSortKey, nextOccupancy, dateTime)
            val accountId = accountRepository.insertAccount(xAccount)
            newAccount = AccountPojo(accountId, profile.id!!, currencyCode, null, "", accountName, nextOrdinal, nextSortKey, nextOccupancy, dateTime)
            return this
        }

        override fun withAccountId(accountId: Long): TransactionBuilder {
            newAccount = accountRepository.findByProfileIdAndId(profile!!.id!!, accountId)
            return this
        }

        override fun repeatIntoDates(dates: List<LocalDate>): TransactionBuilder {
            this.repeatDates.addAll(dates)
            return this
        }

        override fun atDate(date: LocalDate): TransactionBuilder {
            if(this.date != null) {
                throw IllegalStateException("Date has already been set")
            }
            this.date = date
            return this
        }

        override fun atTime(time: LocalTime): TransactionBuilder {
            this.time = time
            this.timeSet = true
            return this
        }

        override fun debitOrCredit(debitOrCredit: Boolean): TransactionBuilder {
            this.debitOrCredit = debitOrCredit
            return this
        }

        override fun getAccount(): AccountPojo {
            return newAccount?: account
        }

        override fun useDatesAsCreationDates(b: Boolean): TransactionBuilder {
            this.useDatesAsCreationDates = b
            return this
        }

        override fun commit(): List<Long> {
            if(hasCommitted) {
                throw IllegalStateException("Transaction already committed")
            }
            if(items.isEmpty()) {
                throw IllegalStateException("Transaction must have at least one item")
            }
            val sum = items.map { it.amount }.reduce { acc, bigDecimal -> acc!!.plus(bigDecimal!!) }
            val account = newAccount?: account
            val firstDate = this.date?: LocalDate.now(timeHandler.getClock())
            val time = if(timeSet) {
                this.time?: LocalTime.now(timeHandler.getClock())
            } else {
                this.time
            }
            var dateTime = LocalDateTime.now(timeHandler.getClock())

            val repeats = listOf(firstDate) + repeatDates
            val sha256Map = mutableMapOf<String, Short>()
            val classLoader = TransactionItemBuilderImpl::class.java.classLoader
            val mainImagesFolder = file(root, Constants.FOLDER_NAME_USERS, userUuid, Constants.FOLDER_NAME_PROFILES, profileUuid, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_IMAGES)
            mainImagesFolder.mkdirs()
            val mainDocumentsFolder = file(root, Constants.FOLDER_NAME_USERS, userUuid, Constants.FOLDER_NAME_PROFILES, profileUuid, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_DOCUMENTS)
            mainDocumentsFolder.mkdirs()

            val mode = customMode?: TransactionMode.Other

            val ids = repeats.map { repeatDate ->
                if(useDatesAsCreationDates) {
                    dateTime = repeatDate.atTime(LocalTime.parse("08:00:00"))
                }

                val ordinalSortKeyOccupancy = transactionRepository.getMaxOrdinalSortKeyAndOccupancySlot(profile.id!!, account.id!!)
                val nextOrdinal = ((ordinalSortKeyOccupancy.ordinal?: 0) + 1).toShort()
                val nextSortKey = ((ordinalSortKeyOccupancy.sortKey?: 0) + 1_000).toShort()
                val nextOccupancy = ((ordinalSortKeyOccupancy.occupancySlot?: 0) + 1).toShort()

                if(customSellerLocation != null) {
                    val ordinalSortKeyOccupancy = sellerLocationRepository.getMaxOrdinalSortKeyAndOccupancySlot(profile.id!!, customSeller!!.id!!)
                    val nextOrdinal = ((ordinalSortKeyOccupancy.ordinal?: 0) + 1).toShort()
                    val nextSortKey = ((ordinalSortKeyOccupancy.sortKey?: 0) + 1_000).toShort()
                    val nextOccupancy = ((ordinalSortKeyOccupancy.occupancySlot?: 0) + 1).toShort()
                    val fixedLocation = customSellerLocation!!.copy(ordinal = nextOrdinal, sortKey = nextSortKey, occupancySlot = nextOccupancy)
                    val newLocationId = sellerLocationRepository.insertSellerLocation(fixedLocation)
                    customSellerLocation = sellerLocationRepository.getByProfileIdAndId(profile!!.id!!, newLocationId)
                }
                val transaction = TransactionPojo(null, profile.id!!,  sum,  debitOrCredit.toByte(), repeatDate, time, mode,null, account.ordinal!!, customSeller?.ordinal, customSellerLocation?.ordinal,
                    nextOrdinal, nextSortKey, nextOccupancy, dateTime)
                val transactionId = transactionRepository.insertTransaction(transaction)

                documents.forEachIndexed { i, doc ->
                    val resourceInputStream = classLoader.getResourceAsStream(doc.resourceName)
                    val file = File(mainDocumentsFolder, doc.fileName)
                    val outputStream = file.outputStream()
                    val copied = resourceInputStream.copyTo(outputStream)
                    resourceInputStream.close()
                    outputStream.close()
                    val documentDb = DocumentPojo(null, transactionId, DocumentType.Other, file.length(), HexFormat.of().parseHex(doc.sha256), "image/jpeg", "", "", (i * 1_000).toShort(), (i+1).toShort(),dateTime)
                    val documentId = documentRepository.insertDocument(documentDb)
//                sha256Map[doc.sha256] = imageId
//                imageId
                }

                items.forEachIndexed { index, item ->
                    val itemOrdinal = (index+1).toShort()
                    val correctedItem = item.copy(accountOrdinal = account.ordinal, transactionOrdinal = nextOrdinal, createdAt = dateTime, sortKey = ((index+1) * 1_000).toShort(), occupancySlot = (index + 1).toShort(), ordinal = itemOrdinal)
                    val itemId = transactionItemRepository.insertTransactionItem(correctedItem)
                    val list = imagesMap[index]
                    if(list != null) {
                        list.forEachIndexed { i, resource ->
                            val existingDbImage = imagesRepository.findBySha256(profile.id!!, HexFormat.of().parseHex(resource.sha256))
                            val imageOrdinal = if(sha256Map.contains(resource.sha256)) {
                                sha256Map[resource.sha256]!!
                            } else if(existingDbImage != null) {
                                existingDbImage.ordinal!!
                            } else {
                                val resourceInputStream = classLoader.getResourceAsStream(resource.resourceName)
                                val file = File(mainImagesFolder, resource.fileName)
                                val outputStream = file.outputStream()
                                val copied = resourceInputStream.copyTo(outputStream)
                                resourceInputStream.close()
                                outputStream.close()
                                val newOrdinal = ((imagesRepository.getMaxOrdinal(profile.id!!)?:0) + 1).toShort()
                                val imageDb = ImagePojo(null, profile.id!!, file.length(), HexFormat.of().parseHex(resource.sha256), "image/jpeg", "", "", newOrdinal, dateTime)
                                val imageId = imagesRepository.insertImage(imageDb)
                                sha256Map[resource.sha256] = newOrdinal
                                newOrdinal
                            }
                            val sortKey = ((i+1) * 1_000).toShort()
                            val occupancySlot = (i+1).toShort()
                            val itemImage = TransactionItemImagesPojo(null, profile.id, account.ordinal, nextOrdinal, itemOrdinal, imageOrdinal, sortKey, occupancySlot,dateTime)
                            itemImagesRepository.insertItemImage(itemImage)
                        }
                    }
                }

                transactionId
            }
            hasCommitted = true
            return ids
        }

        override fun withDetailedItem(): TransactionItemBuilder {
            val itemBuilder = TransactionItemBuilderImpl(this, categoryRepository, this.profile)
            return itemBuilder
        }

        override fun repeatIntoDateRange(startDate: LocalDate, endDate: LocalDate): TransactionBuilder {
            if(startDate > endDate) {
                throw IllegalArgumentException("Invalid date range")
            }
            val dates = arrayListOf<LocalDate>()
            var runningDate = startDate
            while(runningDate <= endDate) {
                dates.add(runningDate)
                runningDate = runningDate.plusDays(1)
            }
            this.repeatDates.addAll(dates)
            return this
        }

        fun addItem(transactionItemDb: TransactionItemPojo) {
            this.items.add(transactionItemDb)
        }

        fun addItem(transactionItemDb: TransactionItemPojo, images: List<TestData.Resource>) {
            this.items.add(transactionItemDb)
            val index = this.items.size - 1
            imagesMap[index] = images
        }

        override fun withDocument(resource: TestData.Resource): TransactionBuilder {
            this.documents.add(resource)
            return this
        }

        override fun withNewSeller(name: String): TransactionBuilder {
            if(customSeller != null) {
                throw IllegalArgumentException("Custom seller already set")
            }

            val ordinalSortKeyOccupancy = sellerRepository.getMaxOrdinalSortKeyAndOccupancySlot(profile.id!!)
            val nextOrdinal = ((ordinalSortKeyOccupancy.ordinal?: 0) + 1).toShort()
            val nextSortKey = ((ordinalSortKeyOccupancy.sortKey?: 0) + 1_000).toShort()
            val nextOccupancy = ((ordinalSortKeyOccupancy.occupancySlot?: 0) + 1).toShort()
            val seller = SellerPojo(null, profile.id!!, name, nextOrdinal, nextSortKey, nextOccupancy, LocalDateTime.now(timeHandler.getClock()))
            val sellerId = sellerRepository.insertSeller(seller)
            this.customSeller = sellerRepository.getByProfileIdAndId(profile.id!!, sellerId)
            return this
        }

        override fun withSeller(sellerId: Long): TransactionBuilder {
            this.customSeller = sellerRepository.getByProfileIdAndId(profile.id!!, sellerId)
            return this
        }

        override fun withNewSellerLocation(name: String): TransactionBuilder {
            if(customSeller == null) {
                throw IllegalArgumentException("Custom seller not created yet")
            }
            this.customSellerLocation = SellerLocationPojo(null, profile.id!!, customSeller!!.ordinal!! ,name)
            return this
        }

        override fun getSeller(): SellerPojo? {
            return customSeller
        }

        override fun withMode(mode: TransactionMode): TransactionBuilder {
            if(customMode != null) {
                throw IllegalArgumentException("Mode already set")
            }
            this.customMode = mode
            return this
        }
    }

    private class TransactionItemBuilderImpl(private val xTransactionBuilderImpl: TransactionBuilderImpl, private val categoryRepository: CategoryRepository, private val profileDb: ProfilePojo): TransactionItemBuilder {

        private var item = TransactionItemPojo(null, profileDb.id, -1, -1, "", BigDecimal.ZERO, -1, 0, 0, 0, null, "", null,
            null, null, null, null,
            false.toByte(), LocalDateTime.now())
        private var images = listOf<TestData.Resource>()
        private var mainDetailsSet = false

        override fun commit(): List<Long> {
            build()
            return xTransactionBuilderImpl.commit()
        }

        override fun mainDetails(description: String, category: String, amount: BigDecimal): TransactionItemBuilder {
            if(mainDetailsSet) {
                throw IllegalStateException("Main details have already been set")
            }
            val categoryDb = categoryRepository.findByProfileIdAndStringId(profileDb.id!!, category)
            if(categoryDb == null) {
                throw IllegalStateException("Category does not exist")
            }
            item = TransactionItemPojo(null, profileDb.id, -1, -1, description, amount, categoryDb.ordinal!!, 0, 1, 1, 1, null, "", null,
            null, null, null, false.toByte(), LocalDateTime.now())
            mainDetailsSet = true
            return this
        }

        override fun otherDetails(brand: String?, quantity: Int, variation: String?, referenceNumber: String?, isReduction: Boolean, weight: BigDecimal?, weightUnit: String?, volume: BigDecimal?, volumeUnit: String?): TransactionItemBuilder {
            item = item.copy(brand = brand, quantity = quantity, variation = variation, isReduction = isReduction.toByte(), weight = weight, weightUnit = weightUnit, volume = volume, volumeUnit = volumeUnit)
            return this
        }

        override fun withImages(vararg resource: TestData.Resource): TransactionItemBuilder {
            this.images = resource.toList()
            return this
        }

        override fun build(): TransactionBuilder {
            if(!mainDetailsSet ||
                item.amount!!.compareTo(BigDecimal.ZERO) == 0 || item.description!!.isBlank() || item.primaryCategoryOrdinal == (-1).toShort()) {
                throw IllegalStateException("Item not properly initialized")
            }
            if(images.isEmpty()) {
                xTransactionBuilderImpl.addItem(item)
            } else {
                xTransactionBuilderImpl.addItem(item, images)
            }
            return xTransactionBuilderImpl
        }
    }
}