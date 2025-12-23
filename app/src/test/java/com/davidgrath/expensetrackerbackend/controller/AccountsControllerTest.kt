package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.configuration.SecurityConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TestConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.repository.UserRepository
import com.davidgrath.expensetrackerbackend.testutils.TestConstants
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.createBearerToken
import com.davidgrath.expensetrackerbackend.testutils.tempInit
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.JWT_ALG
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@RunWith(SpringRunner::class)
@WebMvcTest(AccountsController::class)
@ContextConfiguration(
    classes = [
        AccountsController::class, SecurityConfiguration::class, TestConfiguration::class, TestAuthController::class, ExceptionResponseHandler::class, TestUtilComponent::class
    ])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AccountsControllerTest {

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

    @Value("\${jwt.signingkey}")
    private val jwtSigningKey: String? = null
    @Autowired
    lateinit var timeHandler: TimeHandler

    @Before
    fun setUp() {
        tempInit(testUtilComponent, timeHandler)
    }

    @After
    fun tearDown() {
        testUtilComponent.deleteAll()
    }

    @Test //TODO Exclude Jupiter so I don't mistakenly import it at some point
    fun getAccountHappyPathTest() {
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!


        val account = accountRepository.getAllByProfileId(profile.id!!).firstOrNull()!!


        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/accounts/{id}", account.id)
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", profile.stringId)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().is2xxSuccessful
            )
    }

    /**
     * This test feels like a false positive because I got an exception from referenceNumber not being nullable and this test passed while it was still non-nullable
     */
    @Test
    fun getAccountsHappyPathTest() {
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)!!



        val jws = createBearerToken("username", jwtSigningKey!!, JWT_ALG, timeHandler)

        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/accounts")
                    .header("Authorization", "Bearer ${jws}")
                    .header("Profile-Id", profile.stringId)
            )
            .andExpectAll(
                MockMvcResultMatchers.status().is2xxSuccessful,
                MockMvcResultMatchers.jsonPath("$", Matchers.hasSize<Int>(1))
            )
    }
}