package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.configuration.TestRunConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.entities.StatisticsFilterDb
import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.entities.views.TransactionAndItemCount
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.AccountPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.CategoryPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ProfilePojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.UserPojo
import com.davidgrath.expensetrackerbackend.testutils.DataBuilder
import com.davidgrath.expensetrackerbackend.testutils.TestConstants
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.assertEqualsBD
import com.davidgrath.expensetrackerbackend.testutils.getDefaultAccountId
import com.davidgrath.expensetrackerbackend.testutils.tempInit
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Currency
import java.util.UUID

@SpringBootTest
@RunWith(SpringRunner::class)
@ContextConfiguration(
    classes = [
        TestRunConfiguration::class,
        TransactionRepository::class,
        ProfileRepository::class,
        UserRepository::class,
        AccountRepository::class,
        StatisticsRepository::class
    ])
class StatisticsRepositoryTest {

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
    lateinit var statisticsRepository: StatisticsRepository
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
        val sumList = statisticsRepository.getSumByCategory(profile!!.id!!, true, statisticsFilter)
        var foodSum = sumList.find { it.categoryId == food.id }!!.sum
        var fitnessSum = sumList.find { it.categoryId == fitness.id }!!.sum

        assertEqualsBD(BigDecimal(5_500), foodSum)
        assertEqualsBD(BigDecimal(12_500), fitnessSum)

        statisticsFilter = StatisticsFilterDb(fromDate = fromDate, toDate = toDate)
        val sumListTo = statisticsRepository.getSumByCategory(profile.id!!, true, statisticsFilter)
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

        val firstSellerId = sellerRepository.getAllByProfileId(profile.id!!).first()!!.id!!

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


        val secondSellerId = sellerRepository.getAllByProfileId(profile.id!!)[1].id!!

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

        val countsNoFilter = statisticsRepository.getTransactionAndItemCount(profile!!.id!!, statisticsFilter)



        val categories = listOf(food, clothing, transport).map { it.id!! }
        statisticsFilter = StatisticsFilterDb(categoryIds = categories)
        val countsFilteredByCategory = statisticsRepository.getTransactionAndItemCount(profile.id!!, statisticsFilter)

        statisticsFilter = StatisticsFilterDb(currencyCode = Currency.getInstance("USD"))
        val countsFilteredByCurrency = statisticsRepository.getTransactionAndItemCount(profile.id!!, statisticsFilter)

        statisticsFilter = StatisticsFilterDb(currencyCode = Currency.getInstance("EUR"))
        val countsFilteredByCurrencyNotOwnedByOwner = statisticsRepository.getTransactionAndItemCount(profile.id!!, statisticsFilter)

        statisticsFilter = StatisticsFilterDb(currencyCode = Currency.getInstance("USD"), accountIds = listOf(newAccountId))
        val countsFilteredByCurrencyWithOverlappingAccounts = statisticsRepository.getTransactionAndItemCount(profile.id!!, statisticsFilter)

        statisticsFilter = StatisticsFilterDb(accountIds = listOf(accountId))
        val countsFilteredByAccount = statisticsRepository.getTransactionAndItemCount(profile.id!!, statisticsFilter)

        statisticsFilter = StatisticsFilterDb(weekDays = listOf(DayOfWeek.WEDNESDAY))
        val countsFilteredByWeekday = statisticsRepository.getTransactionAndItemCount(profile.id!!, statisticsFilter)
        statisticsFilter = StatisticsFilterDb(transactionModes = listOf(TransactionMode.Card))
        val countsFilteredByMode = statisticsRepository.getTransactionAndItemCount(profile.id!!, statisticsFilter)
        val firstSellerFirstLocationId = sellerLocationRepository.getByProfileIdAndSellerId(profile.id!!, firstSellerId).first().id!!
        val firstSellerSecondLocationId = sellerLocationRepository.getByProfileIdAndSellerId(profile.id!!, firstSellerId)[1].id!!

        val secondSellerLocationId =  sellerLocationRepository.getByProfileIdAndSellerId(profile.id!!, secondSellerId).first().id!!
        statisticsFilter = StatisticsFilterDb(sellerIds = listOf(firstSellerId))
        val countsFilteredBySeller = statisticsRepository.getTransactionAndItemCount(profile.id!!, statisticsFilter)

