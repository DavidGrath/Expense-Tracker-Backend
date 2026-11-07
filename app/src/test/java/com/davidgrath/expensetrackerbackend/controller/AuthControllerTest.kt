package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.configuration.SecurityConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TestRunControllerConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
import com.davidgrath.expensetrackerbackend.generatedapi.model.SignUpRequest
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.createBearerToken
import com.davidgrath.expensetrackerbackend.testutils.createRefreshToken
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.JWT_ALG
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
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
import java.util.*


@RunWith(SpringRunner::class)
@WebMvcTest(AuthController::class)
@ContextConfiguration(
    classes = [
        AuthController::class, SecurityConfiguration::class, TestRunControllerConfiguration::class, TestAuthController::class, ExceptionResponseHandler::class, TestUtilComponent::class, //TokenTypeFilter::class
    ])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AuthControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc
    @Autowired
    lateinit var testUtilComponent: TestUtilComponent
    @Autowired
    lateinit var timeHandler: TimeHandler
    @Autowired
    lateinit var userService: UserService
    @Value("\${jwt.signingkey}")
    private val jwtSigningKey: String? = null

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
        testUtilComponent.deleteAll()
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
            .andExpectAll(
                MockMvcResultMatchers.status().isOk(),
                MockMvcResultMatchers.jsonPath("$.access_token", Matchers.startsWith("eyJ"))
            )
            .andReturn();
//        assertTrue(signUpResult.getResponse().getContentAsString().startsWith("eyJ")) //pretty much constant for every JSON Base64
    }


    @Test
    fun bearerTokenTest() {
        val jws = createBearerToken("userx", jwtSigningKey!!, JWT_ALG, timeHandler)
        val testResult = mockMvc
            .perform(
                MockMvcRequestBuilders.get("/test").header("Authorization", "Bearer $jws")
            )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn()
    }

    @Test
    fun basicAuthTest() {
        userService.createUser("username", "password", Locale.US)
        val jws = createBearerToken("userx", jwtSigningKey!!, JWT_ALG, timeHandler)
        var testResult = mockMvc
            .perform(
                MockMvcRequestBuilders.get("/test").header("Authorization", "Bearer $jws")
            )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn()

        val encoder = Base64.getEncoder()
        val basic = encoder.encodeToString("username:password".toByteArray())

        testResult = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/auth/login").header("Authorization", "Basic $basic")
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
                    "$.$CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE", Matchers.equalTo(ErrorCode.USERNAME_TAKEN.code)
                )
            )
    }

    @Test
    fun loginTest() {
        userService.createUser("username", "password", Locale.US)
        val encoder = Base64.getEncoder()
        val basic = encoder.encodeToString("username:password".toByteArray())
        val loginResult = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/auth/login")
                    .header("Authorization", "Basic ${basic}")
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk(),
                MockMvcResultMatchers.jsonPath("$.access_token", Matchers.startsWith("eyJ"))
            )
            .andReturn();
//        assertTrue(signUpResult.getResponse().getContentAsString().startsWith("eyJ"))
    }
    @Test
    fun loginTestFail() {
        userService.createUser("username", "password", Locale.US)
        val encoder = Base64.getEncoder()
        val basic = encoder.encodeToString("username:wordpass".toByteArray())
        val loginResult = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/auth/login")
                    .header("Authorization", "Basic ${basic}")
            )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andReturn();
    }

    @Test
    fun refreshTokenTest() {
//        val jws = createBearerToken("userx", jwtSigningKey!!, JWT_ALG, timeHandler)
        val refreshToken = createRefreshToken("userx", jwtSigningKey!!, JWT_ALG, timeHandler)
        val testResult = mockMvc
            .perform(
                MockMvcRequestBuilders.get("/auth/refresh")
                    .param("refresh_token", refreshToken)
//                    .header("Authorization", "Bearer $jws")
            )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn()
    }

    @Test
    fun givenTokenTypeIsAccessWhenUseForRefreshThenFail() {
        val jws = createBearerToken("userx", jwtSigningKey!!, JWT_ALG, timeHandler)
        val testResult = mockMvc
            .perform(
                MockMvcRequestBuilders.get("/auth/refresh")
                    .param("refresh_token", jws)
//                    .header("Authorization", "Bearer $jws")
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isUnauthorized(),
                MockMvcResultMatchers.jsonPath(
                    "$.$CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE", Matchers.equalTo(ErrorCode.INVALID_AUTH_TOKEN_TYPE.code)
                )
            )
            .andReturn()
    }

    @Test
    fun givenTokenTypeIsRefreshWhenUseForAccessThenFail() {
        val refreshToken = createRefreshToken("userx", jwtSigningKey!!, JWT_ALG, timeHandler)
        val testResult = mockMvc
            .perform(
                MockMvcRequestBuilders.get("/test").header("Authorization", "Bearer $refreshToken")
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isUnauthorized(),
                MockMvcResultMatchers.jsonPath(
                    "$.$CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE", Matchers.equalTo(ErrorCode.INVALID_AUTH_TOKEN_TYPE.code)
                )
            )
            .andReturn()
    }


    companion object {
        private val LOGGER = LoggerFactory.getLogger(AuthControllerTest::class.java)
    }
}