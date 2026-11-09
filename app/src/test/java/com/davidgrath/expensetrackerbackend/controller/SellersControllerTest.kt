package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.configuration.SecurityConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TestRunControllerConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.SellerLocationPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.SellerPojo
import com.davidgrath.expensetrackerbackend.generatedapi.api.SellersApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.SellerCreateRequest
import com.davidgrath.expensetrackerbackend.generatedapi.model.SellerLocationCreateRequest
import com.davidgrath.expensetrackerbackend.generatedapi.model.SellerLocationUpdateRequest
import com.davidgrath.expensetrackerbackend.generatedapi.model.SellerUpdateRequest
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
class SellersControllerTest {

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

    @Test
    fun getAllLocationsOkTest() {
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val (sellerId, locationId) = seedSellers(profileId)

        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.get(SellersApi.PATH_SELLERS_ID_LOCATIONS_GET, sellerId)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!).toString())
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk,
                MockMvcResultMatchers.jsonPath(
                    "$.*", Matchers.iterableWithSize<Int>(Matchers.equalTo(1))
                )
            ).andReturn()
    }

    @Test
    fun getLocationByIdOkTest() {
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val (sellerId, locationId) = seedSellers(profileId)

        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.get(SellersApi.PATH_SELLERS_ID_LOCATIONS_LOCATION_ID_GET, sellerId, locationId)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!).toString())
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk
            ).andReturn()
    }

    @Test
    fun getSellerByIdOkTest() {
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        val (sellerId, locationId) = seedSellers(profileId)

        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.get(SellersApi.PATH_SELLERS_ID_GET, sellerId)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!).toString())
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk
            ).andReturn()
    }

    @Test
    fun getAllSellersOkTest() {
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!

        seedSellers(profileId)

        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.get(SellersApi.PATH_SELLERS_GET)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!).toString())
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk,
                MockMvcResultMatchers.jsonPath(
                    "$.*", Matchers.iterableWithSize<Int>(Matchers.equalTo(1))
                )
            ).andReturn()
    }


    @Test
    fun givenSellerCountIsMaxWhenAddSellerThenBadRequest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val existing = sellerRepository.getAllByProfileId(profile.id!!)
        val remaining = Constants.Constraints.MAX_SELLERS_PER_PROFILE - existing.size
        if(remaining <= 0) {
            throw RuntimeException("Existing sellers too many")
        }
        for(sel in 0 until remaining) {
            seedSellers(profile.id!!)
        }
        val createRequest = SellerCreateRequest("Supermarket")
        var json: String = objectWriter.writeValueAsString(createRequest)

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post(SellersApi.PATH_SELLERS_POST)
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
    fun givenLocationCountIsMaxWhenAddLocationThenBadRequest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val (sellerId, sellerOrdinal, locationOrdinal) = seedSellers(profile.id!!)
        val existing = sellerLocationRepository.getByProfileIdAndSellerId(profile.id!!, sellerId)
        val remaining = Constants.Constraints.MAX_LOCATIONS_PER_SELLER - existing.size
        if(remaining <= 0) {
            throw RuntimeException("Existing locations too many")
        }
        val createdAt = LocalDateTime.now(timeHandler.getClock())
        for(sel in 0 until remaining) {
            val sellerLocation = SellerLocationPojo(null, profile.id!!, sellerOrdinal, "Branch location", null, null, null,
                sel+1+existing.size, (sel+1+existing.size) * 1_000, sel+1+existing.size, createdAt)
            val sellerLocationId = sellerLocationRepository.insertSellerLocation(sellerLocation)
        }
        val createRequest = SellerLocationCreateRequest("Supermarket branch")
        var json: String = objectWriter.writeValueAsString(createRequest)

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post(SellersApi.PATH_SELLERS_ID_LOCATIONS_POST, sellerId)
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
    fun givenNameBlankWhenCreateSellerThenBadRequest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

//        val (sellerId, locationId) = seedSellers(profile.id!!)


        val createRequest = SellerCreateRequest(" \t\n")
        var json: String = objectWriter.writeValueAsString(createRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post(SellersApi.PATH_SELLERS_POST)
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

    @Test
    fun givenNameBlankWhenUpdateSellerThenBadRequest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val (sellerId, locationId) = seedSellers(profile.id!!)


        val updateRequest = SellerUpdateRequest(" \t\n")
        var json: String = objectWriter.writeValueAsString(updateRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.put(SellersApi.PATH_SELLERS_ID_PUT, sellerId)
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

    @Test
    fun givenNameBlankWhenCreateLocationThenBadRequest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val (sellerId, locationId) = seedSellers(profile.id!!)


        val createRequest = SellerLocationCreateRequest(" \t\n")
        var json: String = objectWriter.writeValueAsString(createRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post(SellersApi.PATH_SELLERS_ID_LOCATIONS_POST, sellerId)
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

    @Test
    fun givenNameBlankWhenUpdateLocationThenBadRequest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val (sellerId, locationId) = seedSellers(profile.id!!)


        val updateRequest = SellerLocationUpdateRequest(" \t\n")
        var json: String = objectWriter.writeValueAsString(updateRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.put(SellersApi.PATH_SELLERS_ID_LOCATIONS_LOCATION_ID_PUT, sellerId, locationId)
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

    /**
     * @return the seller ID, seller ordinal, and location ordinal
     */
    private fun seedSellers(profileId: Long): Triple<Long, Int, Int> {
        val createdAt = LocalDateTime.now(timeHandler.getClock())
        val sellerOrdinal = 1
        val seller = SellerPojo(null, profileId, "Restaurant", sellerOrdinal, 1_000, 1, createdAt)
        val sellerId = sellerRepository.insertSeller(seller)
        val locationOrdinal = 1
        val sellerLocation = SellerLocationPojo(null, profileId, sellerOrdinal, "Branch location", null, null, null, locationOrdinal, 1_000, 1, createdAt)
        val sellerLocationId = sellerLocationRepository.insertSellerLocation(sellerLocation)
        return Triple(sellerId, sellerOrdinal, locationOrdinal)
    }
}