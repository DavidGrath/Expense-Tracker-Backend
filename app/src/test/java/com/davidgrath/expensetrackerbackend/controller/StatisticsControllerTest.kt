package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.configuration.SecurityConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TestRunControllerConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.configuration.WebMvcConfiguration
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
import com.davidgrath.expensetrackerbackend.generatedapi.api.SellersApi
import com.davidgrath.expensetrackerbackend.generatedapi.api.StatisticsApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.SellerLocationUpdateRequest
import com.davidgrath.expensetrackerbackend.generatedapi.model.StatisticsFilter
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.repository.SellerLocationRepository
import com.davidgrath.expensetrackerbackend.repository.SellerRepository
import com.davidgrath.expensetrackerbackend.repository.UserRepository
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
@WebMvcTest(StatisticsController::class)
@ContextConfiguration(
    classes = [
        StatisticsController::class, SecurityConfiguration::class, TestRunControllerConfiguration::class, ExceptionResponseHandler::class,
        TestUtilComponent::class, WebMvcConfiguration::class
    ])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class StatisticsControllerTest {

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

    val objectWriter: ObjectWriter = ObjectMapper().writer()

    @Before
    fun setUp() {
        tempInit(testUtilComponent, timeHandler)
    }

    @After
    fun tearDown() {
        testUtilComponent.deleteAll()
    }

    /**
     * The precise details for what forms an "invalid" StatisticsFilter are covered in [com.davidgrath.expensetrackerbackend.service.StatisticsServiceTest],
     * so I'll use a simple trivial case here to assert the controller does its job of mapping correctly
     */
    @Test
    fun givenFilterIsInvalidWhenStatisticsEndpointThenBadRequest() {

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val invalidAccountList = emptyList<Long>()
        val invalidFilter = StatisticsFilter().accounts(invalidAccountList)
        var json: String = objectWriter.writeValueAsString(invalidFilter)

        val endpoints = listOf(
            StatisticsApi.PATH_STATISTICS_TRANSACTIONS_POST,
            StatisticsApi.PATH_STATISTICS_TRANSACTION_AND_ITEM_COUNT_POST,

            StatisticsApi.PATH_STATISTICS_TOTAL_EXPENSES_BY_CATEGORY_POST,
            StatisticsApi.PATH_STATISTICS_TOTAL_INCOME_BY_CATEGORY_POST,
            StatisticsApi.PATH_STATISTICS_TOTAL_EXPENSES_POST,
            StatisticsApi.PATH_STATISTICS_TOTAL_INCOME_POST,
            StatisticsApi.PATH_STATISTICS_TOTAL_EXPENSES_BY_DATE_POST,
            StatisticsApi.PATH_STATISTICS_TOTAL_INCOME_BY_DATE_POST,
            StatisticsApi.PATH_STATISTICS_TOP_N_EXPENSES_PER_CATEGORY_POST,
            StatisticsApi.PATH_STATISTICS_TOP_N_INCOME_PER_CATEGORY_POST,
            StatisticsApi.PATH_STATISTICS_CUM_SUM_EXPENSES_BY_DATE_POST,
            StatisticsApi.PATH_STATISTICS_CUM_SUM_INCOME_BY_DATE_POST,
        )
        for(endpoint in endpoints) {

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post(endpoint)
                        .header("Authorization", "Bearer ${jws}")
                        .header("Profile-Id", bytesToUUID(profile.uuid!!).toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                )
                .andExpectAll(
                    MockMvcResultMatchers.status().isBadRequest,
                    MockMvcResultMatchers.jsonPath(
                        "$.${Constants.CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE}", Matchers.equalTo(ErrorCode.INVALID_STATISTICS_FILTER.code)
                    )
                ).andReturn()
        }
    }

    @Test
    @Ignore("Not ready yet")
    fun givenCurrencyCodeIsInvalidWhenStatisticsEndpointThenBadRequest() {
        //TODO new code: invalid_currency_code
        //TODO Hibernate validations
    }
}