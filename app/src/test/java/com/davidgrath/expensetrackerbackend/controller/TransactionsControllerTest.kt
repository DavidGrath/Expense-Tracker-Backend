package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.generatedapi.model.BasicTransactionCreateRequest
import com.davidgrath.expensetrackerbackend.configuration.SecurityConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TestConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.configuration.WebMvcConfiguration
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
import com.davidgrath.expensetrackerbackend.generatedapi.api.TransactionsApi
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.CategoryRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.repository.UserRepository
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.testutils.DataBuilder
import com.davidgrath.expensetrackerbackend.testutils.TestConstants
import com.davidgrath.expensetrackerbackend.testutils.TestData
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.createBearerToken
import com.davidgrath.expensetrackerbackend.testutils.getDefaultAccountId
import com.davidgrath.expensetrackerbackend.testutils.tempInit
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.JWT_ALG
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigDecimal
import java.nio.file.Files
import java.time.Duration
import java.time.LocalDate
import java.util.Locale

@RunWith(SpringRunner::class)
@WebMvcTest(TransactionsController::class)
@ContextConfiguration(
    classes = [
        TransactionsController::class, SecurityConfiguration::class, TestConfiguration::class, ExceptionResponseHandler::class,
        TestUtilComponent::class, WebMvcConfiguration::class
    ])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class TransactionsControllerTest {

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

    @Test
    fun basicTransactionValidationTest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profile.id!!, "miscellaneous")
        val objectWriter: ObjectWriter = ObjectMapper().writer()

        val nullDescriptionRequest = BasicTransactionCreateRequest(accountId, true, BigDecimal.TEN, null, misc!!.id, "Etc/UTC")
        var json: String = objectWriter.writeValueAsString(nullDescriptionRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/transactions/basic")
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", profile.stringId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isBadRequest,
                MockMvcResultMatchers.jsonPath(
                    "$.detail", Matchers.equalToIgnoringCase("Invalid request content.")
                )
            ).andReturn()

        val zeroAmountRequest = BasicTransactionCreateRequest(accountId, true, BigDecimal.ZERO, "Description", misc!!.id, "Etc/UTC")
        json = objectWriter.writeValueAsString(zeroAmountRequest)
        response = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/transactions/basic")
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", profile.stringId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isBadRequest,
                MockMvcResultMatchers.jsonPath(
                    "$.detail", Matchers.equalToIgnoringCase("Invalid request content.")
                )
            ).andReturn()

    }

    @Test
    fun givenAccountIdExistsAndAccountIdBelongsToAnotherUserWhenCreateTransactionThenNotFound() {

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val userId = userService.createUser("user2", "password", Locale.US)
        val profile2 = profileRepository.findByStringId(userId, Constants.DEFAULT_PROFILE_ID)!!
        val accounts = accountRepository.getAllByProfileId(profile2.id!!)
        val misc = categoryRepository.findByProfileIdAndStringId(profile.id!!, "miscellaneous") //This should cause a failure
        var basicTransactionRequest = BasicTransactionCreateRequest(accounts[0].id!!, true, BigDecimal.TEN, "Description", misc!!.id, "Etc/UTC")

        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)
        val objectWriter: ObjectWriter = ObjectMapper().writer()
        val json: String = objectWriter.writeValueAsString(basicTransactionRequest)
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/transactions/basic")
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", profile.stringId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isNotFound,
                MockMvcResultMatchers.jsonPath(
                    "$.$CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE", Matchers.equalTo(ErrorCode.ENTITY_NOT_FOUND.code)
                )
            )
    }

    @Test
    fun givenCategoryIdExistsAndCategoryIdBelongsToAnotherUserWhenCreateTransactionThenNotFound() {

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val userId = userService.createUser("user2", "password", Locale.US)
        val profile2 = profileRepository.findByStringId(userId, Constants.DEFAULT_PROFILE_ID)!!
        val accounts = accountRepository.getAllByProfileId(profile.id!!)
        val misc = categoryRepository.findByProfileIdAndStringId(profile2.id!!, "miscellaneous") //This should cause a failure
        var basicTransactionRequest = BasicTransactionCreateRequest(accounts[0].id!!, true, BigDecimal.TEN, "Description", misc!!.id, "Etc/UTC")

        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)
        val objectWriter: ObjectWriter = ObjectMapper().writer()
        val json: String = objectWriter.writeValueAsString(basicTransactionRequest)
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/transactions/basic")
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", profile.stringId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isNotFound,
                MockMvcResultMatchers.jsonPath(
                    "$.$CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE", Matchers.equalTo(ErrorCode.ENTITY_NOT_FOUND.code)
                )
            )
    }

    @Test
    @Ignore("Not ready yet")
    fun givenTransactionExistsAndTransactionInOtherProfileWhenGetThenNotInProfile() {

        /*val user = userRepository.findByUuid(Constants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

//        val profile2 = profileRepository.getAll(userId, Constants.DEFAULT_PROFILE_ID)!!
        val accounts = accountRepository.getAllByProfileId(profile2.id!!)
        val misc = categoryRepository.findByProfileIdAndStringId(profile.id!!, "miscellaneous") //This should cause a failure
        var basicTransactionRequest = BasicTransactionCreateRequest(accounts[0].id!!, true, BigDecimal.TEN, "Description", misc!!.id)

        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)
        val objectWriter: ObjectWriter = ObjectMapper().writer()
        val json: String = objectWriter.writeValueAsString(basicTransactionRequest)
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/transactions/{id}", transactionId)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", profile.stringId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isBadRequest(),
                MockMvcResultMatchers.jsonPath(
                    "$.errorCode", Matchers.equalTo(ErrorCode.ENTITY_NOT_IN_PROFILE.code)
                )
            )*/
    }

    @Test
    @Ignore("Not ready yet")
    fun givenAccountIdExistsAndAccountIdBelongsToAnotherProfileWhenCreateTransactionThenBadRequest() {
        assertTrue(false)
    }

    @Test
    fun givenRequestTimeZoneNotValidWhenCreateTransactionThenBadRequest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profile.id!!, "miscellaneous")
        val objectWriter: ObjectWriter = ObjectMapper().writer()

        var badTimeZoneRequest = BasicTransactionCreateRequest(accountId, true, BigDecimal.TEN, "Description", misc!!.id, "Etc/Wrong")
        var json: String = objectWriter.writeValueAsString(badTimeZoneRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/transactions/basic")
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", profile.stringId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isBadRequest,
                MockMvcResultMatchers.jsonPath(
                    "$.$CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE", Matchers.equalToIgnoringCase(ErrorCode.TIME_ZONE_NOT_VALID.code)
                )
            ).andReturn()
    }

    /**
     * Background: I started running some insertion scripts with JMeter, and later opened the Android app with the same user and got a timeout, based on OkHttp's default of 10 seconds.
     * I inspected it with Tempo, and found that from the 777 single-item transactions I inserted that day, I got a trace with 3,891 spans, only 3 of which were not DB operations (span.db.operation != nil)
     * and it took roughly 1m20s, so this test is the result of that.
     * UPDATE: My p95 improved, but the requests still take up to 23.6 seconds because of the documents.
     * I tried using withDocument to be able to test my N+1 problem fix with the documents, but my before and after are PT5.3328895S and PT2.4806974S, so I can't really tell
     */
    @Test
