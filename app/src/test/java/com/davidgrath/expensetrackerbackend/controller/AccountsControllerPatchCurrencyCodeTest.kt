package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.configuration.SecurityConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TestRunControllerConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
import com.davidgrath.expensetrackerbackend.generatedapi.model.AccountCurrencyUpdateRequest
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.repository.UserRepository
import com.davidgrath.expensetrackerbackend.testutils.TestConstants
import com.davidgrath.expensetrackerbackend.testutils.TestConstants.Companion.DEFAULT_CURRENCY_CODE
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.createBearerToken
import com.davidgrath.expensetrackerbackend.testutils.tempInit
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.JWT_ALG
import com.davidgrath.expensetrackerbackend.utils.bytesToUUID
import com.fasterxml.jackson.databind.ObjectMapper
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

@RunWith(SpringRunner::class)
@WebMvcTest(AccountsController::class)
@ContextConfiguration(
    classes = [
        AccountsController::class, SecurityConfiguration::class, TestRunControllerConfiguration::class, TestAuthController::class, ExceptionResponseHandler::class, TestUtilComponent::class
    ])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AccountsControllerPatchCurrencyCodeTest {
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


    @Test
    //TODO Not found
    fun accountCurrencyPatchOkTest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!


        val defaultAccountId = accountRepository.getAllByProfileId(profile.id!!).firstOrNull()!!.id!!
        val newCode = "GBP"
        val updateRequest = AccountCurrencyUpdateRequest(newCode)
        var json: String = objectWriter.writeValueAsString(updateRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.patch("/accounts/{id}/currencyCode", defaultAccountId)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk,
            ).andReturn()
        //TODO I'm generally not sure if I should consistently test the side effects of controller methods after the webMvc assertion (e.g in this case: accountRepository, assertEquals newCode currencyCode)
    }


    @Test
    fun givenCurrencyCodeInvalidWhenUpdateAccountPatchThenBadRequest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!




        val defaultAccountId = accountRepository.getAllByProfileId(profile.id!!).firstOrNull()!!.id!!
        val badCode = "XYZ"
        val updateRequest = AccountCurrencyUpdateRequest(badCode)
        var json: String = objectWriter.writeValueAsString(updateRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.patch("/accounts/{id}/currencyCode", defaultAccountId)
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
    @Ignore("row_count and the value returned by JDBC through jooq both return 1 instead of 0 for some reason. Tackle later")
    fun givenNoChangesWhenUpdateAccountCurrencyCodePatchThenNoContent() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val defaultAccountId = accountRepository.getAllByProfileId(profile.id!!).firstOrNull()!!.id!!
        val updateRequest = AccountCurrencyUpdateRequest(DEFAULT_CURRENCY_CODE)
        var json: String = objectWriter.writeValueAsString(updateRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.patch("/accounts/{id}/currencyCode", defaultAccountId)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isNoContent,
                MockMvcResultMatchers.content().string("")
            ).andReturn()
    }


    @Test
    @Ignore
    fun givenAccountNotForProfileWhenPatchCurrencyCodeThenNotFound() {
        assertTrue(false)
    }

    @Test
    fun givenAccountNotExistOrNotForUserWhenPatchCurrencyCodeThenNotFound() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!


        val nonExistentId = -1
        val newCode = "GBP"
        val updateRequest = AccountCurrencyUpdateRequest(newCode)
        var json: String = objectWriter.writeValueAsString(updateRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.patch("/accounts/{id}/currencyCode", nonExistentId)
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

}