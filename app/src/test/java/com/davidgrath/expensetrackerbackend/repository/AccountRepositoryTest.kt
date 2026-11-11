package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.configuration.TestRunConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.AccountPojo
import com.davidgrath.expensetrackerbackend.testutils.*
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.FALSE_BYTE
import com.davidgrath.expensetrackerbackend.utils.uuidToBytes
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@SpringBootTest
@RunWith(SpringRunner::class)
@ContextConfiguration(
    classes = [
        TestRunConfiguration::class,
        TransactionRepository::class,
        ProfileRepository::class,
        UserRepository::class,
        AccountRepository::class
    ])
class AccountRepositoryTest {


    @Autowired
    lateinit var accountRepository: AccountRepository
    @Autowired
    lateinit var userRepository: UserRepository
    @Autowired
    lateinit var profileRepository: ProfileRepository
    @Autowired
    lateinit var timeHandler: TimeHandler
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
    fun getAccountWithStatsTest() {

        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()

        val totalExpense = BigDecimal(100)
        val totalIncome = BigDecimal(200)
        val totalTransactions = 2L
        val totalItems = 3L

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val user = userRepository.findByUuid(uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)))!!
        val profile = profileRepository.findByUuid(user.id!!, uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID)))

        val dataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)

        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(100))
            .atDate(LocalDate.now(timeHandler.getClock()))
            .commit()

        dataBuilder.createTransaction()
            .debitOrCredit(false)
            .withItem("Description", "miscellaneous", BigDecimal(100))
            .withItem("Description 2", "miscellaneous", BigDecimal(100))
            .atDate(LocalDate.now(timeHandler.getClock()))
            .commit()


        val accountStats = accountRepository.getAccountSummary(profile!!.id!!, accountId)!!
        assertEqualsBD(totalExpense, accountStats.expenses)
        assertEqualsBD(totalIncome, accountStats.income)
        assertEquals(totalTransactions, accountStats.transactionCount)
        assertEquals(totalItems, accountStats.itemCount)

        val newAccountId = accountRepository.insertAccount(AccountPojo(null, profile.id!!, "GBP",  null, null, "British",
            FALSE_BYTE, 2, 2_000, 2, LocalDateTime.now(timeHandler.getClock())))


        val newAccountStats = accountRepository.getAccountSummary(profile.id!!, newAccountId)!!
        assertEqualsBD(BigDecimal.ZERO, newAccountStats.expenses)
        assertEqualsBD(BigDecimal.ZERO, newAccountStats.income)
        assertEquals(0, newAccountStats.transactionCount)
        assertEquals(0, newAccountStats.itemCount)
    }

    @Test
    fun statsByAccountTest() {

        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()

        val totalExpense = BigDecimal(100)
        val totalIncome = BigDecimal(200)
        val totalTransactions = 2L
        val totalItems = 3L

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val user = userRepository.findByUuid(uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)))!!
        val profile = profileRepository.findByUuid(user.id!!, uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID)))!!

        val dataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)

        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(100))
            .atDate(LocalDate.now(timeHandler.getClock()))
            .commit()

        dataBuilder.createTransaction()
            .debitOrCredit(false)
            .withDefaultItemPrice(BigDecimal(100))
            .withItem("Description").withItem("Description 2")
            .atDate(LocalDate.now(timeHandler.getClock()))
            .commit()


        val newAccountId = accountRepository.insertAccount(AccountPojo(null, profile.id!!, "GBP",  null, null, "British",
            FALSE_BYTE, 2, 2_000, 2, LocalDateTime.now(timeHandler.getClock())))

        val accountStatsList = accountRepository.getAccountSummariesForProfile(profile.id!!)


        assertEqualsBD(totalExpense, accountStatsList[0].expenses)
        assertEqualsBD(totalIncome, accountStatsList[0].income)
        assertEquals(totalTransactions, accountStatsList[0].transactionCount)
        assertEquals(totalItems, accountStatsList[0].itemCount)

        assertEqualsBD(BigDecimal.ZERO, accountStatsList[1].expenses)
        assertEqualsBD(BigDecimal.ZERO, accountStatsList[1].income)
        assertEquals(0, accountStatsList[1].transactionCount)
        assertEquals(0, accountStatsList[1].itemCount)
    }
}