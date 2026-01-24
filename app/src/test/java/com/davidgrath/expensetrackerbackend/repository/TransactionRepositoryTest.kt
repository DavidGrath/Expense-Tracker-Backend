package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.configuration.TestRunConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.AccountPojo
import com.davidgrath.expensetrackerbackend.testutils.DataBuilder
import com.davidgrath.expensetrackerbackend.testutils.TestConstants
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.assertEqualsBD
import com.davidgrath.expensetrackerbackend.testutils.getDefaultAccountId
import com.davidgrath.expensetrackerbackend.testutils.tempInit
import com.davidgrath.expensetrackerbackend.utils.Constants
import org.junit.After
import org.junit.Assert.*
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
import java.time.LocalTime

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
    fun sumByDateTest() {

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
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)

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
    fun totalSpentTest() {
        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()

        val time = LocalTime.of(8, 0, 0)
        val firstTransactionDate = LocalDate.parse("2025-01-01")
        val secondTransactionDate = LocalDate.parse("2025-01-02")
        val thirdTransactionDate = LocalDate.parse("2025-01-04")
        val fourthTransactionDate = LocalDate.parse("2025-01-04")
        val fifthTransactionDate = LocalDate.parse("2025-01-05")

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)

        val dataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)


        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(110.00))
            .atDate(firstTransactionDate)
            .atTime(time)
            .commit()
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(300.00))
            .atDate(secondTransactionDate)
            .atTime(time)
            .commit()
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(450.00))
            .atDate(thirdTransactionDate)
            .atTime(time)
            .commit()
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(750.00))
            .atDate(fourthTransactionDate)
            .atTime(time)
            .commit()
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(1000.00))
            .atDate(fifthTransactionDate)
            .atTime(time)
            .commit()

        val firstSum = transactionRepository.getTransactionSum(profile!!.id!!, true, firstTransactionDate, null, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

        assertEqualsBD(BigDecimal(2610.00), firstSum)

        val secondSum = transactionRepository.getTransactionSum(profile.id!!, true, firstTransactionDate, fourthTransactionDate, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        assertEqualsBD(BigDecimal(1610.00), secondSum)
    }

    @Test
    fun sumByCategoryTest() {

        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)

        val dataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)


        val fitness = categoryRepository.findByProfileIdAndStringId(profile!!.id!!, "fitness")!!
        val food = categoryRepository.findByProfileIdAndStringId(profile.id!!, "food")!!

        val fromDate = LocalDate.parse("2025-01-01")
        val id = dataBuilder.createTransaction()
            .atDate(fromDate)
            .withItem("Bread", "food", BigDecimal(1_500))
            .withItem("Dumbbells", "fitness", BigDecimal(2_500))
            .commit().first()
        val toDate = LocalDate.parse("2025-01-02")

        val id2 = dataBuilder.createTransaction()
            .atDate(toDate)
            .withItem("Water", "food", BigDecimal(1_000))
            .withItem("Sweatpants", "fitness", BigDecimal(5_000))
            .commit().first()

        val id3 = dataBuilder.createTransaction()
            .atDate(toDate.plusDays(1))
            .withItem("Fish", "food", BigDecimal(3_000))
            .withItem("Jump Rope", "fitness", BigDecimal(5_000))
            .commit()


        val sumList = transactionRepository.getSumByCategory(profile!!.id!!, true, fromDate, null, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        var foodSum = sumList.find { it.categoryId == food.id }!!.sum
        var fitnessSum = sumList.find { it.categoryId == fitness.id }!!.sum

        assertEqualsBD(BigDecimal(5_500), foodSum)
        assertEqualsBD(BigDecimal(12_500), fitnessSum)

        val sumListTo = transactionRepository.getSumByCategory(profile.id!!, true, fromDate, toDate, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        foodSum = sumListTo.find { it.categoryId == food.id }!!.sum
        fitnessSum = sumListTo.find { it.categoryId == fitness.id }!!.sum

        assertEqualsBD(BigDecimal(2_500), foodSum)
        assertEqualsBD(BigDecimal(7_500), fitnessSum)
    }

    @Test
    fun transactionAndItemCountTest() {

        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()


        val transactionCount = 3L
        val itemCount = 7L
        val filteredTransactionCount = 2L
        val filteredItemCount = 4L

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)

        val dataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)


        val fitness = categoryRepository.findByProfileIdAndStringId(profile!!.id!!, "fitness")!!
        val food = categoryRepository.findByProfileIdAndStringId(profile.id!!, "food")!!
        val clothing = categoryRepository.findByProfileIdAndStringId(profile.id!!, "clothing")!!
        val transport = categoryRepository.findByProfileIdAndStringId(profile.id!!, "transportation")!!

        val date = LocalDate.parse("2025-01-01")
        val id = dataBuilder.createTransaction()
            .atDate(date)
            .withItem("Bread", "food", BigDecimal(1_500))
            .withItem("Dumbbells", "fitness", BigDecimal(2_500))
            .commit().first()

        val id2 = dataBuilder.createTransaction()
            .atDate(date)
            .withItem("Hammer", "utilities", BigDecimal(5_000))
            .withItem("Sweatpants", "fitness", BigDecimal(5_000))
            .commit().first()

        val id3 = dataBuilder.createTransaction()
            .atDate(date)
            .withItem("Fish", "food", BigDecimal(3_000))
            .withItem("Red shirt", "clothing", BigDecimal(5_000))
            .withItem("Fuel", "transportation", BigDecimal(5_000))
            .commit()


        val counts = transactionRepository.getTransactionAndItemCount(profile!!.id!!, date, null, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())


        assertEquals(transactionCount, counts.transactionCount)
        assertEquals(itemCount, counts.itemCount)

        val categories = listOf(food, clothing, transport).map { it.id!! }
        val countsFiltered = transactionRepository.getTransactionAndItemCount(profile.id!!, date, null, emptyList(), emptyList(), categories, emptyList(), emptyList())

        assertEquals(filteredTransactionCount, countsFiltered.transactionCount)
        assertEquals(filteredItemCount, countsFiltered.itemCount)
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
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)
        val newAccountId = accountRepository.insertAccount(AccountPojo(null, profile!!.id!!, "GBP",  null, null, "British",
            LocalDateTime.now(timeHandler.getClock())))
        val emptyAccountId = accountRepository.insertAccount(AccountPojo(null, profile!!.id!!, "GBP",  null, null, "Empty",
            LocalDateTime.now(timeHandler.getClock())))
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
    fun cumSumByDateTest() {

        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()

        val firstTransactionDate = LocalDate.parse("2025-01-01")
        val secondTransactionDate = LocalDate.parse("2025-01-02")
        val thirdTransactionDate = LocalDate.parse("2025-01-04")
        val fourthTransactionDate = LocalDate.parse("2025-01-04")
        val fifthTransactionDate = LocalDate.parse("2025-01-05")

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)

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

        val sums = transactionRepository.getTransactionCumSumByDate(profile!!.id!!, true, firstTransactionDate, null, emptyList(), emptyList(), emptyList() , emptyList(),  emptyList())
        val firstSum = sums.find { it.aggregateDate.compareTo(firstTransactionDate) == 0 }!!
        val secondSum = sums.find { it.aggregateDate.compareTo(secondTransactionDate) == 0 }!!
        val thirdSum = sums.find { it.aggregateDate.compareTo(thirdTransactionDate) == 0 }!!
        assertEquals(4, sums.size)
        assertEqualsBD(BigDecimal(110.00), firstSum.sum)
        assertEqualsBD(BigDecimal(410.00), secondSum.sum)
        assertEqualsBD(BigDecimal(1610.00), thirdSum.sum)
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
        val profile = profileRepository.findByStringId(user.id!!, Constants.DEFAULT_PROFILE_ID)

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