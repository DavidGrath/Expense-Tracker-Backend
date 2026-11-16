package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.configuration.SecurityConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TestRunControllerConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.SellerLocationPojo
import com.davidgrath.expensetrackerbackend.generatedapi.api.SellersApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.SellerLocationCreateRequest
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.repository.SellerLocationRepository
import com.davidgrath.expensetrackerbackend.repository.SellerRepository
import com.davidgrath.expensetrackerbackend.repository.UserRepository
import com.davidgrath.expensetrackerbackend.testutils.TestConstants
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.createBearerToken
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
import java.time.LocalDateTime

@RunWith(SpringRunner::class)
@WebMvcTest(SellersController::class)
@ContextConfiguration(
    classes = [
        SellersController::class, SecurityConfiguration::class, TestRunControllerConfiguration::class, ExceptionResponseHandler::class
    ])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class SellersControllerPostIdLocationsTest {


    @Autowired
    lateinit var mockMvc: MockMvc
    @Autowired
    lateinit var testUtilComponent: TestUtilComponent
    @Autowired
    lateinit var sellerRepository: SellerRepository
    @Autowired
    lateinit var sellerLocationRepository: SellerLocationRepository
    @Autowired
    lateinit var userRepository: UserRepository
    @Autowired
    lateinit var profileRepository: ProfileRepository

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

    val endpoint = SellersApi.PATH_SELLERS_ID_LOCATIONS_POST
    val verb = HttpMethod.POST

    //TODO Ok test, IDOR test, not found test

    @Test
    fun givenLocationCountIsMaxWhenAddLocationThenBadRequest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val (sellerId, sellerOrdinal, locationId) = testUtilComponent.seedSellers(profile.id!!)
        val existing = sellerLocationRepository.getByProfileIdAndSellerId(profile.id!!, sellerId)
        val remaining = Constants.Constraints.MAX_LOCATIONS_PER_SELLER - existing.size
        if(remaining <= 0) {
            throw RuntimeException("Existing locations too many")
        }
        val createdAt = LocalDateTime.now(timeHandler.getClock())
        for(sel in 0 until remaining) {
            val ordinal = (sel+1+existing.size).toShort()
//            val sortKey = ((sel+1+existing.size) * 1_000).toShort() //TODO And I caught a 16-bit wraparound error ((50 * 1_000) > 32_767). I knew I'd have to tackle this eventually, but for now I'll change the increment to 500. In short, the main test case is if Short.MAX_VALUE - maxSortKey < SORT_KEY_INTERVAL, other edge cases include a difference of 1, a difference of 0, and if it's already taken. In general, rebalancing will be fun
            val sortKey = ((sel+1+existing.size) * 500).toShort()
            val occupancySlot = (sel+1+existing.size).toShort()
            val sellerLocation = SellerLocationPojo(null, profile.id!!, sellerOrdinal, "Branch location", null, null, null,
                ordinal, sortKey, occupancySlot, createdAt)
            val sellerLocationId = sellerLocationRepository.insertSellerLocation(sellerLocation)
        }
        val createRequest = SellerLocationCreateRequest("Supermarket branch")
        var json: String = objectWriter.writeValueAsString(createRequest)

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.request(verb, endpoint, sellerId)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!).toString())
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
    //TODO Expand API to include coordinates and address
    fun givenNameBlankWhenCreateLocationThenBadRequest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val (sellerId, sellerOrdinal, locationId) = testUtilComponent.seedSellers(profile.id!!)


        val createRequest = SellerLocationCreateRequest(" \t\n")
        var json: String = objectWriter.writeValueAsString(createRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.request(verb, endpoint, sellerId)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!).toString())
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