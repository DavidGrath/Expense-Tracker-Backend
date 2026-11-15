package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.configuration.SecurityConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TestRunControllerConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.AccountPojo
import com.davidgrath.expensetrackerbackend.generatedapi.api.AccountsApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.AccountCreateRequest
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.repository.UserRepository
import com.davidgrath.expensetrackerbackend.testutils.TestConstants
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.createBearerToken
import com.davidgrath.expensetrackerbackend.testutils.tempInit
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.FALSE_BYTE
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.JWT_ALG
import com.davidgrath.expensetrackerbackend.utils.bytesToUUID
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
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
import java.time.LocalDateTime

@RunWith(SpringRunner::class)
@WebMvcTest(AccountsController::class)
@ContextConfiguration(
    classes = [
        AccountsController::class, SecurityConfiguration::class, TestRunControllerConfiguration::class, TestAuthController::class, ExceptionResponseHandler::class, TestUtilComponent::class
    ])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AccountsControllerPostTest {

    @Autowired
    lateinit var mockMvc: MockMvc
    @Autowired
    lateinit var testUtilComponent: TestUtilComponent
    @Autowired
    lateinit var userRepository: UserRepository
    @Autowired
    lateinit var profileRepository: ProfileRepository
    @Autowired
    lateinit var accountRepository: AccountRepository

    @Value("\${jwt.signingkey}")
    private val jwtSigningKey: String? = null
    @Autowired
    lateinit var timeHandler: TimeHandler

    val objectMapper = ObjectMapper()
    val objectWriter = objectMapper.writer()

    @Before
    fun setUp() {
        tempInit(testUtilComponent, timeHandler)
    }

    @After
    fun tearDown() {
        testUtilComponent.deleteAll()
    }

    val endpoint = AccountsApi.PATH_ACCOUNTS_POST
    val verb = HttpMethod.POST
    //TODO IdorTest


    @Test
    fun postAccountOkTest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val createRequest = AccountCreateRequest("New account", "GBP")
        var json: String = objectWriter.writeValueAsString(createRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.request(verb, endpoint)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk
            ).andReturn()

        //TODO Maybe side effects
    }

    @Test
    fun givenAccountCountIsMaxWhenCreateThenBadRequest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val existing = accountRepository.getAllByProfileId(profile.id!!)
        val remaining = Constants.Constraints.MAX_ACCOUNTS_PER_PROFILE - existing.size
        if(remaining <= 0) {
            throw RuntimeException("Existing accounts too many")
        }
        for(acc in 0 until remaining) {
            val nextOrdinal = ((acc+1)+existing.size).toShort()
            val nextSortKey = (((acc+1)+existing.size)*1_000).toShort()
            val nextOccupancy = ((acc+1)+existing.size).toShort()
            accountRepository.insertAccount(AccountPojo(null, profile.id!!, "USD", null, null, "New Account $acc",
                FALSE_BYTE, nextOrdinal, nextSortKey, nextOccupancy,LocalDateTime.now(timeHandler.getClock())))
        }
        val createRequest = AccountCreateRequest("New account", "GBP")
        var json: String = objectWriter.writeValueAsString(createRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.request(verb, endpoint)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isBadRequest,
                MockMvcResultMatchers.jsonPath(
                    "$.${Constants.CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE}", Matchers.equalTo(ErrorCode.LIMITS_EXCEEDED.code)
                )
            ).andReturn()
    }


    @Test
    fun givenCurrencyCodeInvalidWhenCreateAccountThenBadRequest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val createRequest = AccountCreateRequest("New Account", "XYZ")
        var json: String = objectWriter.writeValueAsString(createRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.request(verb, endpoint)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isBadRequest,
                MockMvcResultMatchers.jsonPath(
                    "$.${Constants.CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE}", Matchers.equalTo(ErrorCode.INVALID_CURRENCY_CODE.code)
                )
            ).andReturn()

    }


    @Test
    fun givenNameBlankWhenCreateAccountThenBadRequest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val createRequest = AccountCreateRequest(" \t\n", "GBP")
        var json: String = objectWriter.writeValueAsString(createRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.request(verb, endpoint)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isBadRequest,
                MockMvcResultMatchers.jsonPath(
                    "$.${Constants.CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE}", Matchers.equalTo(ErrorCode.INVALID_INPUT.code)
                )
            ).andReturn()
    }

}