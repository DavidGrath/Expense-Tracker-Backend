package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.configuration.TestRunConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.entities.StatisticsFilterDb
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.AccountPojo
import com.davidgrath.expensetrackerbackend.testutils.*
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.FALSE_BYTE
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalDateTime

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
class TransactionRepositoryTest {

    @Autowired
    lateinit var transactionRepository: TransactionRepository
    @Autowired
    lateinit var userRepository: UserRepository
    @Autowired
    lateinit var profileRepository: ProfileRepository
    @Autowired
    lateinit var accountRepository: AccountRepository
    @Autowired
    lateinit var categoryRepository: CategoryRepository
    @Autowired
    lateinit var sellerRepository: SellerRepository
    @Autowired
    lateinit var sellerLocationRepository: SellerLocationRepository
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
    fun sumByDateTest() { //TODO These tests are wrong. Move

        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()

        val transactionTime = "06:00"
        val transactionOffset = "+05:00"
        val transactionTimezone = "Etc/GMT-5"

        val firstTransactionDate = LocalDate.parse("2025-01-01")
        val secondTransactionDate = LocalDate.parse("2025-01-02")
        val thirdTransactionDate = LocalDate.parse("2025-01-04")
        val fourthTransactionDate = LocalDate.parse("2025-01-04")
        val fifthTransactionDate = LocalDate.parse("2025-01-05")

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)

        val dataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(110.00))
            .atDate(firstTransactionDate)
            .commit()
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(300.00))
            .atDate(secondTransactionDate)
            .commit()
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(450.00))
            .atDate(thirdTransactionDate)
            .commit()
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(750.00))
            .atDate(fourthTransactionDate)
            .commit()
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(1_000.00))
            .atDate(fifthTransactionDate)
            .commit()

        val sums = transactionRepository.getTransactionSumByDate(profile!!.id!!, true, firstTransactionDate, null, emptyList(), emptyList(), emptyList() , emptyList(),  emptyList())
        val firstSum = sums.find { it.aggregateDate.compareTo(firstTransactionDate) == 0 }!!
        val secondSum = sums.find { it.aggregateDate.compareTo(secondTransactionDate) == 0 }!!
        val thirdSum = sums.find { it.aggregateDate.compareTo(thirdTransactionDate) == 0 }!!
        assertEqualsBD(BigDecimal(110.00), firstSum.sum)
        assertEqualsBD(BigDecimal(300.00), secondSum.sum)
        assertEqualsBD(BigDecimal(1200.00), thirdSum.sum)

        val boundSums = transactionRepository.getTransactionSumByDate(profile.id!!, true, firstTransactionDate, fourthTransactionDate, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        val fourthSum = boundSums.find { it.aggregateDate.compareTo(fifthTransactionDate) == 0 }
        assertNull(fourthSum)
    }





    @Test
    fun earliestDateTest() {

        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()

        val transactionTime = "06:00"
        val transactionOffset = "+05:00"
        val transactionTimezone = "Etc/GMT-5"

        val firstTransactionDate = LocalDate.parse("2025-01-01")
        val secondTransactionDate = LocalDate.parse("2025-01-02")
        val thirdTransactionDate = LocalDate.parse("2025-01-04")
        val fourthTransactionDate = LocalDate.parse("2025-01-04")
        val fifthTransactionDate = LocalDate.parse("2025-01-05")

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)
        val newAccountId = accountRepository.insertAccount(AccountPojo(null, profile!!.id!!, "GBP",  null, null, "British",
            FALSE_BYTE, 2, 2_000, 2,LocalDateTime.now(timeHandler.getClock())))
        val emptyAccountId = accountRepository.insertAccount(AccountPojo(null, profile!!.id!!, "GBP",  null, null, "Empty",
            FALSE_BYTE, 3, 3_000, 3, LocalDateTime.now(timeHandler.getClock())))
        val dataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(110.00))
            .atDate(firstTransactionDate)
            .commit()
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(300.00))
            .atDate(secondTransactionDate)
            .withAccountId(newAccountId)
            .commit()
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(450.00))
            .atDate(thirdTransactionDate)
            .withAccountId(newAccountId)
            .commit()
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(750.00))
            .atDate(fourthTransactionDate)
            .commit()
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(1_000.00))
            .atDate(fifthTransactionDate)
            .commit()

        val earliestDate = transactionRepository.getEarliestTransactionDate(profile!!.id!!, emptyList())
        assertEquals(firstTransactionDate, earliestDate)
        val earliestDateNewAccount = transactionRepository.getEarliestTransactionDate(profile.id!!, listOf(newAccountId))
        assertEquals(secondTransactionDate, earliestDateNewAccount)
        val earliestDateEmptyAccount: LocalDate? = transactionRepository.getEarliestTransactionDate(profile.id!!, listOf(emptyAccountId))
        assertNull(earliestDateEmptyAccount)
    }


    @Test
    fun topNExpensesPerCategoryTest() {
        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()

        val firstTransactionDate = LocalDate.parse("2025-01-01")
        val secondTransactionDate = LocalDate.parse("2025-01-02")
        val thirdTransactionDate = LocalDate.parse("2025-01-04")
        val fourthTransactionDate = LocalDate.parse("2025-01-04")
        val fifthTransactionDate = LocalDate.parse("2025-01-05")

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)

        val dataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(110.00))
            .atDate(firstTransactionDate)
            .commit()
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(300.00))
            .withItem("Description 2", "miscellaneous", BigDecimal(20.00))
            .withItem("Description 3", "miscellaneous", BigDecimal(10.00))
            .atDate(secondTransactionDate)
            .commit()
        dataBuilder.createTransaction()
            .withItem("Description", "food", BigDecimal(450.00))
            .atDate(thirdTransactionDate)
            .commit()
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(750.00))
            .atDate(fourthTransactionDate)
            .commit()
        dataBuilder.createTransaction()
            .withItem("Description", "food", BigDecimal(1_000.00))
            .atDate(fifthTransactionDate)
            .commit()

        val sums = transactionRepository.getTopNExpensesByCategory(profile!!.id!!, true, 3, firstTransactionDate, null, emptyList(), emptyList(), emptyList() , emptyList(),  emptyList())
        val firstSum = sums[0]
        val thirdSum = sums[2]

        assertEquals(5, sums.size)
        assertEquals("food", firstSum.stringId)
        assertEqualsBD(BigDecimal(1_450), firstSum.sum)
        assertEqualsBD(BigDecimal(1_000), firstSum.itemAmount)

        assertEquals("miscellaneous", thirdSum.stringId)
        assertEqualsBD(BigDecimal(1_190), thirdSum.sum)
        assertEqualsBD(BigDecimal(750), thirdSum.itemAmount)

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionRepositoryTest::class.java)
    }
}

