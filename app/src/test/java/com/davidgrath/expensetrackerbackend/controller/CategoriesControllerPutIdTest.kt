package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.configuration.SecurityConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TestRunControllerConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.CategoryPojo
import com.davidgrath.expensetrackerbackend.generatedapi.api.CategoriesApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.CategoryUpdateRequest
import com.davidgrath.expensetrackerbackend.repository.CategoryRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.repository.UserRepository
import com.davidgrath.expensetrackerbackend.testutils.TestConstants
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.createBearerToken
import com.davidgrath.expensetrackerbackend.testutils.tempInit
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.JWT_ALG
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.TRUE_BYTE
import com.davidgrath.expensetrackerbackend.utils.bytesToUUID
import com.davidgrath.expensetrackerbackend.utils.toByte
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
@WebMvcTest(CategoriesController::class)
@ContextConfiguration(
    classes = [
        CategoriesController::class, SecurityConfiguration::class, TestRunControllerConfiguration::class, TestAuthController::class, ExceptionResponseHandler::class, TestUtilComponent::class
    ])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CategoriesControllerPutIdTest {


    @Autowired
    lateinit var mockMvc: MockMvc
    @Autowired
    lateinit var testUtilComponent: TestUtilComponent
    @Autowired
    lateinit var timeHandler: TimeHandler

    @Autowired
    lateinit var categoryRepository: CategoryRepository
    @Autowired
    lateinit var userRepository: UserRepository
    @Autowired
    lateinit var profileRepository: ProfileRepository

    @Value("\${jwt.signingkey}")
    private val jwtSigningKey: String? = null

    @Before
    fun setUp() {
        tempInit(testUtilComponent, timeHandler)
    }

    @After
    fun tearDown() {
        testUtilComponent.deleteAll()
    }

    val endpoint = CategoriesApi.PATH_CATEGORIES_ID_PUT
    val verb = HttpMethod.PUT

    //TODO IdorTest, Not found tests

    @Test
    fun categoryUpdateOkTest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val ordinalSortKeyOccupancy = categoryRepository.getMaxOrdinalSortKeyAndOccupancySlot(profile.id!!)
        val nextOrdinal = ((ordinalSortKeyOccupancy.ordinal?: 0) + 1).toShort()
        val nextSortKey = ((ordinalSortKeyOccupancy.sortKey?: 0) + 1_000).toShort()
        val nextOccupancy = ((ordinalSortKeyOccupancy.occupancySlot?: 0) + 1).toShort()
        val customCategoryId = categoryRepository.insertCategory(CategoryPojo(null, profile.id!!, null, true.toByte(), "Shopping",
            null, nextOrdinal, nextSortKey, nextOccupancy,LocalDateTime.now(timeHandler.getClock())))

        val objectWriter: ObjectWriter = ObjectMapper().writer()

        val updateRequest = CategoryUpdateRequest("New name", "materialsymbols:shopping_cart")
        var json: String = objectWriter.writeValueAsString(updateRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.request(verb, endpoint, customCategoryId)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk
            ).andReturn()
        //TODO Side effects
    }


    @Test
    fun givenNameIsBlankWhenUpdateCategoryThenBadRequest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val ordinalSortKeyOccupancy = categoryRepository.getMaxOrdinalSortKeyAndOccupancySlot(profile.id!!)
        val nextOrdinal = ((ordinalSortKeyOccupancy.ordinal?: 0) + 1).toShort()
        val nextSortKey = ((ordinalSortKeyOccupancy.sortKey?: 0) + 1_000).toShort()
        val nextOccupancy = ((ordinalSortKeyOccupancy.occupancySlot?: 0) + 1).toShort()
        val customCategoryId = categoryRepository.insertCategory(CategoryPojo(null, profile.id!!, null, true.toByte(), "Shopping",
            null, nextOrdinal, nextSortKey, nextOccupancy,LocalDateTime.now(timeHandler.getClock())))

        val objectWriter: ObjectWriter = ObjectMapper().writer()

        val updateRequest = CategoryUpdateRequest(" \t\n", "materialsymbols:shopping_cart")
        var json: String = objectWriter.writeValueAsString(updateRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.request(verb, endpoint, customCategoryId)
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
    fun givenIconNotInRecognizedSetWhenUpdateThenBadRequest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val ordinalSortKeyOccupancy = categoryRepository.getMaxOrdinalSortKeyAndOccupancySlot(profile.id!!)
        val nextOrdinal = ((ordinalSortKeyOccupancy.ordinal?: 0) + 1).toShort()
        val nextSortKey = ((ordinalSortKeyOccupancy.sortKey?: 0) + 1_000).toShort()
        val nextOccupancy = ((ordinalSortKeyOccupancy.occupancySlot?: 0) + 1).toShort()
        val customCategoryId = categoryRepository.insertCategory(CategoryPojo(null, profile.id!!, null, true.toByte(), "Shopping",
            "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category", nextOrdinal, nextSortKey, nextOccupancy,LocalDateTime.now(timeHandler.getClock()), ))

        val objectWriter: ObjectWriter = ObjectMapper().writer()

        val updateRequest = CategoryUpdateRequest("Shopping and groceries", "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:shopping_cart_x")
        var json: String = objectWriter.writeValueAsString(updateRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.request(verb, endpoint, customCategoryId)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isBadRequest,
                MockMvcResultMatchers.jsonPath(
                    "$.${Constants.CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE}", Matchers.equalTo(ErrorCode.CATEGORY_ICON_NAME_INCORRECT.code)
                )
            ).andReturn()
    }

    /**
     * I might remove this restriction later, but it stays for now
     */
    @Test
    fun givenCategoryIsDefaultWhenUpdateThenBadRequest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val categoryId = categoryRepository.findByProfileIdAndStringId(profile.id!!, "food")!!.id!!

        val objectWriter: ObjectWriter = ObjectMapper().writer()

        val updateRequest = CategoryUpdateRequest("Not Food", "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:shopping_cart")
        var json: String = objectWriter.writeValueAsString(updateRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.request(verb, endpoint, categoryId)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isBadRequest,
                MockMvcResultMatchers.jsonPath(
                    "$.${Constants.CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE}", Matchers.equalTo(ErrorCode.CATEGORY_NOT_MODIFIABLE.code)
                )
            ).andReturn()
    }



    //TODO Not found tests for update. Also new patch endpoints.
    //TODO Also also, I'm considering deleting the concept of a "core category" altogether, the main purposes I had in mind for those were i8n strings and receipt scanning inference, so take note and contemplate
    @Test
    fun givenIconFormatIncorrectWhenUpdateCategoryThenBadRequest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val ordinalSortKeyOccupancy = categoryRepository.getMaxOrdinalSortKeyAndOccupancySlot(profile.id!!)
        val nextOrdinal = ((ordinalSortKeyOccupancy.ordinal?: 0) + 1).toShort()
        val nextSortKey = ((ordinalSortKeyOccupancy.sortKey?: 0) + 1_000).toShort()
        val nextOccupancy = ((ordinalSortKeyOccupancy.occupancySlot?: 0) + 1).toShort()
        val customCategoryId = categoryRepository.insertCategory(CategoryPojo(null, profile.id!!, null, TRUE_BYTE, "Shopping",
            null, nextOrdinal, nextSortKey, nextOccupancy, LocalDateTime.now(timeHandler.getClock())))

        val objectWriter: ObjectWriter = ObjectMapper().writer()

        val updateRequest = CategoryUpdateRequest("new category", "shopping_cart")
        var json: String = objectWriter.writeValueAsString(updateRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.request(verb, endpoint, customCategoryId)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isBadRequest,
                MockMvcResultMatchers.jsonPath(
                    "$.${Constants.CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE}", Matchers.equalTo(ErrorCode.CATEGORY_ICON_FORMAT_INCORRECT.code)
                )
            ).andReturn()
    }

}