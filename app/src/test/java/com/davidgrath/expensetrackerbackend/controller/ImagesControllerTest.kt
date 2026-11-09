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
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.FALSE_BYTE
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.JWT_ALG
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.TRUE_BYTE
import com.davidgrath.expensetrackerbackend.utils.bytesToUUID
import com.davidgrath.expensetrackerbackend.utils.file
import com.davidgrath.expensetrackerbackend.utils.getSha256
import com.davidgrath.expensetrackerbackend.utils.getSha256AsBytes
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
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val resource = TestData.Resource.Images.BREAD
        val classLoader = ImagesControllerTest::class.java.classLoader

        val profileUuidString = bytesToUUID(profile.uuid!!).toString()

        testUtilComponent.createTransactionWithImage(profileId, user.uuid!!, profile.uuid!!, accountId, classLoader, resource)

        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.get(ImagesApi.PATH_IMAGES_GET)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", profileUuidString)
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
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val resource = TestData.Resource.Images.BREAD
        val classLoader = ImagesControllerTest::class.java.classLoader

        val profileUuidString = bytesToUUID(profile.uuid!!).toString()

        val imageId = testUtilComponent.createTransactionWithImage(profileId, user.uuid!!, profile.uuid!!, accountId, classLoader, resource)

        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.get(ImagesApi.PATH_IMAGES_ID_GET, imageId)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", profileUuidString)
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
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val resource = TestData.Resource.Images.BREAD
        val classLoader = ImagesControllerTest::class.java.classLoader

        val profileUuidString = bytesToUUID(profile.uuid!!).toString()

        val imageId = testUtilComponent.createTransactionWithImage(profileId, user.uuid!!, profile.uuid!!, accountId, classLoader, resource)

        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.get(ImagesApi.PATH_IMAGES_STATS_GET)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", profileUuidString)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk,
                MockMvcResultMatchers.jsonPath(
                    "$.count", Matchers.equalTo<Int>(1)
                )
            ).andReturn()
    }



}