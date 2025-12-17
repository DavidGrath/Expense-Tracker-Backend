package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetracker.generatedapi.model.BasicTransactionCreateRequest
import com.davidgrath.expensetrackerbackend.configuration.SecurityConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TestConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.CategoryRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.repository.UserRepository
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.createBearerToken
import com.davidgrath.expensetrackerbackend.testutils.getDefaultAccountId
import com.davidgrath.expensetrackerbackend.testutils.tempInit
import com.davidgrath.expensetrackerbackend.utils.Constants
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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigDecimal
import java.util.Base64
import java.util.Locale

@ActiveProfiles("dev")
@RunWith(SpringRunner::class)
@WebMvcTest(TransactionsController::class)
@ContextConfiguration(
    classes = [
        TransactionsController::class, SecurityConfiguration::class, TestConfiguration::class, ExceptionResponseHandler::class, TestUtilComponent::class
    ])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
//@TestPropertySource(
//    locations = ["classpath:application-dev.properties", "classpath:secrets-dev.properties"])
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
//        userService.createUser("username", "password", Locale.US)
        tempInit(testUtilComponent, timeHandler)
    }

    @After
    fun tearDown() {
        testUtilComponent.deleteAll()
    }

    @Test
    fun basicTransactionValidationTest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(Constants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profile.id!!, "miscellaneous")
        val objectWriter: ObjectWriter = ObjectMapper().writer()

        var basicTransactionRequest = BasicTransactionCreateRequest(accountId, true, BigDecimal.TEN, null, misc!!.id)
        var json: String = objectWriter.writeValueAsString(basicTransactionRequest)
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

        basicTransactionRequest = BasicTransactionCreateRequest(accountId, true, BigDecimal.ZERO, "Description", misc!!.id)
        json = objectWriter.writeValueAsString(basicTransactionRequest)
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
    fun givenAccountIdExistsButAccountIdBelongsToAnotherUserWhenCreateTransactionThenNotFound() {

        val user = userRepository.findByUuid(Constants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val userId = userService.createUser("user2", "password", Locale.US)
        val profile2 = profileRepository.findByStringId(userId, Constants.DEFAULT_PROFILE_ID)!!
        val accounts = accountRepository.getAllByProfileId(profile2.id!!)
        val misc = categoryRepository.findByProfileIdAndStringId(profile.id!!, "miscellaneous") //This should cause a failure
        var basicTransactionRequest = BasicTransactionCreateRequest(accounts[0].id!!, true, BigDecimal.TEN, "Description", misc!!.id)

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
                    "$.errorCode", Matchers.equalTo(ErrorCode.ENTITY_NOT_FOUND.code)
                )
            )
    }

    @Test
    @Ignore("Not ready yet")
    fun givenAccountIdExistsButAccountIdBelongsToAnotherProfileWhenCreateTransactionThenBadRequest() {
        assertTrue(false)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionsControllerTest::class.java)
    }
}