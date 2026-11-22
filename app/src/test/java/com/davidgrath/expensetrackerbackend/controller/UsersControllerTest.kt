package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.configuration.SecurityConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TestRunControllerConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.configuration.WebMvcConfiguration
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
import com.davidgrath.expensetrackerbackend.generatedapi.model.UserStatus
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.createBearerToken
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.JWT_ALG
import com.davidgrath.expensetrackerbackend.utils.bytesToUUID
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
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.Locale
import java.util.UUID

@RunWith(SpringRunner::class)
@WebMvcTest(UsersController::class)
@ContextConfiguration(
    classes = [
        UsersController::class, SecurityConfiguration::class, TestRunControllerConfiguration::class, ExceptionResponseHandler::class,
        TestUtilComponent::class, WebMvcConfiguration::class
    ])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class UsersControllerTest {

    @Autowired
    lateinit var userService: UserService
    @Autowired
    lateinit var timeHandler: TimeHandler
    @Autowired
    lateinit var testUtilComponent: TestUtilComponent

    @Value("\${jwt.signingkey}")
    private val jwtSigningKey: String? = null
    @Autowired
    lateinit var mockMvc: MockMvc

    @Before
    fun setUp() {
        userService.createUser("username", "swordPas", Locale.US)
    }

    @After
    fun tearDown() {
        testUtilComponent.deleteAll()
    }

    @Test
    fun givenStateIsActiveWhenGetMeThenCorrectDetailsPopulated() {

        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)
        val user = userService.getByUsername("username")
        val userUuid = bytesToUUID(user!!.uuid!!).toString()

        mockMvc
            .perform(
                MockMvcRequestBuilders.request(HttpMethod.GET,"/users/me")
                    .header("Authorization", "Bearer ${jws}")
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk,
//                MockMvcResultMatchers.jsonPath("$.email", Matchers.equalTo("new.user@example.com")), //TODO Rework sign up
//                MockMvcResultMatchers.jsonPath("$.status", Matchers.equalTo(UserStatus.ACTIVE.value)),
                MockMvcResultMatchers.jsonPath("$.uuid", Matchers.equalTo(userUuid)),
                MockMvcResultMatchers.jsonPath("$.username", Matchers.equalTo(user.username)),
//                MockMvcResultMatchers.jsonPath("$.signedUpDateTimeUTC", Matchers.equalTo("2026-06-30T08:00")),
                MockMvcResultMatchers.jsonPath("$.activatedDateTimeUTC", Matchers.equalTo("2025-06-30T08:00:00Z")),
            ).andReturn()
    }

    @Test
    @Ignore("Not ready yet")
    fun givenStateIsNewWhenGetMeThenCorrectDetailsPopulated() {

        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)
        val user = userService.getByUsername("username")
        val userUuid = bytesToUUID(user!!.uuid!!).toString()

        mockMvc
            .perform(
                MockMvcRequestBuilders.request(HttpMethod.GET,"/users/me")
                    .header("Authorization", "Bearer ${jws}")
            )
            .andExpectAll(
                MockMvcResultMatchers.status().isOk,
//                MockMvcResultMatchers.jsonPath("$.email", Matchers.equalTo("new.user@example.com")), //TODO Rework sign up
//                MockMvcResultMatchers.jsonPath("$.status", Matchers.equalTo(UserStatus.NEW.value)),
                MockMvcResultMatchers.jsonPath("$.uuid", Matchers.equalTo(userUuid)), //TODO get the actual one
                MockMvcResultMatchers.jsonPath("$.username", Matchers.nullValue()),
//                MockMvcResultMatchers.jsonPath("$.signedUpDateTimeUTC", Matchers.equalTo("2026-06-30T08:00")), //TODO get the actual one
                MockMvcResultMatchers.jsonPath("$.activatedDateTimeUTC", Matchers.nullValue()),
            ).andReturn()
    }
}