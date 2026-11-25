package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.configuration.SecurityConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TestRunControllerConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.configuration.WebMvcConfiguration
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
import com.davidgrath.expensetrackerbackend.generatedapi.api.TransactionsApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.BasicTransactionCreateRequest
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.CategoryRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.repository.UserRepository
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.testutils.TestConstants
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.createBearerToken
import com.davidgrath.expensetrackerbackend.testutils.getDefaultAccountId
import com.davidgrath.expensetrackerbackend.testutils.tempInit
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.JWT_ALG
import com.davidgrath.expensetrackerbackend.utils.bytesToUUID
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigDecimal
import java.util.Locale

@RunWith(SpringRunner::class)
@WebMvcTest(TransactionsController::class)
@ContextConfiguration(
    classes = [
        TransactionsController::class, SecurityConfiguration::class, TestRunControllerConfiguration::class, ExceptionResponseHandler::class,
        TestUtilComponent::class, WebMvcConfiguration::class
    ])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class TransactionsControllerPostBasicTest {

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

    val objectWriter: ObjectWriter = ObjectMapper().writer()

    @Before
    fun setUp() {
        tempInit(testUtilComponent, timeHandler)
    }

    @After
    fun tearDown() {
        testUtilComponent.deleteAll()
    }

    val endpoint = TransactionsApi.PATH_TRANSACTIONS_BASIC_POST
    val verb = HttpMethod.POST

    @Test
    fun transactionBasicPostOkTest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profile.id!!, "miscellaneous")

        var badTimeZoneRequest = BasicTransactionCreateRequest(accountId, true, BigDecimal.TEN, "Description", misc!!.id, "Etc/UTC")
        var json: String = objectWriter.writeValueAsString(badTimeZoneRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.request(verb, endpoint)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!).toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk
            ).andReturn()
    }


    @Test
    fun basicTransactionValidationTest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profile.id!!, "miscellaneous")


        val nullDescriptionRequest = BasicTransactionCreateRequest(accountId, true, BigDecimal.TEN, null, misc!!.id, "Etc/UTC")
        var json: String = objectWriter.writeValueAsString(nullDescriptionRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.request(verb, endpoint)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!).toString())
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
                MockMvcRequestBuilders.request(verb, endpoint)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!).toString())
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
    fun givenRequestTimeZoneNotValidWhenCreateTransactionThenBadRequest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profile.id!!, "miscellaneous")

        var badTimeZoneRequest = BasicTransactionCreateRequest(accountId, true, BigDecimal.TEN, "Description", misc!!.id, "Etc/Wrong")
        var json: String = objectWriter.writeValueAsString(badTimeZoneRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.request(verb, endpoint)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!).toString())
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


    @Test
    fun givenAccountIdExistsAndAccountIdBelongsToAnotherUserWhenCreateTransactionThenNotFound() {

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val userId = userService.createUser("user2", "password", Locale.US)
        val profile2 = profileRepository.findByUuid(userId, Constants.DEFAULT_PROFILE_ID)!!
        val accounts = accountRepository.getAllByProfileId(profile2.id!!)
        val misc = categoryRepository.findByProfileIdAndStringId(profile.id!!, "miscellaneous") //TODO This should cause a failure
        var basicTransactionRequest = BasicTransactionCreateRequest(accounts[0].id!!, true, BigDecimal.TEN, "Description", misc!!.id, "Etc/UTC")

        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)
        val json: String = objectWriter.writeValueAsString(basicTransactionRequest)
        mockMvc
            .perform(
                MockMvcRequestBuilders.request(verb, endpoint)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!).toString())
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
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val userId = userService.createUser("user2", "password", Locale.US)
        val profile2 = profileRepository.findByUuid(userId, Constants.DEFAULT_PROFILE_ID)!!
        val accounts = accountRepository.getAllByProfileId(profile.id!!)
        val misc = categoryRepository.findByProfileIdAndStringId(profile2.id!!, "miscellaneous") //TODO This should cause a failure
        var basicTransactionRequest = BasicTransactionCreateRequest(accounts[0].id!!, true, BigDecimal.TEN, "Description", misc!!.id, "Etc/UTC")

        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)
        val json: String = objectWriter.writeValueAsString(basicTransactionRequest)
        mockMvc
            .perform(
                MockMvcRequestBuilders.request(verb, endpoint)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!).toString())
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
    fun givenAccountIdExistsAndAccountIdBelongsToAnotherProfileWhenCreateTransactionThenBadRequest() {
        assertTrue(false)
    }


    @Test
    @Ignore("Not ready yet")
    fun givenCategoryIdExistsAndCategoryIdBelongsToAnotherProfileWhenCreateTransactionThenNotFound() {
        assertTrue(false)
    }


}