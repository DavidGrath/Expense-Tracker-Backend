package com.davidgrath.expensetrackerbackend.testutils

import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ImagePojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.SellerLocationPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.SellerPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionItemImagesPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionItemPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionPojo
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.CategoryRepository
import com.davidgrath.expensetrackerbackend.repository.DocumentRepository
import com.davidgrath.expensetrackerbackend.repository.ImageRepository
import com.davidgrath.expensetrackerbackend.repository.ItemImagesRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.repository.SellerLocationRepository
import com.davidgrath.expensetrackerbackend.repository.SellerRepository
import com.davidgrath.expensetrackerbackend.repository.TestRepository
import com.davidgrath.expensetrackerbackend.repository.TransactionItemRepository
import com.davidgrath.expensetrackerbackend.repository.TransactionRepository
import com.davidgrath.expensetrackerbackend.repository.UserDraftRepository
import com.davidgrath.expensetrackerbackend.repository.UserRepository
import com.davidgrath.expensetrackerbackend.service.TransactionDraftServiceTest.Companion.MOCKED_DATE
import com.davidgrath.expensetrackerbackend.service.TransactionDraftServiceTest.Companion.MOCKED_TIME
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.FALSE_BYTE
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.TRUE_BYTE
import com.davidgrath.expensetrackerbackend.utils.bytesToUUID
import com.davidgrath.expensetrackerbackend.utils.file
import com.davidgrath.expensetrackerbackend.utils.getSha256AsBytes
import com.davidgrath.expensetrackerbackend.utils.toByte
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestComponent
import java.io.File
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@TestComponent
class TestUtilComponent {

    @Autowired
    lateinit var testRepository: TestRepository

    @Autowired
    lateinit var profileRepository: ProfileRepository
    @Autowired
    lateinit var transactionRepository: TransactionRepository

    @Autowired
    lateinit var categoryRepository: CategoryRepository
    @Autowired
    lateinit var accountRepository: AccountRepository
    @Autowired
    lateinit var itemRepository: TransactionItemRepository
    @Autowired
    lateinit var imageRepository: ImageRepository
    @Autowired
    lateinit var itemImagesRepository: ItemImagesRepository
    @Autowired
    lateinit var userRepository: UserRepository
    @Autowired
    lateinit var documentRepository: DocumentRepository
    @Autowired
    lateinit var userDraftRepository: UserDraftRepository
    @Autowired
    lateinit var sellerRepository: SellerRepository
    @Autowired
    lateinit var sellerLocationRepository: SellerLocationRepository
    @Autowired
    lateinit var transactionItemRepository: TransactionItemRepository
    @Autowired
    lateinit var timeHandler: TimeHandler


    fun deleteAll() {
        testRepository.deleteAllUserDrafts()
        testRepository.deleteAllItemImages()
        testRepository.deleteAllDocuments()
        testRepository.deleteAllItems()
        testRepository.deleteAllImages()
        testRepository.deleteAllCategories()
        testRepository.deleteAllTransactions()
        testRepository.deleteAllAccounts()
        testRepository.deleteAllSellerLocations()
        testRepository.deleteAllSellers()
        testRepository.deleteAllProfiles()
        testRepository.deleteAllUsers()
    }