        statisticsFilter = StatisticsFilterDb(sellerIds = listOf(firstSellerId), sellerLocationIds = listOf(secondSellerLocationId))
        val countsFilteredBySellerWithOverlappingLocations = statisticsRepository.getTransactionAndItemCount(profile.id!!, statisticsFilter)

        statisticsFilter = StatisticsFilterDb(sellerLocationIds = listOf(firstSellerFirstLocationId, secondSellerLocationId))
        val countsFilteredByLocation = statisticsRepository.getTransactionAndItemCount(profile.id!!, statisticsFilter)

        assertEquals(TransactionAndItemCount(8, 13), countsNoFilter)
        assertEquals(TransactionAndItemCount(2, 4), countsFilteredByCategory)
        assertEquals(TransactionAndItemCount(2, 4), countsFilteredByWeekday)
        assertEquals(TransactionAndItemCount(8, 13), countsFilteredByCurrency)
        assertEquals(TransactionAndItemCount(2, 5), countsFilteredByAccount)
        assertEquals(TransactionAndItemCount(2, 3), countsFilteredByMode)
        assertEquals(TransactionAndItemCount(3, 3), countsFilteredBySeller)
        assertEquals(TransactionAndItemCount(2, 3), countsFilteredByLocation)

        assertEquals(TransactionAndItemCount(8, 13), countsFilteredByCurrencyWithOverlappingAccounts)
        assertEquals(TransactionAndItemCount(4, 5), countsFilteredBySellerWithOverlappingLocations)
        assertEquals(TransactionAndItemCount(0, 0), countsFilteredByCurrencyNotOwnedByOwner)
    }


    @Test
    fun totalSpentFiltersTest() {

        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()
        val dataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)

        val firstTransactionDate = LocalDate.parse("2025-01-01") //Wednesday
        val secondTransactionDate = LocalDate.parse("2025-01-02")
        val thirdTransactionDate = LocalDate.parse("2025-01-04") //Saturday
        val fourthTransactionDate = LocalDate.parse("2025-01-05")
        val fifthTransactionDate = LocalDate.parse("2025-01-06")

        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val fitness = categoryRepository.findByProfileIdAndStringId(profile.id!!, "fitness")
        val clothing = categoryRepository.findByProfileIdAndStringId(profile.id!!, "clothing")
        val utilities = categoryRepository.findByProfileIdAndStringId(profile.id!!, "utilities")


        dataBuilder.createTransaction().atDate(firstTransactionDate)
            .withItem("Basic Item", "miscellaneous", BigDecimal(110.00))
            .withDetailedItem().mainDetails("Basic Reduction", "miscellaneous", BigDecimal(100)).otherDetails(null, 1, null, null, true, null, null, null, null).build()
            .withItem("Dumbbells", "fitness", BigDecimal(400.00))
            .commit()
        val transactionBuilder = dataBuilder.createTransaction().atDate(secondTransactionDate)
            .withItem("Basic Item", "miscellaneous", BigDecimal(300.00))
            .withItem("Screwdriver", "utilities", BigDecimal(300.00))
            .withNewAccount("USD", "Test Account")
        val newAccountId = transactionBuilder.getAccount().id!!
        transactionBuilder.commit()
        dataBuilder.createTransaction().atDate(thirdTransactionDate)
            .withItem("Basic Item", "clothing", BigDecimal(450.00))
            .withMode(TransactionMode.Transfer)
            .commit()
        val trxBuilder = dataBuilder.createTransaction().atDate(fourthTransactionDate)
            .withItem("Basic Item", "miscellaneous", BigDecimal(750.00))
            .withNewSeller("The store")
        val sellerId = trxBuilder.getSeller()!!.id!!
        trxBuilder.commit()
        dataBuilder.createTransaction().atDate(fifthTransactionDate)
            .withItem("Basic Item", "utilities", BigDecimal(1000.00))
            .commit()

        var statisticsFilter = StatisticsFilterDb()
        val sumNoFilter = statisticsRepository.getTransactionItemDebitSum(profile.id!!, statisticsFilter)

        val accounts = listOf(newAccountId)
        statisticsFilter = StatisticsFilterDb(accountIds = accounts)
        val sumFilteredByAccount = statisticsRepository.getTransactionItemDebitSum(profile.id!!, statisticsFilter)

        val weekdays = listOf(DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY)
        statisticsFilter = StatisticsFilterDb(weekDays = weekdays)
        val sumFilteredByWeekdays = statisticsRepository.getTransactionItemDebitSum(profile.id!!, statisticsFilter)

        val categories = listOf(fitness!!.id!!, utilities!!.id!!, clothing!!.id!!)
        statisticsFilter = StatisticsFilterDb(categoryIds = categories)
        val sumFilteredByCategory = statisticsRepository.getTransactionItemDebitSum(profile.id!!, statisticsFilter)

        val modes = listOf(TransactionMode.Transfer)
        statisticsFilter = StatisticsFilterDb(transactionModes = modes)
        val sumFilteredByMode = statisticsRepository.getTransactionItemDebitSum(profile.id!!, statisticsFilter)

        val sellers = listOf(sellerId)
        statisticsFilter = StatisticsFilterDb(sellerIds = sellers)
        val sumFilteredBySeller = statisticsRepository.getTransactionItemDebitSum(profile.id!!, statisticsFilter)

        statisticsFilter = StatisticsFilterDb(fromDate = secondTransactionDate, toDate = fifthTransactionDate)
        val sumFilteredByDateRange = statisticsRepository.getTransactionItemDebitSum(profile.id!!, statisticsFilter)

        assertEqualsBD(BigDecimal(3310.00), sumNoFilter)
        assertEqualsBD(BigDecimal(600.00), sumFilteredByAccount)
        assertEqualsBD(BigDecimal(960.00), sumFilteredByWeekdays)
        assertEqualsBD(BigDecimal(2150.00), sumFilteredByCategory)
        assertEqualsBD(BigDecimal(450.00), sumFilteredByMode)
        assertEqualsBD(BigDecimal(750.00), sumFilteredBySeller)
        assertEqualsBD(BigDecimal(2800.00), sumFilteredByDateRange)
