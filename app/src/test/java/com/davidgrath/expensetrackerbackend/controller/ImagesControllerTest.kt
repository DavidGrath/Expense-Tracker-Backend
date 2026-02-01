package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.configuration.SecurityConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TestRunControllerConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ImagePojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionItemImagesPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionItemPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionPojo
import com.davidgrath.expensetrackerbackend.generatedapi.api.ImagesApi
import com.davidgrath.expensetrackerbackend.repository.*
import com.davidgrath.expensetrackerbackend.service.TransactionDraftServiceTest.Companion.MOCKED_DATE
import com.davidgrath.expensetrackerbackend.service.TransactionDraftServiceTest.Companion.MOCKED_TIME
import com.davidgrath.expensetrackerbackend.testutils.*
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.JWT_ALG
import com.davidgrath.expensetrackerbackend.utils.file
import com.davidgrath.expensetrackerbackend.utils.getSha256
import com.davidgrath.expensetrackerbackend.utils.toByte
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.util.*

@RunWith(SpringRunner::class)
@WebMvcTest(ImagesController::class)
@ContextConfiguration(
    classes = [
        ImagesController::class, SecurityConfiguration::class, TestRunControllerConfiguration::class, ExceptionResponseHandler::class
    ])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ImagesControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc
    @Autowired
    lateinit var testUtilComponent: TestUtilComponent
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
    lateinit var imageRepository: ImageRepository
    @Autowired
    lateinit var userRepository: UserRepository
    @Autowired
    lateinit var profileRepository: ProfileRepository

    @Value("\${jwt.signingkey}")
    private val jwtSigningKey: String? = null
    @Autowired
    lateinit var timeHandler: TimeHandler

    @Before
    fun setUp() {
        tempInit(testUtilComponent, timeHandler)
    }

    @After
    fun tearDown() {
        testUtilComponent.deleteAll()
    }

    @Test
    fun getAllImagesOkTest() {
        val tempFolder = Files.createTempDirectory("temp").toFile()
        tempFolder.deleteOnExit()

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val resource = TestData.Resource.Images.BREAD
        val classLoader = ImagesControllerTest::class.java.classLoader


        createTransactionWithImage(profileId, user.uuid!!, profile.stringId!!, accountId, classLoader, resource)

        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.get(ImagesApi.PATH_IMAGES_GET)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", profile.stringId)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk,
                MockMvcResultMatchers.jsonPath(
                    "$.*", Matchers.iterableWithSize<Int>(Matchers.equalTo(1))
                )
            ).andReturn()
    }

    @Test
    fun getImageByIdOkTest() {
        val tempFolder = Files.createTempDirectory("temp").toFile()
        tempFolder.deleteOnExit()

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val resource = TestData.Resource.Images.BREAD
        val classLoader = ImagesControllerTest::class.java.classLoader


        val imageId = createTransactionWithImage(profileId, user.uuid!!, profile.stringId!!, accountId, classLoader, resource)

        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.get(ImagesApi.PATH_IMAGES_ID_GET, imageId)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", profile.stringId)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk
            ).andReturn()
    }

    @Test
    fun getImageStatsOkTest() {
        val tempFolder = Files.createTempDirectory("temp").toFile()
        tempFolder.deleteOnExit()

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val resource = TestData.Resource.Images.BREAD
        val classLoader = ImagesControllerTest::class.java.classLoader


        val imageId = createTransactionWithImage(profileId, user.uuid!!, profile.stringId!!, accountId, classLoader, resource)

        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.get(ImagesApi.PATH_IMAGES_STATS_GET)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", profile.stringId)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk,
                MockMvcResultMatchers.jsonPath(
                    "$.count", Matchers.equalTo<Int>(1)
                )
            ).andReturn()
    }


    fun createTransactionWithImage(profileId: Long, userUuid: String, profileStringId: String, accountId: Long, classLoader: ClassLoader, resource: TestData.Resource): Long {
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
        val newPath = "/${Constants.FOLDER_NAME_PROFILES}/$profileStringId/${Constants.FOLDER_NAME_DATA}/${Constants.SUBFOLDER_NAME_IMAGES}/${newFileName}"
        val destinationFolder = file(timeHandler.getRoot(), Constants.FOLDER_NAME_USERS, userUuid, Constants.FOLDER_NAME_PROFILES,profileStringId, Constants.FOLDER_NAME_DATA, Constants.SUBFOLDER_NAME_IMAGES)
        destinationFolder.mkdirs()
        val destination = File(destinationFolder, newFileName)

        val inputStream = classLoader.getResourceAsStream(resource.resourceName)
        val outputStream = destination.outputStream()
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        val hashStream = destination.inputStream()

        val hash = getSha256(hashStream)
        hashStream.close()

        val image = ImagePojo(null, profileId, destination.length(), hash, "image/jpeg", "", newPath, MOCKED_DATE.atTime(MOCKED_TIME))
        val imageId = imageRepository.insertImage(image)
        val itemImage = TransactionItemImagesPojo(null, itemId, imageId, MOCKED_DATE.atTime(MOCKED_TIME))
        itemImagesRepository.insertItemImage(itemImage)
        return imageId
    }
}