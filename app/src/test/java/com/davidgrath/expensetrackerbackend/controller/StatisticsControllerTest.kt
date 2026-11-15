package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.configuration.SecurityConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TestRunControllerConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.configuration.WebMvcConfiguration
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.AccountPojo
import com.davidgrath.expensetrackerbackend.generatedapi.api.StatisticsApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.StatisticsFilter
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.CategoryRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.repository.SellerLocationRepository
import com.davidgrath.expensetrackerbackend.repository.SellerRepository
import com.davidgrath.expensetrackerbackend.repository.UserRepository
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.testutils.DataBuilder
import com.davidgrath.expensetrackerbackend.testutils.TestConstants
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.createBearerToken
import com.davidgrath.expensetrackerbackend.testutils.getDefaultAccountId
import com.davidgrath.expensetrackerbackend.testutils.tempInit
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.FALSE_BYTE
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
import java.math.BigDecimal
import java.nio.file.Files
import java.time.LocalDateTime
import java.util.Locale

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
    @Autowired
    lateinit var userService: UserService
    @Autowired
    lateinit var categoryRepository: CategoryRepository
    @Autowired
    lateinit var sellerRepository: SellerRepository
    @Autowired
    lateinit var sellerLocationRepository: SellerLocationRepository

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

    //TODO I'm considering creating a db entity to represent a StatisticsFilter. Though that's a lotta work. Later
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
        val newAccountId = accountRepository.insertAccount(
            AccountPojo(null, profile.id!!, "GBP", null, null, "British", FALSE_BYTE, 2, 2_000, 2, LocalDateTime.now(timeHandler.getClock()))
        )
        val heterogeneousCurrencyAccountIds = listOf(accountId, newAccountId)
        val invalidAccountList = heterogeneousCurrencyAccountIds
        val invalidFilter = StatisticsFilter().accounts(invalidAccountList)
        var json: String = objectWriter.writeValueAsString(invalidFilter)


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
    fun givenFilterAccountDoesNotBelongToUserWhenStatisticsEndpointThenBadRequest() {
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!


        val user2Id = userService.createUser("user2", "password", Locale.US)
        val profile2 = profileRepository.findByUuid(user2Id, Constants.DEFAULT_PROFILE_ID)!!
        val accounts2 = accountRepository.getAllByProfileId(profile2.id!!)
        val misc2 = categoryRepository.findByProfileIdAndStringId(profile2.id!!, "miscellaneous")

        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user2AccountId = accounts2.first().id!!
        val invalidAccountList = listOf(user2AccountId)
        val invalidFilter = StatisticsFilter().accounts(invalidAccountList)
        var json: String = objectWriter.writeValueAsString(invalidFilter)

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
                    MockMvcResultMatchers.status().isNotFound,
                    MockMvcResultMatchers.jsonPath(
                        "$.${Constants.CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE}", Matchers.equalTo(ErrorCode.ENTITY_NOT_FOUND.code)
                    )
                ).andReturn()
        }
    }

    @Test
    fun givenFilterSellerDoesNotBelongToUserWhenStatisticsEndpointThenBadRequest() {
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val user2Id = userService.createUser("user2", "password", Locale.US)
        val user2Uuid = bytesToUUID(userRepository.findById(user2Id)!!.uuid!!)
        val profile2 = profileRepository.findByUuid(user2Id, Constants.DEFAULT_PROFILE_ID)!!
        val accounts = accountRepository.getAllByProfileId(profile.id!!)
        val misc2 = categoryRepository.findByProfileIdAndStringId(profile2.id!!, "miscellaneous")

        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()
        val user1DataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)
        user1DataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(100))
            .withNewSeller("Restaurant")
            .commit()

        val user2DataBuilder = DataBuilder(folder, timeHandler, user2Uuid.toString(), Constants.DEFAULT_PROFILE_ID, testUtilComponent)
        user2DataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(100))
            .withNewSeller("Hardware store")
            .commit()

        val user1Seller = sellerRepository.getAllByProfileId(profile.id!!).first()!!.id!!
        val user2Seller = sellerRepository.getAllByProfileId(profile2.id!!).first()!!.id!!
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val validAccountList = listOf(accountId)
        val sellers = listOf(user1Seller, user2Seller)
        val invalidFilter = StatisticsFilter().accounts(validAccountList).sellers(sellers)
        var json: String = objectWriter.writeValueAsString(invalidFilter)

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
                    MockMvcResultMatchers.status().isNotFound,
                    MockMvcResultMatchers.jsonPath(
                        "$.${Constants.CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE}", Matchers.equalTo(ErrorCode.ENTITY_NOT_FOUND.code)
                    )
                ).andReturn()
        }
    }

    @Test
    fun givenFilterSellerLocationDoesNotBelongToUserWhenStatisticsEndpointThenBadRequest() {
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val user2Id = userService.createUser("user2", "password", Locale.US)
        val user2Uuid = bytesToUUID(userRepository.findById(user2Id)!!.uuid!!)
        val profile2 = profileRepository.findByUuid(user2Id, Constants.DEFAULT_PROFILE_ID)!!
        val accounts = accountRepository.getAllByProfileId(profile.id!!)
        val misc2 = categoryRepository.findByProfileIdAndStringId(profile2.id!!, "miscellaneous")

        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()
        val user1DataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)
        user1DataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(100))
            .withNewSeller("Restaurant")
            .withNewSellerLocation("Branch in the city")
            .commit()

        val user2DataBuilder = DataBuilder(folder, timeHandler, user2Uuid.toString(), Constants.DEFAULT_PROFILE_ID, testUtilComponent)
        user2DataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(100))
            .withNewSeller("Hardware store")
            .withNewSellerLocation("Branch in the city")
            .commit()

        val user1Seller = sellerRepository.getAllByProfileId(profile.id!!).first()!!.id!!
        val user2Seller = sellerRepository.getAllByProfileId(profile2.id!!).first()!!.id!!
        val user1SellerFirstLocation = sellerLocationRepository.getByProfileIdAndSellerId(profile.id!!, user1Seller).first()!!.id!!
        val user2SellerFirstLocation = sellerLocationRepository.getByProfileIdAndSellerId(profile2.id!!, user2Seller).first()!!.id!!
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val validAccountList = listOf(accountId)
        val invalidLocationSellers = listOf(user1SellerFirstLocation, user2SellerFirstLocation)
        val invalidFilter = StatisticsFilter().accounts(validAccountList).sellers(invalidLocationSellers)
        var json: String = objectWriter.writeValueAsString(invalidFilter)

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
                    MockMvcResultMatchers.status().isNotFound,
                    MockMvcResultMatchers.jsonPath(
                        "$.${Constants.CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE}", Matchers.equalTo(ErrorCode.ENTITY_NOT_FOUND.code)
                    )
                ).andReturn()
        }
    }

    @Test
    fun givenFilterCategoryDoesNotBelongToUserWhenStatisticsEndpointThenBadRequest() {
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!


        val user2Id = userService.createUser("user2", "password", Locale.US)
        val profile2 = profileRepository.findByUuid(user2Id, Constants.DEFAULT_PROFILE_ID)!!
        val accounts2 = accountRepository.getAllByProfileId(profile2.id!!)
        val misc2 = categoryRepository.findByProfileIdAndStringId(profile2.id!!, "miscellaneous")

        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val validAccountList = listOf(accountId)
        val invalidCategories = listOf(misc2!!.id!!)
        val invalidFilter = StatisticsFilter().accounts(validAccountList).categories(invalidCategories)
        var json: String = objectWriter.writeValueAsString(invalidFilter)

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
                    MockMvcResultMatchers.status().isNotFound,
                    MockMvcResultMatchers.jsonPath(
                        "$.${Constants.CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE}", Matchers.equalTo(ErrorCode.ENTITY_NOT_FOUND.code)
                    )
                ).andReturn()
        }
    }



    @Test
    @Ignore("Not ready yet")
    fun givenCurrencyCodeIsInvalidWhenStatisticsEndpointThenBadRequest() {
        //TODO Hibernate validations
    }
}