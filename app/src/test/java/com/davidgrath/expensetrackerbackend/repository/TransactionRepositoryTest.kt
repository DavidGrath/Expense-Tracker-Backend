package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.configuration.TestRunConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.entities.StatisticsFilterDb
import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.entities.views.TransactionAndItemCount
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.AccountPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.CategoryPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ProfilePojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.SellerPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.UserPojo
import com.davidgrath.expensetrackerbackend.testutils.*
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.FALSE_BYTE
import com.davidgrath.expensetrackerbackend.utils.uuidToBytes
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Currency
import java.util.Locale
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


    //TODO Test for all filters
    @Test
    fun sumByCategoryTest() {

        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)

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


        var statisticsFilter = StatisticsFilterDb(fromDate = fromDate)
        val sumList = transactionRepository.getSumByCategory(profile!!.id!!, true, statisticsFilter)
        var foodSum = sumList.find { it.categoryId == food.id }!!.sum
        var fitnessSum = sumList.find { it.categoryId == fitness.id }!!.sum

        assertEqualsBD(BigDecimal(5_500), foodSum)
        assertEqualsBD(BigDecimal(12_500), fitnessSum)

        statisticsFilter = StatisticsFilterDb(fromDate = fromDate, toDate = toDate)
        val sumListTo = transactionRepository.getSumByCategory(profile.id!!, true, statisticsFilter)
        foodSum = sumListTo.find { it.categoryId == food.id }!!.sum
        fitnessSum = sumListTo.find { it.categoryId == fitness.id }!!.sum

        assertEqualsBD(BigDecimal(2_500), foodSum)
        assertEqualsBD(BigDecimal(7_500), fitnessSum)
    }

    @Test
    fun transactionAndItemCountFiltersTest() {

        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()



        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)

        val dataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)


        val fitness = categoryRepository.findByProfileIdAndStringId(profile!!.id!!, "fitness")!!
        val food = categoryRepository.findByProfileIdAndStringId(profile.id!!, "food")!!
        val clothing = categoryRepository.findByProfileIdAndStringId(profile.id!!, "clothing")!!
        val transport = categoryRepository.findByProfileIdAndStringId(profile.id!!, "transportation")!!

        val firstDate = LocalDate.parse("2025-01-01") // Wednesday
        val secondDate = LocalDate.parse("2025-01-02")
        val thirdDate = LocalDate.parse("2025-01-03")
        val forthDate = LocalDate.parse("2025-01-04")
        val fifthDate = LocalDate.parse("2025-01-05")
        val sixthDate = LocalDate.parse("2025-01-06")
        val seventhDate = LocalDate.parse("2025-01-07")
        val eighthDate = LocalDate.parse("2025-01-08")

        val transactionBuilder = dataBuilder.createTransaction()
            .atDate(firstDate)
            .withItem("Bread", "food", BigDecimal(1_500))
            .withItem("Dumbbells", "fitness", BigDecimal(2_500))
            .withNewAccount("USD", "Test Account")
        val newAccountId = transactionBuilder.getAccount().id!!
        val id = transactionBuilder.commit().first()

        val id2 = dataBuilder.createTransaction()
            .atDate(secondDate)
            .withItem("Hammer", "utilities", BigDecimal(5_000))
            .withItem("Sweatpants", "fitness", BigDecimal(5_000))
            .commit().first()

        val id3 = dataBuilder.createTransaction()
            .atDate(thirdDate)
            .withItem("Fish", "food", BigDecimal(3_000))
            .withItem("Red shirt", "clothing", BigDecimal(5_000))
            .withItem("Fuel", "transportation", BigDecimal(5_000))
            .commit()

        dataBuilder.createTransaction()
            .atDate(forthDate)
            .withItem("Medicine", "healthcare", BigDecimal(1_000))
            .withNewSeller("Generic Hospital")
            .withNewAccount("USD", "Test Account 2")
            .commit()

        val firstSellerId = sellerRepository.getAll(profile.id!!).first()!!.id!!

        dataBuilder.createTransaction()
            .atDate(fifthDate)
            .withItem("Vitamin C", "healthcare", BigDecimal(1_000))
            .withSeller(firstSellerId)
            .withNewSellerLocation("Brand New Branch")
            .withNewAccount("USD", "Test Account 3")
            .commit()

        dataBuilder.createTransaction()
            .atDate(sixthDate)
            .withItem("Bandages", "healthcare", BigDecimal(1_000))
            .withSeller(firstSellerId)
            .withNewSellerLocation("New New Branch")
            .withNewAccount("USD", "Test Account 4")
            .commit()

        dataBuilder.createTransaction()
            .atDate(seventhDate)
            .withItem("Light bulb", "household", BigDecimal(1_000))
            .withNewSeller("Generic Hardware Store")
            .withNewAccount("USD", "Test Account 5")
            .withMode(TransactionMode.Card)
            .commit()


        val secondSellerId = sellerRepository.getAll(profile.id!!)[1].id!!

        dataBuilder.createTransaction()
            .atDate(eighthDate)
            .withItem("Paint", "household", BigDecimal(1_000))
            .withItem("Light bulb", "household", BigDecimal(1_000))
            .withSeller(secondSellerId)
            .withNewSellerLocation("Somewhere out there")
            .withNewAccount("USD", "Test Account 6")
            .withMode(TransactionMode.Card)
            .commit()

        var statisticsFilter = StatisticsFilterDb()

        val countsNoFilter = transactionRepository.getTransactionAndItemCount(profile!!.id!!, statisticsFilter)



        val categories = listOf(food, clothing, transport).map { it.id!! }
        statisticsFilter = StatisticsFilterDb(categoryIds = categories)
        val countsFilteredByCategory = transactionRepository.getTransactionAndItemCount(profile.id!!, statisticsFilter)
        statisticsFilter = StatisticsFilterDb(currencyCode = Currency.getInstance("USD"), accountIds = listOf(newAccountId))
        val countsFilteredByCurrency = transactionRepository.getTransactionAndItemCount(profile.id!!, statisticsFilter)
        statisticsFilter = StatisticsFilterDb(accountIds = listOf(accountId))
        val countsFilteredByAccount = transactionRepository.getTransactionAndItemCount(profile.id!!, statisticsFilter)

        statisticsFilter = StatisticsFilterDb(weekDays = listOf(DayOfWeek.WEDNESDAY))
        val countsFilteredByWeekday = transactionRepository.getTransactionAndItemCount(profile.id!!, statisticsFilter)
        statisticsFilter = StatisticsFilterDb(transactionModes = listOf(TransactionMode.Card))
        val countsFilteredByMode = transactionRepository.getTransactionAndItemCount(profile.id!!, statisticsFilter)
        val firstSellerFirstLocationId = sellerLocationRepository.getByProfileIdAndSellerId(profile.id!!, firstSellerId).first().id!!
        val firstSellerSecondLocationId = sellerLocationRepository.getByProfileIdAndSellerId(profile.id!!, firstSellerId)[1].id!!

        val secondSellerLocationId =  sellerLocationRepository.getByProfileIdAndSellerId(profile.id!!, secondSellerId).first().id!!
        statisticsFilter = StatisticsFilterDb(sellerIds = listOf(firstSellerId), sellerLocationIds = listOf(secondSellerLocationId))
        val countsFilteredBySeller = transactionRepository.getTransactionAndItemCount(profile.id!!, statisticsFilter)
        statisticsFilter = StatisticsFilterDb(sellerLocationIds = listOf(firstSellerFirstLocationId, secondSellerLocationId))
        val countsFilteredByLocation = transactionRepository.getTransactionAndItemCount(profile.id!!, statisticsFilter)

        assertEquals(TransactionAndItemCount(8, 13), countsNoFilter)
        assertEquals(TransactionAndItemCount(2, 4), countsFilteredByCategory)
        assertEquals(TransactionAndItemCount(2, 4), countsFilteredByWeekday)
        assertEquals(TransactionAndItemCount(8, 13), countsFilteredByCurrency)
        assertEquals(TransactionAndItemCount(2, 5), countsFilteredByAccount)
        assertEquals(TransactionAndItemCount(2, 3), countsFilteredByMode)
        assertEquals(TransactionAndItemCount(4, 5), countsFilteredBySeller)
        assertEquals(TransactionAndItemCount(2, 3), countsFilteredByLocation)
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

    /**
     * I accidentally thought I had accidentally created an IDOR path through the seller ID,
     * but I've confirmed this doesn't happen through this test. Keep it here to mark my paranoia
     */
    @Test
    fun idorTest() {
        val clock = timeHandler.getClock()
        val date = ZonedDateTime.now(clock)
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()
        val user2Uuid = UUID.randomUUID()
        val user2UuidBytes = uuidToBytes(user2Uuid)
        val user2Id = userRepository.insertUser(UserPojo(null, user2UuidBytes,"user2", "password", "", utcDate))
        val defaultUserProfileUuid = uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID))
        val user2ProfileUuid = uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID))
        val user2Profile = ProfilePojo(null, user2Id, "Default Profile", user2ProfileUuid, utcDate)
        val user2ProfileId = profileRepository.insertProfile(user2Profile)

        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()

        val user1DataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)
        user1DataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(100))
            .withNewSeller("Restaurant")
            .commit()

        val user2DataBuilder = DataBuilder(folder, timeHandler, user2Uuid.toString(), Constants.DEFAULT_PROFILE_ID, testUtilComponent)
        val account = AccountPojo(null, user2ProfileId, "USD", null, null, "Default Account", utcDate)
        accountRepository.insertAccount(account)
        categoryRepository.insertCategory(CategoryPojo(null, user2ProfileId, "miscellaneous", FALSE_BYTE, null, utcDate, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category"))
        user2DataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(100))
            .withNewSeller("Hardware store")
            .commit()

        val user1Id = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!.id!!
        val user1ProfileId = profileRepository.findByUuid(user1Id, defaultUserProfileUuid)!!.id!!
        val user1Seller = sellerRepository.getAll(user1ProfileId).first()!!.id!!
        val user2Seller = sellerRepository.getAll(user2ProfileId).first()!!.id!!

        val statisticsFilter = StatisticsFilterDb(sellerIds = listOf(user1Seller, user2Seller))
        val count = transactionRepository.getTransactionAndItemCount(user1ProfileId, statisticsFilter)
        assertEquals(TransactionAndItemCount(1, 1), count)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionRepositoryTest::class.java)
    }
}