//        assertEqualsBD(BigDecimal(2800.00), sumFilteredByCurrency)
//        assertEqualsBD(BigDecimal(2800.00), sumFilteredByLocationIds)

    }

    /**
     * Essentially a mirror copy of the debit tests so I don't rely on assumptions
     */
    @Test
    fun totalEarnedFiltersTest() {


        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()
        val dataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)

        val firstTransactionDate = LocalDate.parse("2025-01-01")
        val secondTransactionDate = LocalDate.parse("2025-01-02")
        val thirdTransactionDate = LocalDate.parse("2025-01-04")
        val fourthTransactionDate = LocalDate.parse("2025-01-05")
        val fifthTransactionDate = LocalDate.parse("2025-01-06")


        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)!!

        val fitness = categoryRepository.findByProfileIdAndStringId(profile.id!!, "fitness")
        val clothing = categoryRepository.findByProfileIdAndStringId(profile.id!!, "clothing")
        val utilities = categoryRepository.findByProfileIdAndStringId(profile.id!!, "utilities")


        dataBuilder.createTransaction().atDate(firstTransactionDate)
            .debitOrCredit(false)
            .withItem("Basic Item", "miscellaneous", BigDecimal(110.00))
            .withDetailedItem().mainDetails("Basic Reduction", "miscellaneous", BigDecimal(100)).otherDetails(null, 1, null, null, true, null, null, null, null).build()
            .withItem("Dumbbells", "fitness", BigDecimal(400.00))
            .commit()
        val transactionBuilder = dataBuilder.createTransaction().atDate(secondTransactionDate)
            .debitOrCredit(false)
            .withItem("Basic Item", "miscellaneous", BigDecimal(300.00))
            .withItem("Screwdriver", "utilities", BigDecimal(300.00))
            .withNewAccount("USD", "Test Account")
        val newAccountId = transactionBuilder.getAccount().id!!
        transactionBuilder.commit()
        dataBuilder.createTransaction().atDate(thirdTransactionDate)
            .debitOrCredit(false)
            .withItem("Basic Item", "clothing", BigDecimal(450.00))
            .withMode(TransactionMode.Transfer)
            .commit()
        val trxBuilder = dataBuilder.createTransaction().atDate(fourthTransactionDate)
            .debitOrCredit(false)
            .withItem("Basic Item", "miscellaneous", BigDecimal(750.00))
            .withNewSeller("The store")
        val sellerId = trxBuilder.getSeller()!!.id!!
        trxBuilder.commit()
        dataBuilder.createTransaction().atDate(fifthTransactionDate)
            .debitOrCredit(false)
            .withItem("Basic Item", "utilities", BigDecimal(1000.00))
            .commit()

        var statisticsFilter = StatisticsFilterDb()
        val sumNoFilter = statisticsRepository.getTransactionItemCreditSum(profile.id!!, statisticsFilter)

        val accounts = listOf(newAccountId)
        statisticsFilter = StatisticsFilterDb(accountIds = accounts)
        val sumFilteredByAccount = statisticsRepository.getTransactionItemCreditSum(profile.id!!, statisticsFilter)

        val weekdays = listOf(DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY)
        statisticsFilter = StatisticsFilterDb(weekDays = weekdays)
        val sumFilteredByDateList = statisticsRepository.getTransactionItemCreditSum(profile.id!!, statisticsFilter)

        val categories = listOf(fitness!!.id!!, utilities!!.id!!, clothing!!.id!!)
        statisticsFilter = StatisticsFilterDb(categoryIds = categories)
        val sumFilteredByCategory = statisticsRepository.getTransactionItemCreditSum(profile.id!!, statisticsFilter)

        val modes = listOf(TransactionMode.Transfer)
        statisticsFilter = StatisticsFilterDb(transactionModes = modes)
        val sumFilteredByMode = statisticsRepository.getTransactionItemCreditSum(profile.id!!, statisticsFilter)

        val sellers = listOf(sellerId)
        statisticsFilter = StatisticsFilterDb(sellerIds = sellers)
        val sumFilteredBySeller = statisticsRepository.getTransactionItemCreditSum(profile.id!!, statisticsFilter)

        statisticsFilter = StatisticsFilterDb(fromDate = secondTransactionDate, toDate = fifthTransactionDate)
        val sumFilteredByDateRange = statisticsRepository.getTransactionItemCreditSum(profile.id!!, statisticsFilter)

        assertEqualsBD(BigDecimal(3310.00), sumNoFilter)
        assertEqualsBD(BigDecimal(600.00), sumFilteredByAccount)
        assertEqualsBD(BigDecimal(960.00), sumFilteredByDateList)
        assertEqualsBD(BigDecimal(2150.00), sumFilteredByCategory)
        assertEqualsBD(BigDecimal(450.00), sumFilteredByMode)
        assertEqualsBD(BigDecimal(750.00), sumFilteredBySeller)
        assertEqualsBD(BigDecimal(2800.00), sumFilteredByDateRange)

    }


    /**
     * I accidentally thought I had accidentally created an IDOR path through the seller ID,
     * but I've confirmed this doesn't happen through this test. Keep it here to mark my paranoia
     * 2026-11-09: UPDATE: and this test actually bailed me out with bug from a faulty JOIN clause I introduced. That's nice
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
        val user2Profile = ProfilePojo(null, user2Id, "Default Profile", user2ProfileUuid, 1_000, 1, utcDate)
        val user2ProfileId = profileRepository.insertProfile(user2Profile)

        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()

        val user1DataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)
        user1DataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(100))
            .withNewSeller("Restaurant")
            .commit()

        val user2DataBuilder = DataBuilder(folder, timeHandler, user2Uuid.toString(), Constants.DEFAULT_PROFILE_ID, testUtilComponent)
        val accountOrdinal: Short = 1
        val account = AccountPojo(null, user2ProfileId, "USD", null, null, "Default Account", accountOrdinal, 1_000, 1, utcDate)
        accountRepository.insertAccount(account)
        categoryRepository.insertCategory(CategoryPojo(null, user2ProfileId, "miscellaneous", FALSE_BYTE, null, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category", 1, 1_000, 1, utcDate))
        user2DataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(100))
            .withNewSeller("Hardware store")
            .commit()

        val user1Id = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!.id!!
        val user1ProfileId = profileRepository.findByUuid(user1Id, defaultUserProfileUuid)!!.id!!
        val user1Seller = sellerRepository.getAllByProfileId(user1ProfileId).first()!!.id!!
        val user2Seller = sellerRepository.getAllByProfileId(user2ProfileId).first()!!.id!!

        val statisticsFilter = StatisticsFilterDb(sellerIds = listOf(user1Seller, user2Seller))
        val count = statisticsRepository.getTransactionAndItemCount(user1ProfileId, statisticsFilter)
        assertEquals(TransactionAndItemCount(1, 1), count)
    }


}