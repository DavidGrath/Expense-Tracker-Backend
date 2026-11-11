package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.configuration.SecurityConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TestRunControllerConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.AccountPojo
import com.davidgrath.expensetrackerbackend.generatedapi.model.AccountCreateRequest
import com.davidgrath.expensetrackerbackend.generatedapi.model.AccountCurrencyUpdateRequest
import com.davidgrath.expensetrackerbackend.generatedapi.model.AccountNameUpdateRequest
import com.davidgrath.expensetrackerbackend.generatedapi.model.AccountUpdateRequest
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.repository.UserRepository
import com.davidgrath.expensetrackerbackend.testutils.TestConstants
import com.davidgrath.expensetrackerbackend.testutils.TestConstants.Companion.DEFAULT_ACCOUNT_NAME
import com.davidgrath.expensetrackerbackend.testutils.TestConstants.Companion.DEFAULT_CURRENCY_CODE
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.createBearerToken
import com.davidgrath.expensetrackerbackend.testutils.tempInit
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.FALSE_BYTE
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.JWT_ALG
import com.davidgrath.expensetrackerbackend.utils.bytesToUUID
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import junit.framework.Assert.assertTrue
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
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
import java.time.LocalDateTime

@RunWith(SpringRunner::class)
@WebMvcTest(AccountsController::class)
@ContextConfiguration(
    classes = [
        AccountsController::class, SecurityConfiguration::class, TestRunControllerConfiguration::class, TestAuthController::class, ExceptionResponseHandler::class, TestUtilComponent::class
    ])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AccountsControllerTest {

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

    @Test //TODO Exclude Jupiter so I don't mistakenly import it at some point
    fun getAccountHappyPathTest() {
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!


        val account = accountRepository.getAllByProfileId(profile.id!!).firstOrNull()!!


        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/accounts/{id}", account.id)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
            )
            .andExpectAll(
                MockMvcResultMatchers.status().is2xxSuccessful
            )
    }


    @Test
    @Ignore("Later")
    fun givenAccountNotExistOrNotForUserWhenGetByIdThenNotFound() {
        assertTrue(false)
    }

    @Test
    @Ignore("Later")
    fun givenAccountNotForProfileWhenGetByIdThenNotFound() {
        assertTrue(false)
    }

    /**
     * This test feels like a false positive because I got an exception from referenceNumber not being nullable and this test passed while it was still non-nullable
     */
    @Test
    fun getAccountsHappyPathTest() {
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!



        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/accounts")
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
            )
            .andExpectAll(
                MockMvcResultMatchers.status().is2xxSuccessful,
                MockMvcResultMatchers.jsonPath("$", Matchers.hasSize<Int>(1))
            )
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
                MockMvcRequestBuilders.post("/accounts")
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

    @Test
    fun givenCurrencyCodeInvalidWhenCreateAccountThenBadRequest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val createRequest = AccountCreateRequest("New Account", "XYZ")
        var json: String = objectWriter.writeValueAsString(createRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/accounts")
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
                MockMvcRequestBuilders.post("/accounts")
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
    fun getAllOkTest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.get("/accounts")
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk,
                MockMvcResultMatchers.jsonPath(
                    "$.*", Matchers.iterableWithSize<Int>(Matchers.greaterThanOrEqualTo(1))
                )
            ).andReturn()
    }

    @Test
    fun getAllDetailsOkTest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.get("/accounts/details")
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk,
                MockMvcResultMatchers.jsonPath(
                    "$.*", Matchers.iterableWithSize<Int>(Matchers.greaterThanOrEqualTo(1))
                )
            ).andReturn()
    }



    @Test
    fun givenNoChangesWhenChangeDefaultAccountThenNoContentAndNoBody() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val defaultAccountId = accountRepository.findDefaultAccountId(profile.id!!)!!

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/accounts/{id}/default", defaultAccountId)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isNoContent,
                MockMvcResultMatchers.content().string("")
            ).andReturn()
    }

    @Test
    fun accountDefaultChangeOkTest() {

        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val defaultAccountId = accountRepository.findDefaultAccountId(profile.id!!)!!

        val newAccountId = accountRepository.insertAccount(AccountPojo(null, profile.id!!, "USD", null, null, "New Account",
            FALSE_BYTE, 2, 2_000, 2,LocalDateTime.now(timeHandler.getClock())))


        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/accounts/{id}/default", newAccountId)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk,
                MockMvcResultMatchers.jsonPath("$.oldDefaultId", Matchers.equalTo(defaultAccountId.toInt())),
                MockMvcResultMatchers.jsonPath("$.newDefaultId", Matchers.equalTo(newAccountId.toInt()))
            ).andReturn()
    }

    //TODO I'll start creating dedicated subclasses per endpoint. Region marker comments feel much weaker, and also, it feels like another reason for upgrading to JUnit 5

}