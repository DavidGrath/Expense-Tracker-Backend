package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TestRunConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.AccountPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.CategoryPojo
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.CategoryRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.repository.TransactionRepository
import com.davidgrath.expensetrackerbackend.repository.UserRepository
import com.davidgrath.expensetrackerbackend.service.exceptions.CountLimitExceededException
import com.davidgrath.expensetrackerbackend.service.exceptions.InvalidCurrencyCodeException
import com.davidgrath.expensetrackerbackend.testutils.TestConstants
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.tempInit
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.FALSE_BYTE
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.TRUE_BYTE
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDateTime

@SpringBootTest
@RunWith(SpringRunner::class)
@ContextConfiguration(
    classes = [
        TestRunConfiguration::class,
        TransactionService::class,
        TransactionRepository::class
    ])
class AccountServiceTest {

    @Autowired
    lateinit var accountService: AccountService

    @Autowired
    lateinit var timeHandler: TimeHandler
    @Autowired
    lateinit var profileRepository: ProfileRepository

    @Autowired
    lateinit var categoryRepository: CategoryRepository
    @Autowired
    lateinit var accountRepository: AccountRepository
    @Autowired
    lateinit var userRepository: UserRepository
    @Autowired
    lateinit var testUtilComponent: TestUtilComponent

    @Before
    fun setUp() {
        tempInit(testUtilComponent, timeHandler)
    }

    @After
    fun tearDown() {
        testUtilComponent.deleteAll()
    }


    @Test
    fun givenAccountsFullWhenCreateAccountThenLimitExceeded() {


        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val existing = accountRepository.getAllByProfileId(profile.id!!)
        val remaining = Constants.Constraints.MAX_ACCOUNTS_PER_PROFILE - existing.size
        if(remaining <= 0) {
            throw RuntimeException("Existing accounts too many")
        }
        val createdAt = LocalDateTime.now(timeHandler.getClock())
        for(sel in 0 until remaining) {
            val ordinal = (sel+1+existing.size).toShort()
            val sortKey = ((sel+1+existing.size) * 1_000).toShort()
            val occupancySlot = (sel+1+existing.size).toShort()
            val account = AccountPojo(null, profile.id!!, "USD", null, null, "New Account-$sel", FALSE_BYTE,ordinal, sortKey, occupancySlot, createdAt)
            accountRepository.insertAccount(account)
        }
        Assert.assertThrows(CountLimitExceededException::class.java) {
            accountService.createAccount(profile.id!!, "New", "USD")
        }
    }


    @Test
    fun givenCurrencyCodeInvalidWhenCreateAccountThenLimitExceeded() {

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        Assert.assertThrows(InvalidCurrencyCodeException::class.java) {
            accountService.createAccount(profile.id!!, "New", "XYZ")
        }
    }
}