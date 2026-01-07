package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TestConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.entities.DocumentType
import com.davidgrath.expensetrackerbackend.entities.TransactionDraftFile
import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.DocumentPojo
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
import com.davidgrath.expensetrackerbackend.repository.TransactionItemRepository
import com.davidgrath.expensetrackerbackend.repository.TransactionRepository
import com.davidgrath.expensetrackerbackend.repository.UserDraftRepository
import com.davidgrath.expensetrackerbackend.repository.UserRepository
import com.davidgrath.expensetrackerbackend.testutils.TestConstants
import com.davidgrath.expensetrackerbackend.testutils.TestData
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.assertEqualsBD
import com.davidgrath.expensetrackerbackend.testutils.copyControllerTempFile
import com.davidgrath.expensetrackerbackend.testutils.getDefaultAccountId
import com.davidgrath.expensetrackerbackend.testutils.getHashCount
import com.davidgrath.expensetrackerbackend.testutils.tempInit
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.LocalDateGsonAdapter
import com.davidgrath.expensetrackerbackend.utils.LocalTimeGsonAdapter
import com.davidgrath.expensetrackerbackend.utils.file
import com.davidgrath.expensetrackerbackend.utils.getSha256
import com.davidgrath.expensetrackerbackend.utils.toBoolean
import com.davidgrath.expensetrackerbackend.utils.toByte
import com.google.gson.GsonBuilder
import org.jooq.JSON
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

@SpringBootTest
@RunWith(SpringRunner::class)
@ContextConfiguration(
    classes = [
        TestConfiguration::class,
        TransactionService::class,
        TransactionRepository::class
    ])
class TransactionDraftServiceTest {

    @Autowired
    lateinit var transactionDraftService: TransactionDraftService
    @Autowired
    lateinit var transactionRepository: TransactionRepository
    @Autowired
    lateinit var transactionItemRepository: TransactionItemRepository
    @Autowired
    lateinit var timeHandler: TimeHandler
    @Autowired
    lateinit var profileRepository: ProfileRepository
    @Autowired
    lateinit var categoryRepository: CategoryRepository
    @Autowired
    lateinit var accountRepository: AccountRepository
    @Autowired
    lateinit var userRepository: UserRepository
    @Autowired
    lateinit var sellerRepository: SellerRepository
    @Autowired
    lateinit var sellerLocationRepository: SellerLocationRepository
    @Autowired
    lateinit var imageRepository: ImageRepository
    @Autowired
    lateinit var itemImagesRepository: ItemImagesRepository
    @Autowired
    lateinit var documentRepository: DocumentRepository

    @Autowired
    lateinit var testUtilComponent: TestUtilComponent

    @Autowired
    lateinit var userDraftRepository: UserDraftRepository

    @Before
    fun setUp() {
        tempInit(testUtilComponent, timeHandler)
    }

    @After
    fun tearDown() {
        File(timeHandler.getRoot(), Constants.FOLDER_NAME_USERS).deleteRecursively()
        testUtilComponent.deleteAll()
    }

    @Test
    fun givenItemIsReductionWhenSaveThenRelevantFieldsPreservedAndOthersDiscarded() {

        //Add item with isReduction
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!

        transactionDraftService.createDraft(profileId, accountId)
        transactionDraftService.addItemToDraft(profileId)

        val draft = transactionDraftService.getDraft(profileId)
        val firstItem = draft!!.items[0]
        val modifiedFirst = firstItem.copy(amount = BigDecimal(11), description = "Desc")
        transactionDraftService.modifyDraftItem(profileId, firstItem.uuid!!, modifiedFirst)

        //Add item with isReduction
        val secondItem = draft!!.items[1]
        val modified = secondItem.copy(isReduction = true, amount = BigDecimal.TEN,description = "Discount", quantity = 13, brand = "Generic", variation = "XXL", referenceNumber = "REF-1234")
        transactionDraftService.modifyDraftItem(profileId, secondItem.uuid!!, modified)
        //Fill in all fields
        transactionDraftService.finishDraft(profileId, user.uuid!!, profile.stringId!!,ZoneId.of("UTC"))
        val account = accountRepository.getAllByProfileId(profileId).first()
        val transactions = transactionRepository.getTransactions(profileId, null, null, listOf(account.id!!), emptyList(), emptyList(), emptyList(), emptyList())
        val transaction = transactions.first()
        val item = transactionItemRepository.getAllByTransactionId(profileId, transaction.id!!)[1]

        assertEqualsBD(BigDecimal.ONE, transaction.amount!!)
        //Assert amount, description, and categoryId remain
        assertTrue(item.isReduction.toBoolean())
        assertEqualsBD(BigDecimal.TEN, item.amount!!)
        assertEquals("Discount", item.description)
        assertEquals(misc.id!!, item.primaryCategoryId)
        //Assert all other fields discarded
        assertNull(item.variation)
        assertNull(item.brand)
        assertNull(item.referenceNumber)
        assertEquals(1, item.quantity)

    }

