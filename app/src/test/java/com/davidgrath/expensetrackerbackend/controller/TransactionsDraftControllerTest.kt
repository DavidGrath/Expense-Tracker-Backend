package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.configuration.SecurityConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TestRunControllerConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.configuration.WebMvcConfiguration
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.SellerPojo
import com.davidgrath.expensetrackerbackend.generatedapi.model.DocumentType
import com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionDraftItemUpdateRequest
import com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionDraftUpdateRequest
import com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionMode
import com.davidgrath.expensetrackerbackend.repository.*
import com.davidgrath.expensetrackerbackend.service.TransactionDraftService
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.testutils.*
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.JWT_ALG
import com.davidgrath.expensetrackerbackend.utils.bytesToUUID
import com.davidgrath.expensetrackerbackend.utils.uuidToBytes
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

@RunWith(SpringRunner::class)
@WebMvcTest(TransactionsController::class)
@ContextConfiguration(
    classes = [
        TransactionsController::class, SecurityConfiguration::class, TestRunControllerConfiguration::class, ExceptionResponseHandler::class,
        TestUtilComponent::class, WebMvcConfiguration::class
    ])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class TransactionsDraftControllerTest {

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

    //Other count-based limitations were covered at the service level, so I'll just leave it at this
    @Test
    fun givenItemCountIsMaxWhenAddItemThenBadRequest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)))!!
        val profile = profileRepository.findByUuid(user.id!!, uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID)))!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)

        val remaining = Constants.Constraints.MAX_ITEMS_PER_TRANSACTION - 1

        transactionDraftService.createDraft(profile.id!!, accountId)
        for(i in 1..remaining) {
            transactionDraftService.addItemToDraft(profile.id!!)
        }

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/transactions/draft/items")
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", Constants.DEFAULT_PROFILE_ID)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isBadRequest,
                MockMvcResultMatchers.jsonPath(
                    "$.${Constants.CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE}", Matchers.equalTo(ErrorCode.LIMITS_EXCEEDED.code)
                )
            ).andReturn()
    }

    @Test
    fun givenDraftNotExistWhenPerformAnyOtherOperationThanPostThenNotFound() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)))!!
        val profile = profileRepository.findByUuid(user.id!!, uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID)))!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profile.id!!, "miscellaneous")

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/transactions/draft/items")
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", Constants.DEFAULT_PROFILE_ID)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isNotFound,
                MockMvcResultMatchers.jsonPath(
                    "$.${Constants.CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE}", Matchers.equalTo(ErrorCode.DRAFT_DOES_NOT_EXIST.code)
                )
            ).andReturn()
    }

    /**
     * Primarily seller,account,category, but I feel like this IDOR thing can be handled at a higher level to reduce
     * the chances of me forgetting
     */
    @Test
    fun givenAnyEntityNotBelongToUserWhenModifyDraftThenNotFound() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profile.id!!, "miscellaneous")
        val user2Id = userService.createUser("user2", "password", Locale.US)
        val profile2 = profileRepository.findByUuid(user2Id, Constants.DEFAULT_PROFILE_ID)!!

        val date = ZonedDateTime.now(timeHandler.getClock())
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()
        val seller2Id = sellerRepository.insertSeller(SellerPojo(null, profile2.id!!, "Restaurant", 1, 1_000, 1, utcDate))

        transactionDraftService.createDraft(profile!!.id!!, accountId)

        val draftUpdateRequest = TransactionDraftUpdateRequest(null, null, accountId, true, null, TransactionMode.OTHER, seller2Id, null)

        val objectWriter: ObjectWriter = ObjectMapper().writer()
        val json = objectWriter.writeValueAsString(draftUpdateRequest)
        val response = mockMvc
            .perform(
                MockMvcRequestBuilders.put("/transactions/draft")
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isNotFound,
                MockMvcResultMatchers.jsonPath(
                    "$.${Constants.CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE}", Matchers.equalTo(ErrorCode.ENTITY_NOT_FOUND.code)
                )
            ).andReturn()
    }

    @Test
    fun givenDraftExistsWhenCreateDraftThenBadRequest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profile.id!!, "miscellaneous")
        transactionDraftService.createDraft(profile!!.id!!, accountId)

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/transactions/draft")
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isBadRequest,
                MockMvcResultMatchers.jsonPath(
                    "$.${Constants.CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE}", Matchers.equalTo(ErrorCode.DRAFT_ALREADY_EXISTS.code)
                )
            ).andReturn()
    }


    @Test
    fun documentSizePassTest() {
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


        val mockFile = MockMultipartFile("document", null, MediaType.IMAGE_JPEG_VALUE, byteArray)

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.multipart("/transactions/draft/documents")
                    .file(mockFile)
                    .param("type", DocumentType.OTHER.value)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
                    .content(byteArray)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk
            ).andReturn()
    }

    // Some cases are already covered at the service level tests, so I'll just be generic here
    @Test
    fun givenDraftHasErrorsWhenSaveThenFail() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profile.id!!, "miscellaneous")
        transactionDraftService.createDraft(profile!!.id!!, accountId)

        var emptyDraftResponse = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/transactions")
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isBadRequest, // Pass an array of `errorCode`s or just a general bad request?
            ).andReturn()

        val draft = transactionDraftService.getDraft(profile.id!!)
        val item = draft!!.items[0]
        val uuid = item.uuid

        val itemUpdateRequest = TransactionDraftItemUpdateRequest(BigDecimal.TEN, " \t ", misc!!.id!!)

        val objectWriter: ObjectWriter = ObjectMapper().writer()
        val json = objectWriter.writeValueAsString(itemUpdateRequest)

        mockMvc
            .perform(
                MockMvcRequestBuilders.put("/transactions/draft/items/{uuid}", uuid)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk,
            ).andReturn()

        val badItemsRequest = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/transactions")
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isBadRequest,
            ).andReturn()
    }

    @Test
    fun givenDraftHasOneItemWhenDeleteThenFail() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)


        transactionDraftService.createDraft(profile.id!!, accountId)

        val draft = transactionDraftService.getDraft(profile.id!!)
        val item = draft!!.items[0]
        val uuid = item.uuid


        mockMvc
            .perform(
                MockMvcRequestBuilders.delete("/transactions/draft/items/{uuid}", uuid)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isBadRequest,
                MockMvcResultMatchers.jsonPath(
                    "$.${Constants.CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE}", Matchers.equalTo(ErrorCode.DRAFT_MUST_HAVE_AT_LEAST_ONE_ITEM.code)
                )
            ).andReturn()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionsDraftControllerTest::class.java)
    }
}