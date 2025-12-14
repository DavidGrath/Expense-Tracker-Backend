package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetracker.generatedapi.model.SignUpRequest
import com.davidgrath.expensetrackerbackend.configuration.SecurityConfiguration
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.repository.*
import com.davidgrath.expensetrackerbackend.service.TransactionService
import com.davidgrath.expensetrackerbackend.configuration.TestConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.UserPojo
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.testutils.deleteAll
import com.davidgrath.expensetrackerbackend.testutils.tempInit
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import io.jsonwebtoken.Jwts
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
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
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec


@ActiveProfiles("dev")
@RunWith(SpringRunner::class)
@WebMvcTest(AuthController::class)
@ContextConfiguration(
    classes = [
        AuthController::class, SecurityConfiguration::class, TestConfiguration::class, TestAuthController::class, ExceptionResponseHandler::class
    ])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
//@TestPropertySource(
//    locations = ["classpath:application-dev.properties", "classpath:secrets-dev.properties"])
class AuthControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc
    @Autowired
    lateinit var profileRepository: ProfileRepository
    @Autowired
    lateinit var transactionRepository: TransactionRepository

    @Autowired
    lateinit var categoryRepository: CategoryRepository
    @Autowired
    lateinit var accountRepository: AccountRepository
    @Autowired
    lateinit var itemRepository: TransactionItemRepository
    @Autowired
    lateinit var imageRepository: ImageRepository
    @Autowired
    lateinit var itemImagesRepository: ItemImagesRepository
    @Autowired
    lateinit var userRepository: UserRepository
    @Autowired
    lateinit var timeHandler: TimeHandler
    @Autowired
    lateinit var userService: UserService

    private val JWT_ALG = "HmacSHA256"

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
        deleteAll(itemImagesRepository, itemRepository, imageRepository, categoryRepository, transactionRepository, accountRepository, profileRepository, userRepository)
    }

    @Test
    fun successfulSignUpFlow() {
        val signUpRequest = SignUpRequest("username", "password", Locale.US.toLanguageTag());
        val objectWriter = ObjectMapper().writer();
        val signUpJson = objectWriter.writeValueAsString(signUpRequest);
        val signUpResult = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/auth/signUp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(signUpJson)
            )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
        assertTrue(signUpResult.getResponse().getContentAsString().startsWith("eyJ")) //pretty much constant for every JSON Base64
    }

    @Value("\${jwt.signingkey}")
    private val jwtSigningKey: String? = null
    @Test
    fun bearerTokenTest() {
        val key: SecretKey = SecretKeySpec(jwtSigningKey!!.toByteArray(), JWT_ALG)
        val localDateTime = LocalDateTime.now(timeHandler.getClock())
//        val expiry = localDateTime.plus(60, ChronoUnit.MINUTES)
        val jws = Jwts.builder()
            .subject("userx")
            .signWith(key)
            .issuedAt(Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()))
            .id(UUID.randomUUID().toString())
            .compact()
        val testResult = mockMvc
            .perform(
                MockMvcRequestBuilders.get("/test").header("Authorization", "Bearer $jws")
            )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn()
    }
    @Test
    fun givenTakenEmailWhenSignUpThenBadRequest() {
        userService.createUser("username", "password", Locale.US)
        val signUpRequest = SignUpRequest(
            "username",
            "password",
            "en-US"
        )
        val objectWriter: ObjectWriter = ObjectMapper().writer()
        val json: String = objectWriter.writeValueAsString(signUpRequest)
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/auth/signUp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isBadRequest,
                MockMvcResultMatchers.jsonPath(
                    "$.errorCode", Matchers.equalTo(ErrorCode.USERNAME_TAKEN.code)
                )
            )
    }

    @Test
    fun loginTest() {
        userService.createUser("username", "password", Locale.US)
        val encoder = Base64.getEncoder()
        val basic = encoder.encodeToString("username:password".toByteArray())
        val signUpResult = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/auth/login")
                    .header("Authorization", "Basic ${basic}")
            )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
        assertTrue(signUpResult.getResponse().getContentAsString().startsWith("eyJ"))
    }
    @Test
    fun loginTestFail() {
        userService.createUser("username", "password", Locale.US)
        val encoder = Base64.getEncoder()
        val basic = encoder.encodeToString("username:wordpass".toByteArray())
        val signUpResult = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/auth/login")
                    .header("Authorization", "Basic ${basic}")
            )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andReturn();
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AuthControllerTest::class.java)
    }
}