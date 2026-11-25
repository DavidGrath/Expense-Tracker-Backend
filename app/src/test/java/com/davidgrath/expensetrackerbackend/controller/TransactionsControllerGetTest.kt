package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.configuration.SecurityConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TestRunControllerConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.configuration.WebMvcConfiguration
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
import com.davidgrath.expensetrackerbackend.generatedapi.api.TransactionsApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.BasicTransactionCreateRequest
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.CategoryRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.repository.UserRepository
import com.davidgrath.expensetrackerbackend.service.TransactionService
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.testutils.TestConstants
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.createBearerToken
import com.davidgrath.expensetrackerbackend.testutils.getDefaultAccountId
import com.davidgrath.expensetrackerbackend.testutils.tempInit
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.JWT_ALG
import com.davidgrath.expensetrackerbackend.utils.bytesToUUID
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
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
import java.math.BigDecimal
import java.time.ZoneId

@RunWith(SpringRunner::class)
@WebMvcTest(TransactionsController::class)
@ContextConfiguration(
    classes = [
        TransactionsController::class, SecurityConfiguration::class, TestRunControllerConfiguration::class, ExceptionResponseHandler::class,
        TestUtilComponent::class, WebMvcConfiguration::class
    ])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class TransactionsControllerGetTest {

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
    lateinit var transactionService: TransactionService
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

    val endpoint = TransactionsApi.PATH_TRANSACTIONS_GET
    val verb = HttpMethod.GET

    @Test
    fun transactionGetOkTest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profile.id!!, "miscellaneous")

        transactionService.addTransaction(profile.id!!, accountId, true, BigDecimal.TEN, "Description", misc!!.id!!, ZoneId.of("UTC"))

        val accountListString = listOf(accountId).joinToString()
        mockMvc
            .perform(
                MockMvcRequestBuilders.request(verb, endpoint)
                    .queryParam("accountIds", accountListString)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!).toString())
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk,
                MockMvcResultMatchers.jsonPath("$[0].amount", Matchers.equalTo(10.0)),
                MockMvcResultMatchers.jsonPath("$[0].items[0].amount", Matchers.equalTo(10.0)),
                MockMvcResultMatchers.jsonPath("$[0].items[0].description", Matchers.equalTo("Description"))
            ).andReturn()
    }
}