package com.davidgrath.expensetrackerbackend.controller

import com.davidgrath.expensetrackerbackend.generatedapi.model.BasicTransactionCreateRequest
import com.davidgrath.expensetrackerbackend.configuration.SecurityConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TestConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.controller.exception.ErrorCode
import com.davidgrath.expensetrackerbackend.controller.exception.ExceptionResponseHandler
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.CategoryRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.repository.UserRepository
import com.davidgrath.expensetrackerbackend.service.UserService
import com.davidgrath.expensetrackerbackend.testutils.TestConstants
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.createBearerToken
import com.davidgrath.expensetrackerbackend.testutils.getDefaultAccountId
import com.davidgrath.expensetrackerbackend.testutils.tempInit
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.CUSTOM_PROBLEM_DETAILS_ATTRIBUTE_ERROR_CODE
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.JWT_ALG
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
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
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigDecimal
import java.util.Base64
import java.util.Locale

@RunWith(SpringRunner::class)
@WebMvcTest(TransactionsController::class)
@ContextConfiguration(
    classes = [
        TransactionsController::class, SecurityConfiguration::class, TestConfiguration::class, ExceptionResponseHandler::class, TestUtilComponent::class
    ])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class TransactionsDraftControllerTest {

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
    @Ignore("Next commit")
    fun givenItemCountIsMaxWhenAddItemThenBadRequest() {
        assertTrue(false)
    }

    @Test
    @Ignore("Next commit")
    fun givenDraftNotExistWhenPerformAnyOtherOperationThanPostThenBadRequest() {
        assertTrue(false)
    }

    /**
     * Primarily seller,account,category, but I feel like this IDOR thing can be handled at a higher level to reduce
     * the chances of me forgetting
     */
    @Test
    @Ignore("Next commit")
    fun givenAnyEntityNotBelongToUserWhenModifyDraftThenBadRequest() {
        assertTrue(false)
    }

    @Test
    @Ignore("Next commit")
    fun givenDraftExistsWhenCreateDraftThenBadRequest() {
        assertTrue(false)
    }


    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionsDraftControllerTest::class.java)
    }
}