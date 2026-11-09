package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.configuration.SecurityConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TestRunControllerConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.CategoryPojo
import com.davidgrath.expensetrackerbackend.generatedapi.model.CategoryCreateRequest
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

class CategoriesControllerTest {

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

    @Test
    fun getAllOkTest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!


        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/categories")
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
            )
            .andExpectAll(
                MockMvcResultMatchers.status().is2xxSuccessful
            )
    }

    @Test
    fun givenCategoryCountIsMaxWhenCreateThenBadRequest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val objectWriter: ObjectWriter = ObjectMapper().writer()
        val existing = categoryRepository.getAllByProfileId(profile.id!!)
        val remaining = Constants.Constraints.MAX_CATEGORIES_PER_PROFILE - existing.size
        if(remaining <= 0) {
            throw RuntimeException("Existing categories too many")
        }
        for(cat in 0 until remaining) {
            val nextOrdinal = ((cat+1)+existing.size).toShort()
            val nextSortKey = (((cat+1)+existing.size)*1_000).toShort()
            val nextOccupancy = ((cat+1)+existing.size).toShort()
            categoryRepository.insertCategory(CategoryPojo(null, profile.id!!, null, TRUE_BYTE, "Custom",
                null, nextOrdinal, nextSortKey, nextOccupancy, LocalDateTime.now(timeHandler.getClock())))
        }
        val createRequest = CategoryCreateRequest("new category", "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category")
        var json: String = objectWriter.writeValueAsString(createRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/categories")
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
    fun givenIconFormatIncorrectWhenCreateCategoryThenBadRequest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val objectWriter: ObjectWriter = ObjectMapper().writer()

        val createRequest = CategoryCreateRequest("new category", "category")
        var json: String = objectWriter.writeValueAsString(createRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/categories")
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
                MockMvcRequestBuilders.put("/categories/{id}", customCategoryId)
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

    @Test
    fun givenIconNotInRecognizedSetWhenCreateThenBadRequest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val objectWriter: ObjectWriter = ObjectMapper().writer()

        val createRequest = CategoryCreateRequest("new category", "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category_x")
        var json: String = objectWriter.writeValueAsString(createRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/categories")
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
                MockMvcRequestBuilders.put("/categories/{id}", customCategoryId)
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
                MockMvcRequestBuilders.put("/categories/{id}", categoryId)
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

    /*
    //I could probably use a Regex in the OpenAPI spec so that this is handled automatically by bean validation, but I'm not sure how negative lookahead:
    (?!^\s+$)^.+$
    would translate back and forth. Also, I'm going to assume that PCRE isn't universal for now

     */
    @Test
    fun givenNameIsBlankWhenCreateCategoryThenBadRequest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val objectWriter: ObjectWriter = ObjectMapper().writer()

        val createRequest = CategoryCreateRequest("  \t\r", "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category")
        var json: String = objectWriter.writeValueAsString(createRequest)
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/categories")
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
                MockMvcRequestBuilders.put("/categories/{id}", customCategoryId)
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