    @Test
    fun givenTransactionHasReductionsAndReductionsLessThanTotalThenTransactionIsValid() {
        //Add normal item


        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!

        transactionDraftService.createDraft(profileId, accountId)

        transactionDraftService.addItemToDraft(profileId)
        val draft = transactionDraftService.getDraft(profileId)
        val firstItem = draft!!.items[0]
        val modifiedFirst = firstItem.copy(amount = BigDecimal(11), description = "Desc")
        transactionDraftService.modifyDraftItem(profileId, firstItem.uuid!!, modifiedFirst)


        //Add item with isReduction
        val secondItem = draft!!.items[1]
        val modified = secondItem.copy(amount = BigDecimal.TEN, description = "Discount", isReduction = true)
        transactionDraftService.modifyDraftItem(profileId, secondItem.uuid!!, modified)

        val isValid = transactionDraftService.validateDraft(profileId)
        assertTrue(isValid)
    }

    @Test
    fun givenTransactionHasReductionsAndReductionsNotLessThanTotalThenTransactionIsNotValid() {
        //Add normal item

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!

        transactionDraftService.createDraft(profileId, accountId)
        transactionDraftService.addItemToDraft(profileId)
        val draft = transactionDraftService.getDraft(profileId)
        val firstItem = draft!!.items[0]
        val modifiedFirst = firstItem.copy(amount = BigDecimal.TEN, description = "Desc")
        transactionDraftService.modifyDraftItem(profileId, firstItem.uuid!!, modifiedFirst)

        val secondItem = draft!!.items[1]
        val modifiedSecond = secondItem.copy(amount = BigDecimal.TEN, description = "Discount", isReduction = true)
        transactionDraftService.modifyDraftItem(profileId, secondItem.uuid!!, modifiedSecond)

        //Add item with isReduction
        val isValid = transactionDraftService.validateDraft(profileId)
        assertFalse(isValid)
    }

    @Test
    fun givenTransactionHasReductionsAndTransactionHasNoNormalItemsThenFail() {
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!

        transactionDraftService.createDraft(profileId, accountId)

        //Change item to isReduction
        val draft = transactionDraftService.getDraft(profileId)
        val firstItem = draft!!.items[0]
        val modifiedFirst = firstItem.copy(amount = BigDecimal.TEN, description = "Discount", isReduction = true)
        transactionDraftService.modifyDraftItem(profileId, firstItem.uuid!!, modifiedFirst)
        //No other items
        //Fail - cannot have only reductions
        val isValid = transactionDraftService.validateDraft(profileId)
        assertFalse(isValid)
        //Oh, also, in general, negatives aren't allowed. Add multiple levels of assertion
    }

    @Test
    fun givenDateIsTodayBasedOnUserTimezoneAndTimeNotSetWhenSaveThenTimeIsNow() {
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!

        transactionDraftService.createDraft(profileId, accountId)

        val localDate = MOCKED_DATE
        transactionDraftService.setDate(profileId, localDate)


        val draft = transactionDraftService.getDraft(profileId)
        val firstItem = draft!!.items[0]
        val modifiedFirst = firstItem.copy(amount = BigDecimal.TEN)
        transactionDraftService.modifyDraftItem(profileId, firstItem.uuid!!, modifiedFirst)

        val transactionId = transactionDraftService.finishDraft(profileId, user.uuid!!, profile.stringId!!,ZoneId.of("UTC"))
        val transaction = transactionRepository.getByProfileIdAndId(profileId, transactionId)!!
        val transactionTime = transaction.time!!

        assertEquals(0, MOCKED_TIME.compareTo(transactionTime))
    }

