package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.configuration.SecurityConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TestRunControllerConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.configuration.WebMvcConfiguration
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
import com.davidgrath.expensetrackerbackend.generatedapi.api.TransactionsApi
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.CategoryRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.repository.SellerRepository
import com.davidgrath.expensetrackerbackend.repository.UserRepository
import com.davidgrath.expensetrackerbackend.service.TransactionDraftService
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.testutils.TestConstants
import com.davidgrath.expensetrackerbackend.testutils.TestData
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.createBearerToken
import com.davidgrath.expensetrackerbackend.testutils.getDefaultAccountId
import com.davidgrath.expensetrackerbackend.testutils.tempInit
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.JWT_ALG
import com.davidgrath.expensetrackerbackend.utils.bytesToUUID
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@RunWith(SpringRunner::class)
@WebMvcTest(TransactionsController::class)
@ContextConfiguration(
    classes = [
        TransactionsController::class, SecurityConfiguration::class, TestRunControllerConfiguration::class, ExceptionResponseHandler::class,
        TestUtilComponent::class, WebMvcConfiguration::class
    ])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class TransactionsDraftControllerPostItemUuidImagesTest {

    val endpoint = TransactionsApi.PATH_TRANSACTIONS_DRAFT_ITEMS_UUID_IMAGES_POST
    val verb = HttpMethod.POST


    @Autowired
    lateinit var mockMvc: MockMvc
    @Autowired
    lateinit var userService: UserService
    @Autowired
    lateinit var testUtilComponent: TestUtilComponent
    @Autowired
    lateinit var userRepository: UserRepository
    @Autowired
    lateinit var profileRepository: ProfileRepository
    @Autowired
    lateinit var accountRepository: AccountRepository
    @Autowired
    lateinit var categoryRepository: CategoryRepository
    @Autowired
    lateinit var transactionDraftService: TransactionDraftService
    @Autowired
    lateinit var sellerRepository: SellerRepository
    @Autowired
    lateinit var timeHandler: TimeHandler

    @Value("\${jwt.signingkey}")
    private val jwtSigningKey: String? = null

    @Before
    fun setUp() {
        tempInit(testUtilComponent, timeHandler)
    }

    @After
    fun tearDown() {
        testUtilComponent.deleteAll()
    }

    //TODO Ok Count limit reached, file contents invalid, file is not accepted format, item not found, image already attached to item (204 no content)


    @Test
    fun imageSizeFailTest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profile.id!!, "miscellaneous")
        transactionDraftService.createDraft(profile!!.id!!, accountId)

        val classLoader = TransactionsDraftControllerTest::class.java.classLoader!!
        val resourceAbove3MiB = TestData.Resource.Images.TRAMPOLINE
        val inStream = classLoader.getResourceAsStream(resourceAbove3MiB.resourceName)!!
        val byteArray = inStream.readBytes()
        inStream.close()

        val draft = transactionDraftService.getDraft(profile.id!!)
        val item = draft!!.items[0]
        val uuid = item.uuid

        LOGGER.debug("MediaType: {}", MediaType.IMAGE_JPEG.toString())
        val mockFile = MockMultipartFile("file", null, MediaType.IMAGE_JPEG.type, byteArray)

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.multipart("/transactions/draft/items/{uuid}/images", uuid)
                    .file(mockFile)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isBadRequest
            ).andReturn()
    }

    @Test
    fun imageSizePassTest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profile.id!!, "miscellaneous")
        transactionDraftService.createDraft(profile!!.id!!, accountId)

        val classLoader = TransactionsDraftControllerTest::class.java.classLoader!!
        val resourceBetween1And3MiB = TestData.Resource.Images.SHIRT
        val inStream = classLoader.getResourceAsStream(resourceBetween1And3MiB.resourceName)!!
        val byteArray = inStream.readBytes()
        inStream.close()

        val draft = transactionDraftService.getDraft(profile.id!!)
        val item = draft!!.items[0]
        val uuid = item.uuid

        val mockFile = MockMultipartFile("file", null, MediaType.IMAGE_JPEG.type, byteArray)

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.multipart("/transactions/draft/items/{uuid}/images", uuid)
                    .file(mockFile)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk
            ).andReturn()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionsDraftControllerPostItemUuidImagesTest::class.java)
    }

}