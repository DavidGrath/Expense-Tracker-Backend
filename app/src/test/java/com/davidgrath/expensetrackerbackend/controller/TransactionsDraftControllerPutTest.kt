package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.configuration.SecurityConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TestRunControllerConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.configuration.WebMvcConfiguration
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
import com.davidgrath.expensetrackerbackend.entities.UnitPrefixEnum
import com.davidgrath.expensetrackerbackend.generatedapi.api.TransactionsApi
import com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionDraftItemUpdateRequest
import com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionDraftItemUpdateRequestVolume
import com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionDraftItemUpdateRequestWeight
import com.davidgrath.expensetrackerbackend.generatedapi.model.UnitPrefix
import com.davidgrath.expensetrackerbackend.generatedapi.model.VolumeUnit
import com.davidgrath.expensetrackerbackend.generatedapi.model.WeightUnit
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.CategoryRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
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
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigDecimal

@RunWith(SpringRunner::class)
@WebMvcTest(TransactionsController::class)
@ContextConfiguration(
    classes = [
        TransactionsController::class, SecurityConfiguration::class, TestRunControllerConfiguration::class, ExceptionResponseHandler::class,
        TestUtilComponent::class, WebMvcConfiguration::class
    ])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Ignore("Not ready yet")
class TransactionsDraftControllerPutTest {

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
    //TODO Ok test, seller, sellerLocation, and account IDOR/404, non metric weight, non metric volume

    val endpoint = TransactionsApi.PATH_TRANSACTIONS_DRAFT_PUT
    val verb = HttpMethod.PUT

    @Test
    fun putDraftItemUuidOkTest() {
        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val food = categoryRepository.findByProfileIdAndStringId(profile.id!!, "food")
        transactionDraftService.createDraft(profile!!.id!!, accountId)


        val draft = transactionDraftService.getDraft(profile.id!!)
        val item = draft!!.items[0]
        val uuid = item.uuid

        val itemUpdateRequest = TransactionDraftItemUpdateRequest(
            BigDecimal.TEN, "Yoghurt", food!!.id!!,
            false, 4, "Generic Brand", "Chocolate",
            TransactionDraftItemUpdateRequestWeight(BigDecimal(2), WeightUnit.G, UnitPrefix.K),
            TransactionDraftItemUpdateRequestVolume(BigDecimal(2_000), VolumeUnit.L, UnitPrefix.M),
        )

        val objectWriter: ObjectWriter = ObjectMapper().writer()
        val json = objectWriter.writeValueAsString(itemUpdateRequest)

        mockMvc
            .perform(
                MockMvcRequestBuilders.request(verb, endpoint, uuid)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", bytesToUUID(profile.uuid!!))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk,
                MockMvcResultMatchers.jsonPath("$.items[0].amount", Matchers.equalTo<Int>(10)),
                MockMvcResultMatchers.jsonPath("$.items[0].description", Matchers.equalTo<String>("Yoghurt")),
                MockMvcResultMatchers.jsonPath("$.items[0].categoryId", Matchers.equalTo<Int>(food.id!!.toInt())),
                MockMvcResultMatchers.jsonPath("$.items[0].isReduction", Matchers.equalTo(false)),
                MockMvcResultMatchers.jsonPath("$.items[0].quantity", Matchers.equalTo<Int>(4)),
                MockMvcResultMatchers.jsonPath("$.items[0].brand", Matchers.equalTo("Generic Brand")),
                MockMvcResultMatchers.jsonPath("$.items[0].variation", Matchers.equalTo("Chocolate")),
                MockMvcResultMatchers.jsonPath("$.items[0].weight.amount", Matchers.equalTo(2)),
                MockMvcResultMatchers.jsonPath("$.items[0].weight.unit", Matchers.equalTo("g")),
                MockMvcResultMatchers.jsonPath("$.items[0].weight.prefix", Matchers.equalTo("k")),
                MockMvcResultMatchers.jsonPath("$.items[0].volume.amount", Matchers.equalTo(2_000)),
                MockMvcResultMatchers.jsonPath("$.items[0].volume.unit", Matchers.equalTo("l")),
                MockMvcResultMatchers.jsonPath("$.items[0].volume.prefix", Matchers.equalTo("m")),
            ).andReturn()

    }
}