    @Test
    fun givenDateIsTodayBasedOnUserTimezoneAndTimeIsSetWhenSaveThenTimeIsCustom() {
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!

        transactionDraftService.createDraft(profileId, accountId)

        val localDate = MOCKED_DATE
        val localTime = LocalTime.of(1, 0, 30)
        transactionDraftService.setDate(profileId, localDate)
        transactionDraftService.setTime(profileId, localTime)

        val transactionId = transactionDraftService.finishDraft(profileId, user.uuid!!, profile.stringId!!,ZoneId.of("UTC"))
        val transaction = transactionRepository.getByProfileIdAndId(profileId, transactionId)
        val transactionTime = transaction!!.time

        assertEquals(localTime, transactionTime)
    }

    @Test
    fun givenDateIsTodayBasedOnUserTimezoneAndDateIsNotTodayBasedOnServerTimezoneAndCustomDateIsNullAndTimeNotSetWhenSaveThenTimeIsNotNull() {

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!

        transactionDraftService.createDraft(profileId, accountId)

        val localDate = MOCKED_DATE
        val localTime = LocalTime.of(1, 0, 30)

        val draft = transactionDraftService.getDraft(profileId)
        val firstItem = draft!!.items[0]
        val modifiedFirst = firstItem.copy(amount = BigDecimal.TEN, description = "Desc")
        transactionDraftService.modifyDraftItem(profileId, firstItem.uuid!!, modifiedFirst)

        val transactionId = transactionDraftService.finishDraft(profileId, user.uuid!!, profile.stringId!!,ZoneId.of("Etc/GMT+10"))  // 10 PM 2025-06-29
        val transaction = transactionRepository.getByProfileIdAndId(profileId, transactionId)
        val transactionTime = transaction!!.time




        assertNotNull(transaction.time)

    }

    @Test
    fun givenCustomDateIsBeforeTodayBasedOnUserTimezoneAndTimeSetWhenSaveThenTimeIsCustom() {
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!

        transactionDraftService.createDraft(profileId, accountId)

        val localDate = LocalDate.of(2025, 1, 1)
        val localTime = LocalTime.of(1, 0, 30)

        transactionDraftService.setDate(profileId, localDate)
        transactionDraftService.setTime(profileId, localTime)

        val draft = transactionDraftService.getDraft(profileId)
        val firstItem = draft!!.items[0]
        val modifiedFirst = firstItem.copy(amount = BigDecimal.TEN, description = "Desc")
        transactionDraftService.modifyDraftItem(profileId, firstItem.uuid!!, modifiedFirst)

        val transactionId = transactionDraftService.finishDraft(profileId, user.uuid!!, profile.stringId!!,ZoneId.of("UTC"))
        val transaction = transactionRepository.getByProfileIdAndId(profileId, transactionId)

        assertEquals(localTime, transaction!!.time)
    }

    @Test
    fun givenSellerAndLocationSetIfLocationSomehowNotLinkedToSellerWhenSaveThenLocationNull() {

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!

        val date = ZonedDateTime.now(timeHandler.getClock())
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()

        val sellerId = sellerRepository.insertSeller(SellerPojo(null, profile.id!!, "Restaurant", utcDate))
        val sellerId2 = sellerRepository.insertSeller(SellerPojo(null, profile.id!!, "Coffee Shop", utcDate))
        val sellerLocationId = sellerLocationRepository.insertSellerLocation(SellerLocationPojo(null, sellerId, "Downtown", false.toByte(), null, null, null, utcDate))

        transactionDraftService.createDraft(profileId, accountId)


        transactionDraftService.setSeller(profileId, sellerId2)
        transactionDraftService.setSellerLocation(profileId, sellerLocationId)

        val transactionId = transactionDraftService.finishDraft(profileId, user.uuid!!, profile.stringId!!,ZoneId.of("UTC"))
        val transaction = transactionRepository.getByProfileIdAndId(profileId, transactionId)!!

        assertNotNull(transaction.sellerId)
        assertNull(transaction.sellerLocationId)
    }

