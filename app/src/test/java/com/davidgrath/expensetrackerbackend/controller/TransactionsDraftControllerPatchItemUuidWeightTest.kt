package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.configuration.SecurityConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TestRunControllerConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.configuration.WebMvcConfiguration
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
import com.davidgrath.expensetrackerbackend.generatedapi.api.TransactionsApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionDraftItemPatchWeightRequest
import com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionDraftItemUpdateRequest
import com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionDraftItemUpdateRequestWeight
import com.davidgrath.expensetrackerbackend.generatedapi.model.UnitPrefix
import com.davidgrath.expensetrackerbackend.generatedapi.model.WeightUnit
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.CategoryRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.repository.SellerRepository
import com.davidgrath.expensetrackerbackend.repository.UserRepository
import com.davidgrath.expensetrackerbackend.service.TransactionDraftService
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.testutils.TestConstants
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.createBearerToken
import com.davidgrath.expensetrackerbackend.testutils.getDefaultAccountId
import com.davidgrath.expensetrackerbackend.testutils.tempInit
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.JWT_ALG
import com.davidgrath.expensetrackerbackend.utils.uuidToBytes
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
import java.util.UUID

@RunWith(SpringRunner::class)
@WebMvcTest(TransactionsController::class)
@ContextConfiguration(
    classes = [
        TransactionsController::class, SecurityConfiguration::class, TestRunControllerConfiguration::class, ExceptionResponseHandler::class,
        TestUtilComponent::class, WebMvcConfiguration::class
    ])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class TransactionsDraftControllerPatchItemUuidWeightTest {

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
    @Autowired
    lateinit var transactionDraftService: TransactionDraftService

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
    val endpoint = TransactionsApi.PATH_TRANSACTIONS_DRAFT_ITEMS_UUID_WEIGHT_PATCH
    val verb = HttpMethod.PATCH

    //TODO 204

    @Test
    fun putItemWeightWithMetricOkTest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)))!!
        val profile = profileRepository.findByUuid(user.id!!, uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID)))!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profile.id!!, "miscellaneous")

        transactionDraftService.createDraft(profile.id!!, accountId)
        val draft = transactionDraftService.getDraft(profile.id!!)
        val item = draft!!.items[0]
        val uuid = item.uuid

        val weightRequest = TransactionDraftItemPatchWeightRequest(BigDecimal.ONE, WeightUnit.G, UnitPrefix.K)

        val objectWriter: ObjectWriter = ObjectMapper().writer()
        val json = objectWriter.writeValueAsString(weightRequest)


        mockMvc
            .perform(
                MockMvcRequestBuilders.request(verb, endpoint, uuid)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", Constants.DEFAULT_PROFILE_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk,
                MockMvcResultMatchers.jsonPath("$.items[0].weight.amount", Matchers.equalTo(1)),
                MockMvcResultMatchers.jsonPath("$.items[0].weight.unit", Matchers.equalTo("g")),
                MockMvcResultMatchers.jsonPath("$.items[0].weight.prefix", Matchers.equalTo("k")),
            ).andReturn()
    }

    @Test
    fun putItemWeightWithNonMetricOkTest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)))!!
        val profile = profileRepository.findByUuid(user.id!!, uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID)))!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profile.id!!, "miscellaneous")

        transactionDraftService.createDraft(profile.id!!, accountId)
        val draft = transactionDraftService.getDraft(profile.id!!)
        val item = draft!!.items[0]
        val uuid = item.uuid

        val nonMetricUnit = WeightUnit._LB_AV_
        val weightRequest = TransactionDraftItemPatchWeightRequest(BigDecimal.ONE, nonMetricUnit, UnitPrefix.K)

        val objectWriter: ObjectWriter = ObjectMapper().writer()
        val json = objectWriter.writeValueAsString(weightRequest)


        mockMvc
            .perform(
                MockMvcRequestBuilders.request(verb, endpoint, uuid)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", Constants.DEFAULT_PROFILE_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk,
                MockMvcResultMatchers.jsonPath("$.items[0].weight.amount", Matchers.equalTo(1)),
                MockMvcResultMatchers.jsonPath("$.items[0].weight.unit", Matchers.equalTo("[lb_av]")),
                MockMvcResultMatchers.jsonPath("$.items[0].weight.prefix").value(Matchers.nullValue())
            ).andReturn()
    }


    @Test
    fun givenItemDoesNotExistWhenPatchWeightThenNotFound() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)))!!
        val profile = profileRepository.findByUuid(user.id!!, uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID)))!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val misc = categoryRepository.findByProfileIdAndStringId(profile.id!!, "miscellaneous")

        transactionDraftService.createDraft(profile.id!!, accountId)
        val draft = transactionDraftService.getDraft(profile.id!!)
        val item = draft!!.items[0]
        val uuid = item.uuid
        val nonExistentUuid = UUID(0L, 0L)
        println("UUID: $nonExistentUuid")

        val weightRequest = TransactionDraftItemPatchWeightRequest(BigDecimal.ONE, WeightUnit.G, UnitPrefix.K)

        val objectWriter: ObjectWriter = ObjectMapper().writer()
        val json = objectWriter.writeValueAsString(weightRequest)


        mockMvc
            .perform(
                MockMvcRequestBuilders.request(verb, endpoint, nonExistentUuid)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", Constants.DEFAULT_PROFILE_ID)
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

