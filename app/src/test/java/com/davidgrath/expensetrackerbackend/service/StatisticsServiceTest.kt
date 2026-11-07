package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TestRunConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.AccountPojo
import com.davidgrath.expensetrackerbackend.generatedapi.model.StatisticsFilter
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.repository.TransactionRepository
import com.davidgrath.expensetrackerbackend.repository.UserRepository
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.getDefaultAccountId
import com.davidgrath.expensetrackerbackend.testutils.getDefaultProfileId
import com.davidgrath.expensetrackerbackend.testutils.tempInit
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import java.time.ZoneId
import java.time.ZonedDateTime

@SpringBootTest
@RunWith(SpringRunner::class)
@ContextConfiguration(
    classes = [
        TestRunConfiguration::class,
        StatisticsService::class,
        TransactionRepository::class
    ])
class StatisticsServiceTest {

    @Autowired
    lateinit var statisticsService: StatisticsService

    @Autowired
    lateinit var timeHandler: TimeHandler
    @Autowired
    lateinit var testUtilComponent: TestUtilComponent
    @Autowired
    lateinit var accountRepository: AccountRepository
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var profileRepository: ProfileRepository

    @Before
    fun setUp() {
        tempInit(testUtilComponent, timeHandler)
    }

    @After
    fun tearDown() {
        testUtilComponent.deleteAll()
    }

    /**
     * ACR-1
     */
    @Test
    fun givenAccountListIsNotEmptyAndAccountListDoesNotHaveHeterogeneousCurrenciesAndCurrencyIsNotNullWhenValidateThenCurrencyOverride() {
        val clock = timeHandler.getClock()
        val date = ZonedDateTime.now(clock)
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()

        val profileId = getDefaultProfileId(userRepository, profileRepository)

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val currency = "USD"
        val statisticsFilter = StatisticsFilter().accounts(listOf(accountId)).currencyCode(currency)

        assertEquals(true, statisticsService.validateFilter(profileId, statisticsFilter))
    }

    /**
     * ACR-2
     */
    @Test
    fun givenAccountListIsNotEmptyAndAccountListDoesNotHaveHeterogeneousCurrenciesAndCurrencyIsNullWhenValidateThenTrue() {
        val clock = timeHandler.getClock()
        val date = ZonedDateTime.now(clock)
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()

        val profileId = getDefaultProfileId(userRepository, profileRepository)

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val statisticsFilter = StatisticsFilter().accounts(listOf(accountId))

        assertEquals(true, statisticsService.validateFilter(profileId, statisticsFilter))
    }

    /**
     * ACR-3
     */
    @Test
    fun givenAccountListIsNotEmptyAndAccountListHasHeterogeneousCurrenciesAndCurrencyIsNotNullWhenValidateThenCurrencyOverride() {
        val clock = timeHandler.getClock()
        val date = ZonedDateTime.now(clock)
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()

        val profileId = getDefaultProfileId(userRepository, profileRepository)
        val account = AccountPojo(null, profileId, "GBP", null, null, "Account name", utcDate)
        val newAccountId = accountRepository.insertAccount(account)
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)

        val currency = "USD"
        val statisticsFilter = StatisticsFilter().accounts(listOf(accountId, newAccountId)).currencyCode(currency)

        assertEquals(true, statisticsService.validateFilter(profileId, statisticsFilter))
    }

    /**
     * ACR-4
     */
    @Test
    fun givenAccountListIsNotEmptyAndAccountListHasHeterogeneousCurrenciesAndCurrencyIsNullWhenValidateThenFalse() {
        val clock = timeHandler.getClock()
        val date = ZonedDateTime.now(clock)
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()

        val profileId = getDefaultProfileId(userRepository, profileRepository)
        val account = AccountPojo(null, profileId, "GBP", null, null, "Account name", utcDate)
        val newAccountId = accountRepository.insertAccount(account)
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val statisticsFilter = StatisticsFilter().accounts(listOf(accountId, newAccountId))

        assertEquals(false, statisticsService.validateFilter(profileId, statisticsFilter))
    }

    /**
     * ACR-5
     */
    @Test
    fun givenAccountListIsEmptyAndAccountListDoesNotHaveHeterogeneousCurrenciesAndCurrencyIsNotNullWhenValidateThenCurrencyOverride() {
        val clock = timeHandler.getClock()
        val date = ZonedDateTime.now(clock)
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()

        val profileId = getDefaultProfileId(userRepository, profileRepository)
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)

        val currency = "USD"
        val statisticsFilter = StatisticsFilter().currencyCode(currency)

        assertEquals(true, statisticsService.validateFilter(profileId, statisticsFilter))
    }

    /**
     * ACR-6
     */
    @Test
    fun givenAccountListIsEmptyAndAccountListDoesNotHaveHeterogeneousCurrenciesAndCurrencyIsNullWhenValidateThenFalse() {
        val clock = timeHandler.getClock()
        val date = ZonedDateTime.now(clock)
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()

        val profileId = getDefaultProfileId(userRepository, profileRepository)
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)

        val statisticsFilter = StatisticsFilter()

        assertEquals(false, statisticsService.validateFilter(profileId, statisticsFilter))
    }
}