    @Test
    fun givenImagesInDraftWhenDoneThenImagesNotInDraftAndImagesInMainFolder() {
        val resource = TestData.Resource.Images.BREAD
        val classLoader = TransactionDraftServiceTest::class.java.classLoader!!
        val tempFolder = Files.createTempDirectory("temp").toFile()
        tempFolder.deleteOnExit()
        copyControllerTempFile(tempFolder, classLoader, resource)
        val tempFile = File(tempFolder, resource.fileName)


        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)


        transactionDraftService.createDraft(profileId, accountId)
        val draft = transactionDraftService.getDraft(profileId)!!
        val item = draft.items[0]

        transactionDraftService.addImageToItem(profileId, item.uuid, tempFile, "image/jpeg", user.uuid!!, profile.stringId!!)
        transactionDraftService.addImageToItem(profileId, item.uuid, tempFile, "image/jpeg", user.uuid!!, profile.stringId!!)

        transactionDraftService.finishDraft(profileId, user.uuid!!, profile.stringId!!,ZoneId.of("UTC"))

        val draftImagesFolder = file(timeHandler.getRoot(),
        Constants.FOLDER_NAME_USERS, user.uuid!!,
        Constants.FOLDER_NAME_PROFILES, profile.stringId!!,
        Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_IMAGES)
        val mainImagesFolder = file(timeHandler.getRoot(),
        Constants.FOLDER_NAME_USERS, user.uuid!!,
        Constants.FOLDER_NAME_PROFILES, profile.stringId!!,
        Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_IMAGES)
        assertEquals(0, getHashCount(resource.sha256, draftImagesFolder))
        assertEquals(1, getHashCount(resource.sha256, mainImagesFolder))
    }

    @Test
    fun givenImageInDraftNotUsedWhenDoneThenImageNotInMainFolder() {
        val resource = TestData.Resource.Images.BREAD
        val classLoader = TransactionDraftServiceTest::class.java.classLoader

        val tempFolder = Files.createTempDirectory("temp").toFile()
        tempFolder.deleteOnExit()

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)

        copyControllerTempFile(tempFolder, classLoader, resource)
        val tempFile = File(tempFolder, resource.fileName)

        val draftImagesFolder = file(timeHandler.getRoot(),
            Constants.FOLDER_NAME_USERS,user.uuid!!,
            Constants.FOLDER_NAME_PROFILES, profile.stringId!!,
            Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_IMAGES)
        draftImagesFolder.mkdirs()
        val mainImagesFolder = file(timeHandler.getRoot(),
            Constants.FOLDER_NAME_USERS,user.uuid!!,
            Constants.FOLDER_NAME_PROFILES, profile.stringId!!,
            Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_IMAGES)
        mainImagesFolder.mkdirs()
        val draftImage = File(draftImagesFolder, "45402cd3-2452-4804-981a-7ea5515dec74.jpg")
        tempFile.copyTo(draftImage)

        val path = "/${Constants.FOLDER_NAME_USERS}/${user.uuid}/${Constants.FOLDER_NAME_PROFILES}/${profile.stringId}/${Constants.FOLDER_NAME_DRAFT}/${Constants.SUBFOLDER_NAME_IMAGES}/${tempFile.name}"

        // Populate hashmap with unused image
        val map = mapOf(resource.sha256 to path)

        transactionDraftService.createDraft(profileId, accountId)
        val userDraft = userDraftRepository.getDraftByProfile(profileId)!!

        val draft = transactionDraftService.getDraft(profileId)!!.copy(imageHashes = map)
        val gson = GsonBuilder()
            .registerTypeAdapter(LocalDate::class.java, LocalDateGsonAdapter())
            .registerTypeAdapter(LocalTime::class.java, LocalTimeGsonAdapter())
            .create()
        val jsonString = gson.toJson(draft)
        val json = JSON.json(jsonString)
        userDraftRepository.updateDraft(profileId, userDraft.id!!, json)
        transactionDraftService.finishDraft(profileId, user.uuid!!, profile.stringId!!, ZoneId.of("UTC"))

        assertEquals(0, getHashCount(resource.sha256, draftImagesFolder))
        assertEquals(0, getHashCount(resource.sha256, mainImagesFolder))
    }

    @Test
    fun givenUserSelectsSameImageMultipleTimesForSameItemThenImageOnlyAddedOnce() {
        val resource = TestData.Resource.Images.BREAD
        val classLoader = TransactionDraftServiceTest::class.java.classLoader!!
        val tempFolder = Files.createTempDirectory("temp").toFile()
        tempFolder.deleteOnExit()
        copyControllerTempFile(tempFolder, classLoader, resource)
        val tempFile = File(tempFolder, resource.fileName)


        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)


        transactionDraftService.createDraft(profileId, accountId)
        val draft = transactionDraftService.getDraft(profileId)!!
        val item = draft.items[0]

        transactionDraftService.addImageToItem(profileId, item.uuid, tempFile, "image/jpeg", user.uuid!!, profile.stringId!!)
        transactionDraftService.addImageToItem(profileId, item.uuid, tempFile, "image/jpeg", user.uuid!!, profile.stringId!!)

        val newDraft = transactionDraftService.getDraft(profileId)!!
        val itemImages = newDraft.items[0].images
        assertEquals(1, itemImages.size)
    }

    @Test
    fun givenUserSelectsSameImageMultipleTimesAcrossMultipleItemsThenImageOnlyCopiedOnceToDraftStorage() {
        val resource = TestData.Resource.Images.BREAD

        val classLoader = TransactionDraftServiceTest::class.java.classLoader!!
        val tempFolder = Files.createTempDirectory("temp").toFile()
        tempFolder.deleteOnExit()
        copyControllerTempFile(tempFolder, classLoader, resource)
        val tempFile = File(tempFolder, resource.fileName)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)

        transactionDraftService.createDraft(profileId, accountId)

        val draft = transactionDraftService.getDraft(profileId)!!
        val uuid1 = draft.items[0].uuid
        val uuid2 = transactionDraftService.addItemToDraft(profileId)!!

        transactionDraftService.addImageToItem(profileId, uuid1, tempFile, "image/jpeg", user.uuid!!, profile.stringId!!)
        transactionDraftService.addImageToItem(profileId, uuid2, tempFile, "image/jpeg", user.uuid!!, profile.stringId!!)

        val draftImagesFolder = file(timeHandler.getRoot(),
            Constants.FOLDER_NAME_USERS, user.uuid!!,
            Constants.FOLDER_NAME_PROFILES, profile.stringId!!,
            Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_IMAGES)
        val updatedDraft = transactionDraftService.getDraft(profileId)!!
        val items = updatedDraft.items
        assertEquals(1, items[0].images.size)
        assertEquals(1, items[1].images.size)
        assertEquals(1, getHashCount(resource.sha256, draftImagesFolder))
    }

    @Test
    fun givenImageCountIsMaxWhenAddImageThenImageCountNotIncreased() {

        //TODO Use image modification instead
        val resources = arrayOf(
            TestData.Resource.Images.DUMBBELLS_1,
            TestData.Resource.Images.DUMBBELLS_2,
            TestData.Resource.Images.DUMBBELLS_3
        )

        val classLoader = TransactionDraftServiceTest::class.java.classLoader!!
        val tempFolder = Files.createTempDirectory("temp").toFile()
        tempFolder.deleteOnExit()
        val tempFiles = resources.map { resource ->

            copyControllerTempFile(tempFolder, classLoader, resource)
            File(tempFolder, resource.fileName)
        }

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)

        transactionDraftService.createDraft(profileId, accountId)

        val draft = transactionDraftService.getDraft(profileId)!!
        val uuid = draft.items[0].uuid

        tempFiles.forEach { file ->
            transactionDraftService.addImageToItem(profileId, uuid, file, "image/jpeg", user.uuid!!, profile.stringId!!)
        }
        val extraResource = TestData.Resource.Images.BREAD
        copyControllerTempFile(tempFolder, classLoader, extraResource)
        val extraTempFile = File(tempFolder, extraResource.fileName)
        transactionDraftService.addImageToItem(profileId, uuid, extraTempFile, "image/jpeg", user.uuid!!, profile.stringId!!)

        val draftImagesFolder = file(timeHandler.getRoot(),
            Constants.FOLDER_NAME_USERS, user.uuid!!,
            Constants.FOLDER_NAME_PROFILES, profile.stringId!!,
            Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_IMAGES)
        val updatedDraft = transactionDraftService.getDraft(profileId)!!
        val items = updatedDraft.items
        assertEquals(Constants.Constraints.MAX_IMAGES_PER_ITEM, items[0].images.size)
        assertEquals(Constants.Constraints.MAX_IMAGES_PER_ITEM, draftImagesFolder.listFiles().size)
        assertEquals(0, getHashCount(extraResource.sha256, draftImagesFolder))
    }
    @Test
    fun givenUserSelectsImageThatHasBeenSavedToMainStorageWhenSelectImageThenImageNotCopiedToDraftStorage() {

        val resource = TestData.Resource.Images.BREAD

        val classLoader = TransactionDraftServiceTest::class.java.classLoader!!
        val tempFolder = Files.createTempDirectory("temp").toFile()
        tempFolder.deleteOnExit()

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)

        copyControllerTempFile(tempFolder, classLoader, resource)
        val tempFile = File(tempFolder, resource.fileName)

        val inputStream = tempFile.inputStream()
        val hash = getSha256(inputStream)
        inputStream.close()
        val path = "/${Constants.FOLDER_NAME_USERS}/${user.uuid}/${Constants.FOLDER_NAME_PROFILES}/${profile.stringId}/${Constants.FOLDER_NAME_DRAFT}/${Constants.SUBFOLDER_NAME_IMAGES}/${tempFile.name}"
        val draftFile = TransactionDraftFile(null, path, "image/jpeg", hash, tempFile.length(), null,false)
        //Deletes the temp file
        createTransactionWithImage(profileId, user.uuid!!, profile.stringId!!, accountId, tempFile,draftFile)
        //Recreate the temp file that was just deleted
        copyControllerTempFile(tempFolder, classLoader, resource)

        val draftImagesFolder = file(timeHandler.getRoot(),
            Constants.FOLDER_NAME_USERS, user.uuid!!,
            Constants.FOLDER_NAME_PROFILES, profile.stringId!!,
            Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_IMAGES)

        transactionDraftService.createDraft(profileId, accountId)
        val draft = transactionDraftService.getDraft(profileId)!!
        val uuid = draft.items[0].uuid

        transactionDraftService.addImageToItem(profileId, uuid, tempFile,"image/jpeg", user.uuid!!, profile.stringId!!)

        val updatedDraft = transactionDraftService.getDraft(profileId)!!
        val items = updatedDraft.items
        assertEquals(0, getHashCount(resource.sha256, draftImagesFolder))
        assertEquals(1, items[0].images.size)
        assertNotNull(items[0].images[0].dbId)
    }

    @Test
    fun givenDocumentInDraftNotUsedWhenDoneThenDocumentNotInMainFolder() {
        val resource = TestData.Resource.Documents.EVIDENCE_IMAGE
        val classLoader = TransactionDraftServiceTest::class.java.classLoader

        val tempFolder = Files.createTempDirectory("temp").toFile()
        tempFolder.deleteOnExit()

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)

        copyControllerTempFile(tempFolder, classLoader, resource)
        val tempFile = File(tempFolder, resource.fileName)

        val draftDocumentsFolder = file(timeHandler.getRoot(),
            Constants.FOLDER_NAME_USERS,user.uuid!!,
            Constants.FOLDER_NAME_PROFILES, profile.stringId!!,
            Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_DOCUMENTS)
        draftDocumentsFolder.mkdirs()
        val mainDocumentsFolder = file(timeHandler.getRoot(),
            Constants.FOLDER_NAME_USERS,user.uuid!!,
            Constants.FOLDER_NAME_PROFILES, profile.stringId!!,
            Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_DOCUMENTS,
            "2025","06","30")
        mainDocumentsFolder.mkdirs()
        val draftDocument = File(draftDocumentsFolder, "45402cd3-2452-4804-981a-7ea5515dec74.jpg")
        tempFile.copyTo(draftDocument)

        val path = "/${Constants.FOLDER_NAME_USERS}/${user.uuid}/${Constants.FOLDER_NAME_PROFILES}/${profile.stringId}/${Constants.FOLDER_NAME_DRAFT}/${Constants.SUBFOLDER_NAME_DOCUMENTS}/2025/06/30/${tempFile.name}"

        // Populate hashmap with unused document
        val map = mapOf(resource.sha256 to path)

        transactionDraftService.createDraft(profileId, accountId)
        val userDraft = userDraftRepository.getDraftByProfile(profileId)!!

        val draft = transactionDraftService.getDraft(profileId)!!.copy(documentHashes = map)
        val gson = GsonBuilder()
            .registerTypeAdapter(LocalDate::class.java, LocalDateGsonAdapter())
            .registerTypeAdapter(LocalTime::class.java, LocalTimeGsonAdapter())
            .create()
        val jsonString = gson.toJson(draft)
        val json = JSON.json(jsonString)
        userDraftRepository.updateDraft(profileId, userDraft.id!!, json)
        transactionDraftService.finishDraft(profileId, user.uuid!!, profile.stringId!!, ZoneId.of("UTC"))

        assertEquals(0, getHashCount(resource.sha256, draftDocumentsFolder))
        assertEquals(0, getHashCount(resource.sha256, mainDocumentsFolder))
    }

    @Test
    fun givenDocumentInDraftWhenDoneThenDocumentNotInDraftAndDocumentInMainFolder() {
        val resource = TestData.Resource.Documents.EVIDENCE_IMAGE
        val classLoader = TransactionDraftServiceTest::class.java.classLoader!!
        val tempFolder = Files.createTempDirectory("temp").toFile()
        tempFolder.deleteOnExit()
        copyControllerTempFile(tempFolder, classLoader, resource)
        val tempFile = File(tempFolder, resource.fileName)


        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)


        transactionDraftService.createDraft(profileId, accountId)

        transactionDraftService.addDocumentToDraft(profileId, DocumentType.Other, tempFile, "image/jpeg",  user.uuid!!, profile.stringId!!)

        transactionDraftService.finishDraft(profileId, user.uuid!!, profile.stringId!!, ZoneId.of("UTC"))

        val draftDocumentsFolder = file(timeHandler.getRoot(),
            Constants.FOLDER_NAME_USERS,user.uuid!!,
            Constants.FOLDER_NAME_PROFILES, profile.stringId!!,
            Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_DOCUMENTS)
        val mainDocumentsFolder = file(timeHandler.getRoot(),
            Constants.FOLDER_NAME_USERS,user.uuid!!,
            Constants.FOLDER_NAME_PROFILES, profile.stringId!!,
            Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_DOCUMENTS,
            "2025","06","30")

        assertEquals(0, getHashCount(resource.sha256, draftDocumentsFolder))
        assertEquals(1, getHashCount(resource.sha256, mainDocumentsFolder))
    }

    @Test
    fun givenDocumentExistsInDraftWhenAddSameDocumentToTransactionThenDocumentOnlyExistsOnce() {
        val resource = TestData.Resource.Documents.EVIDENCE_IMAGE
        val classLoader = TransactionDraftServiceTest::class.java.classLoader!!
        val tempFolder = Files.createTempDirectory("temp").toFile()
        tempFolder.deleteOnExit()
        copyControllerTempFile(tempFolder, classLoader, resource)
        val tempFile = File(tempFolder, resource.fileName)


        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)


        transactionDraftService.createDraft(profileId, accountId)
        val draft = transactionDraftService.getDraft(profileId)!!

        transactionDraftService.addDocumentToDraft(profileId, DocumentType.Other, tempFile, "image/jpeg",  user.uuid!!, profile.stringId!!)
        transactionDraftService.addDocumentToDraft(profileId, DocumentType.Other, tempFile, "image/jpeg",  user.uuid!!, profile.stringId!!)


        val newDraft = transactionDraftService.getDraft(profileId)!!
        val draftDocumentsFolder = file(timeHandler.getRoot(),
            Constants.FOLDER_NAME_USERS,user.uuid!!,
            Constants.FOLDER_NAME_PROFILES, profile.stringId!!,
            Constants.FOLDER_NAME_DRAFT, Constants.SUBFOLDER_NAME_DOCUMENTS)

        val documents = newDraft.documents
        assertEquals(1, documents.size)
        assertEquals(1, getHashCount(resource.sha256, draftDocumentsFolder))
    }

    fun createTransactionWithImage(profileId: Long, userUuid: String, profileStringId: String, accountId: Long, tempFile: File, draftFile: TransactionDraftFile) {
        val amount = BigDecimal.TEN
        val account = accountRepository.findByProfileIdAndId(profileId, accountId)!!

        val maxOrdinal = transactionRepository.getMaxOrdinalInDayForAccount(profileId, accountId, MOCKED_DATE)?: 0
        var transactionOrdinal = maxOrdinal + 1

        val transaction = TransactionPojo(null, accountId, amount, account.currencyCode,
            null, true.toByte(), MOCKED_DATE, transactionOrdinal,
            TransactionMode.Other, null, null, null, MOCKED_DATE.atTime(MOCKED_TIME), MOCKED_TIME)

        val transactionId = transactionRepository.insertTransaction(transaction)
        val misc = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!

        val item = TransactionItemPojo(null, transactionId, "Basic", amount, misc.id!!, 1, 1, null,
            null, null, false.toByte(),
            MOCKED_DATE.atTime(MOCKED_TIME))
        val itemId = transactionItemRepository.insertTransactionItem(item)
        //TODO make this more convenient
        val newFileName = "${UUID.randomUUID()}.jpg"
        val newPath = "/${Constants.FOLDER_NAME_USERS}/$userUuid/${Constants.FOLDER_NAME_PROFILES}/$profileStringId/${Constants.FOLDER_NAME_DATA}/${Constants.SUBFOLDER_NAME_IMAGES}/${newFileName}"
        val destination = file(timeHandler.getRoot(), Constants.FOLDER_NAME_USERS, userUuid, Constants.FOLDER_NAME_PROFILES,profileStringId, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_IMAGES, newFileName)

        tempFile.copyTo(destination)
        tempFile.delete()
        val image = ImagePojo(null, profileId, draftFile.sizeBytes, draftFile.sha256, draftFile.mimeType, "", newPath, MOCKED_DATE.atTime(MOCKED_TIME))
        val imageId = imageRepository.insertImage(image)
        val itemImage = TransactionItemImagesPojo(null, itemId, imageId, MOCKED_DATE.atTime(MOCKED_TIME))
        itemImagesRepository.insertItemImage(itemImage)
    }

    fun createTransactionWithDocument(profileId: Long, userUuid: String, profileStringId: String, accountId: Long, tempFolder: File, resource: TestData.Resource) {
        val amount = BigDecimal.TEN
        val account = accountRepository.findByProfileIdAndId(profileId, accountId)!!

        val maxOrdinal = transactionRepository.getMaxOrdinalInDayForAccount(profileId, accountId, MOCKED_DATE)?: 0
        var transactionOrdinal = maxOrdinal + 1

        val transaction = TransactionPojo(null, accountId, amount, account.currencyCode,
            null, true.toByte(), MOCKED_DATE, transactionOrdinal,
            TransactionMode.Other, null, null, null, MOCKED_DATE.atTime(MOCKED_TIME), MOCKED_TIME)

        val transactionId = transactionRepository.insertTransaction(transaction)
        val misc = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!

        val item = TransactionItemPojo(null, transactionId, "Basic", amount, misc.id!!, 1, 1, null,
            null, null, false.toByte(),
            MOCKED_DATE.atTime(MOCKED_TIME))
        val itemId = transactionItemRepository.insertTransactionItem(item)
        //TODO make this more convenient
        val newFileName = "${UUID.randomUUID()}.jpg"
        val newPath = "/${Constants.FOLDER_NAME_USERS}/$userUuid/${Constants.FOLDER_NAME_PROFILES}/$profileStringId/${Constants.FOLDER_NAME_DATA}/${Constants.SUBFOLDER_NAME_DOCUMENTS}/${newFileName}"
        val destination = file(timeHandler.getRoot(), Constants.FOLDER_NAME_USERS, userUuid, Constants.FOLDER_NAME_PROFILES,profileStringId, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_DOCUMENTS, newFileName)

        val tempFile = File(tempFolder, resource.fileName)
        val length = tempFile.length()
        tempFile.copyTo(destination)
        tempFile.delete()
        val document = DocumentPojo(null, transactionId, DocumentType.Other, length, resource.sha256, "image/jpeg", "", newPath, MOCKED_DATE.atTime(MOCKED_TIME))
        val documentId = documentRepository.insertDocument(document)
    }

    companion object {
        val MOCKED_DATE = LocalDate.of(2025, 6, 30)
        val MOCKED_TIME = LocalTime.of(8, 0)
    }
}