    fun createTransactionWithImage(profileId: Long, userUuid: ByteArray, profileUuid: ByteArray, accountId: Long, classLoader: ClassLoader, resource: TestData.Resource): Long {

        val amount = BigDecimal.TEN
        val account = accountRepository.findByProfileIdAndId(profileId, accountId)!!

        val ordinalSortKeyOccupancy = transactionRepository.getMaxOrdinalSortKeyAndOccupancySlot(profileId, accountId)
        val nextOrdinal = ((ordinalSortKeyOccupancy.ordinal?: 0) + 1).toShort()
        val nextSortKey = ((ordinalSortKeyOccupancy.sortKey?: 0) + 1_000).toShort()
        val nextOccupancy = ((ordinalSortKeyOccupancy.occupancySlot?: 0) + 1).toShort()

        val transaction = TransactionPojo(null, profileId, amount, TRUE_BYTE, MOCKED_DATE, MOCKED_TIME,
            TransactionMode.Other, null, account.ordinal, null, null, nextOrdinal, nextSortKey, nextOccupancy, MOCKED_DATE.atTime(MOCKED_TIME))

        val transactionId = transactionRepository.insertTransaction(transaction)
        val misc = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!

        val userUuidString = bytesToUUID(userUuid).toString()
        val profileUuidString = bytesToUUID(profileUuid).toString()

        val itemOrdinal: Short = 1
        val item = TransactionItemPojo(null, profileId, account.ordinal, nextOrdinal, "Basic", amount, misc.ordinal!!, itemOrdinal, 1_000, 1, 1, null,
            null, null, null, null, null, null, null, FALSE_BYTE,FALSE_BYTE,
            MOCKED_DATE.atTime(MOCKED_TIME))
        val itemId = transactionItemRepository.insertTransactionItem(item)
        //TODO make this more convenient
        val newFileName = "${UUID.randomUUID()}.jpg"
        val newPath = "${Constants.FOLDER_NAME_PROFILES}/$profileUuidString/${Constants.FOLDER_NAME_DATA}/${Constants.SUBFOLDER_NAME_IMAGES}/${newFileName}"
        val destinationFolder = file(
            timeHandler.getRoot(),
            Constants.FOLDER_NAME_USERS,
            userUuidString,
            Constants.FOLDER_NAME_PROFILES,
            profileUuidString,
            Constants.FOLDER_NAME_DATA,
            Constants.SUBFOLDER_NAME_IMAGES
        )
        destinationFolder.mkdirs()
        val destination = File(destinationFolder, newFileName)

        val inputStream = classLoader.getResourceAsStream(resource.resourceName)
        val outputStream = destination.outputStream()
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        val hashStream = destination.inputStream()

        val hash = getSha256AsBytes(hashStream)
        hashStream.close()

        val imageOrdinal: Short = 1
        val image = ImagePojo(null, profileId, destination.length(), hash, "image/jpeg", "", newPath, imageOrdinal, MOCKED_DATE.atTime(MOCKED_TIME))
        val imageId = imageRepository.insertImage(image)
        val itemImage = TransactionItemImagesPojo(null, profileId, account.ordinal, nextOrdinal, itemOrdinal, imageOrdinal, 1_000, 1, MOCKED_DATE.atTime(MOCKED_TIME))
        itemImagesRepository.insertItemImage(itemImage)
        return imageId
    }


    /**
     * @return the seller ID, seller ordinal, and location ID
     */
    fun seedSellers(profileId: Long): Triple<Long, Short, Long> {
        val createdAt = LocalDateTime.now(timeHandler.getClock())
        val ordinalSortKeyOccupancy = sellerRepository.getMaxOrdinalSortKeyAndOccupancySlot(profileId)
        val nextOrdinal = ((ordinalSortKeyOccupancy.ordinal?: 0) + 1).toShort()
        val nextSortKey = ((ordinalSortKeyOccupancy.sortKey?: 0) + 1_000).toShort()
        val nextOccupancy = ((ordinalSortKeyOccupancy.occupancySlot?: 0) + 1).toShort()
        val seller = SellerPojo(null, profileId, "Restaurant", nextOrdinal, nextSortKey, nextOccupancy, createdAt)
        val sellerId = sellerRepository.insertSeller(seller)
        val locationOrdinal: Short = 1
        val sellerLocation = SellerLocationPojo(null, profileId, nextOrdinal, "Branch location", null, null, null, locationOrdinal, 500, 1, createdAt) //TODO rework to interval when done
        val sellerLocationId = sellerLocationRepository.insertSellerLocation(sellerLocation)
        return Triple(sellerId, nextOrdinal, sellerLocationId)
    }
}