//    @Ignore("It will add unnecessary time to regular test runs")
    fun getTransactionsTimeTest() {
        val tempFolder = Files.createTempDirectory("temp").toFile()
        tempFolder.deleteOnExit()
        val dataBuilder = DataBuilder(tempFolder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)
        val repetitionCount = 1_000
        val dates = Array(repetitionCount - 1) { //The builder inserts with an initial date no matter what. Maybe I should change that behaviour
            LocalDate.now(timeHandler.getClock())
        }

        val insertionStartTime = System.nanoTime()
        dataBuilder.createTransaction()
            .withDetailedItem()
            .mainDetails("Desc", "miscellaneous", BigDecimal(1000.00))
            .withImages(TestData.Resource.Images.BREAD, TestData.Resource.Images.SHIRT)
            .build()
            .repeatIntoDates(dates.toList())
            .withDocument(TestData.Resource.Documents.EVIDENCE_IMAGE)
            .commit()
        val insertionEndTime = System.nanoTime()
        val dur = Duration.ofNanos(insertionEndTime - insertionStartTime)
        LOGGER.debug("Insertion time: {}", dur.toString())

        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profile.id!!, "miscellaneous")


        val startTimeNanos = System.nanoTime()
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.get(TransactionsApi.PATH_TRANSACTIONS_GET)
                    .param("accountIds", accountId.toString())
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", profile.stringId)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk,
                MockMvcResultMatchers.jsonPath(
                    "$", Matchers.hasSize<Int>(repetitionCount)
                )
            ).andReturn()
        val endTimeNanos = System.nanoTime()
        val d = endTimeNanos - startTimeNanos
        val duration = Duration.ofNanos(d)
        LOGGER.debug("Request time: {}", duration.toString())
        val durationSeconds = Duration.ofNanos(d).toSeconds()
        assertTrue(durationSeconds <= 10)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionsControllerTest::class.java)
    }
}