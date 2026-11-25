package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.configuration.TestRunConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.entities.StatisticsFilterDb
import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.entities.dtos.TransactionAndItemCount
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
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Assert.assertEquals
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
        val utilities = categoryRepository.findByProfileIdAndStringId(profile.id!!, "utilities")!!
        val transportation = categoryRepository.findByProfileIdAndStringId(profile.id!!, "transportation")!!
        val clothing = categoryRepository.findByProfileIdAndStringId(profile.id!!, "clothing")!!
        val healthcare = categoryRepository.findByProfileIdAndStringId(profile.id!!, "healthcare")!!
        val household = categoryRepository.findByProfileIdAndStringId(profile.id!!, "household")!!

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
            .withItem("Bread", "food", BigDecimal(2))
            .withItem("Dumbbells", "fitness", BigDecimal(4))
            .withNewAccount("USD", "Test Account")
        val newAccountId = transactionBuilder.getAccount().id!!
        val id = transactionBuilder.commit().first()

        val id2 = dataBuilder.createTransaction()
            .atDate(secondDate)
            .withItem("Hammer", "utilities", BigDecimal(8))
            .withItem("Sweatpants", "fitness", BigDecimal(16))
            .commit().first()

        val id3 = dataBuilder.createTransaction()
            .atDate(thirdDate)
            .withItem("Fish", "food", BigDecimal(32))
            .withItem("Red shirt", "clothing", BigDecimal(64))
            .withItem("Fuel", "transportation", BigDecimal(128))
            .commit()

        dataBuilder.createTransaction()
            .atDate(forthDate)
            .withItem("Medicine", "healthcare", BigDecimal(256))
            .withNewSeller("Generic Hospital")
            .withNewAccount("USD", "Test Account 2")
            .commit()

        val firstSellerId = sellerRepository.getAllByProfileId(profile.id!!).first()!!.id!!

        dataBuilder.createTransaction()
            .atDate(fifthDate)
            .withItem("Vitamin C", "healthcare", BigDecimal(512))
            .withSeller(firstSellerId)
            .withNewSellerLocation("Brand New Branch")
            .withNewAccount("USD", "Test Account 3")
            .commit()

        dataBuilder.createTransaction()
            .atDate(sixthDate)
            .withItem("Bandages", "healthcare", BigDecimal(1024))
            .withSeller(firstSellerId)
            .withNewSellerLocation("New New Branch")
            .withNewAccount("USD", "Test Account 4")
            .commit()

        dataBuilder.createTransaction()
            .atDate(seventhDate)
            .withItem("Light bulb", "household", BigDecimal(2048))
            .withNewSeller("Generic Hardware Store")
            .withNewAccount("USD", "Test Account 5")
            .withMode(TransactionMode.Card)
            .commit()


        val secondSellerId = sellerRepository.getAllByProfileId(profile.id!!)[1].id!!

        dataBuilder.createTransaction()
            .atDate(eighthDate)
            .withItem("Paint", "household", BigDecimal(4096))
            .withItem("Light bulb", "household", BigDecimal(8192)) //This is based on Sidon sets, whose core characteristic is that no 2 elements produce the same sum. I know i'm summing more than just pairs, but the idea is to reduce the likeliness of a clash
            .withSeller(secondSellerId)
            .withNewSellerLocation("Somewhere out there")
            .withNewAccount("USD", "Test Account 6")
            .withMode(TransactionMode.Card)
            .commit()


        var statisticsFilter = StatisticsFilterDb()

        val sumListNoFilter = statisticsRepository.getSumByCategory(profile!!.id!!, true, statisticsFilter)
        assertEquals(7, sumListNoFilter.size)
        var foodSum = sumListNoFilter.find { it.categoryId == food.id }!!.sum
        var fitnessSum = sumListNoFilter.find { it.categoryId == fitness.id }!!.sum
        var utilitiesSum = sumListNoFilter.find { it.categoryId == utilities.id }!!.sum
        var transportationSum = sumListNoFilter.find { it.categoryId == transportation.id }!!.sum
        var clothingSum = sumListNoFilter.find { it.categoryId == clothing.id }!!.sum
        var healthcareSum = sumListNoFilter.find { it.categoryId == healthcare.id }!!.sum
        var householdSum = sumListNoFilter.find { it.categoryId == household.id }!!.sum
        assertEqualsBD(BigDecimal(34), foodSum)
        assertEqualsBD(BigDecimal(20), fitnessSum)
        assertEqualsBD(BigDecimal(8), utilitiesSum)
        assertEqualsBD(BigDecimal(128), transportationSum)
        assertEqualsBD(BigDecimal(64), clothingSum)
        assertEqualsBD(BigDecimal(1792), healthcareSum)
        assertEqualsBD(BigDecimal(14336), householdSum)


        val categories = listOf(food, clothing, transportation).map { it.id!! }
        statisticsFilter = StatisticsFilterDb(categoryIds = categories)
        val sumFilteredByCategory = statisticsRepository.getSumByCategory(profile.id!!, true, statisticsFilter)
        assertEquals(3, sumFilteredByCategory.size)
        foodSum = sumFilteredByCategory.find { it.categoryId == food.id }!!.sum
        transportationSum = sumFilteredByCategory.find { it.categoryId == transportation.id }!!.sum
        clothingSum = sumFilteredByCategory.find { it.categoryId == clothing.id }!!.sum
        assertEqualsBD(BigDecimal(34), foodSum)
        assertEqualsBD(BigDecimal(64), clothingSum)
        assertEqualsBD(BigDecimal(128), transportationSum)

        statisticsFilter = StatisticsFilterDb(currencyCode = Currency.getInstance("USD"))
        val sumFilteredByCurrency = statisticsRepository.getSumByCategory(profile.id!!, true, statisticsFilter)
        assertEquals(7, sumFilteredByCurrency.size)
        foodSum = sumFilteredByCurrency.find { it.categoryId == food.id }!!.sum
        fitnessSum = sumFilteredByCurrency.find { it.categoryId == fitness.id }!!.sum
        utilitiesSum = sumFilteredByCurrency.find { it.categoryId == utilities.id }!!.sum
        transportationSum = sumFilteredByCurrency.find { it.categoryId == transportation.id }!!.sum
        clothingSum = sumFilteredByCurrency.find { it.categoryId == clothing.id }!!.sum
        healthcareSum = sumFilteredByCurrency.find { it.categoryId == healthcare.id }!!.sum
        householdSum = sumFilteredByCurrency.find { it.categoryId == household.id }!!.sum
        assertEqualsBD(BigDecimal(34), foodSum)
        assertEqualsBD(BigDecimal(20), fitnessSum)
        assertEqualsBD(BigDecimal(8), utilitiesSum)
        assertEqualsBD(BigDecimal(128), transportationSum)
        assertEqualsBD(BigDecimal(64), clothingSum)
        assertEqualsBD(BigDecimal(1792), healthcareSum)
        assertEqualsBD(BigDecimal(14336), householdSum)

        statisticsFilter = StatisticsFilterDb(currencyCode = Currency.getInstance("EUR"))
        val sumFilteredByCurrencyNotOwnedByOwner = statisticsRepository.getSumByCategory(profile.id!!, true, statisticsFilter)
        assertEquals(0, sumFilteredByCurrencyNotOwnedByOwner.size)

        statisticsFilter = StatisticsFilterDb(currencyCode = Currency.getInstance("USD"), accountIds = listOf(newAccountId))
        val sumFilteredByCurrencyWithOverlappingAccounts = statisticsRepository.getSumByCategory(profile.id!!, true, statisticsFilter)
        assertEquals(7, sumFilteredByCurrency.size)
        foodSum = sumFilteredByCurrencyWithOverlappingAccounts.find { it.categoryId == food.id }!!.sum
        fitnessSum = sumFilteredByCurrencyWithOverlappingAccounts.find { it.categoryId == fitness.id }!!.sum
        utilitiesSum = sumFilteredByCurrencyWithOverlappingAccounts.find { it.categoryId == utilities.id }!!.sum
        transportationSum = sumFilteredByCurrencyWithOverlappingAccounts.find { it.categoryId == transportation.id }!!.sum
        clothingSum = sumFilteredByCurrencyWithOverlappingAccounts.find { it.categoryId == clothing.id }!!.sum
        healthcareSum = sumFilteredByCurrencyWithOverlappingAccounts.find { it.categoryId == healthcare.id }!!.sum
        householdSum = sumFilteredByCurrencyWithOverlappingAccounts.find { it.categoryId == household.id }!!.sum
        assertEqualsBD(BigDecimal(34), foodSum)
        assertEqualsBD(BigDecimal(20), fitnessSum)
        assertEqualsBD(BigDecimal(8), utilitiesSum)
        assertEqualsBD(BigDecimal(128), transportationSum)
        assertEqualsBD(BigDecimal(64), clothingSum)
        assertEqualsBD(BigDecimal(1792), healthcareSum)
        assertEqualsBD(BigDecimal(14336), householdSum)

        statisticsFilter = StatisticsFilterDb(accountIds = listOf(accountId))
        val sumFilteredByAccount = statisticsRepository.getSumByCategory(profile.id!!, true, statisticsFilter)
        assertEquals(5, sumFilteredByAccount.size)
        foodSum = sumFilteredByAccount.find { it.categoryId == food.id }!!.sum
        fitnessSum = sumFilteredByAccount.find { it.categoryId == fitness.id }!!.sum
        utilitiesSum = sumFilteredByCurrencyWithOverlappingAccounts.find { it.categoryId == utilities.id }!!.sum
        transportationSum = sumFilteredByCurrencyWithOverlappingAccounts.find { it.categoryId == transportation.id }!!.sum
        clothingSum = sumFilteredByCurrencyWithOverlappingAccounts.find { it.categoryId == clothing.id }!!.sum

        assertEqualsBD(BigDecimal(32), foodSum)
        assertEqualsBD(BigDecimal(16), fitnessSum)
        assertEqualsBD(BigDecimal(8), utilitiesSum)
        assertEqualsBD(BigDecimal(128), transportationSum)
        assertEqualsBD(BigDecimal(64), clothingSum)

        statisticsFilter = StatisticsFilterDb(weekDays = listOf(DayOfWeek.WEDNESDAY))
        val sumFilteredByWeekday = statisticsRepository.getSumByCategory(profile.id!!, true, statisticsFilter)
        assertEquals(3, sumFilteredByWeekday.size)
        foodSum = sumFilteredByWeekday.find { it.categoryId == food.id }!!.sum
        fitnessSum = sumFilteredByWeekday.find { it.categoryId == fitness.id }!!.sum
        householdSum = sumFilteredByWeekday.find { it.categoryId == household.id }!!.sum
        assertEqualsBD(BigDecimal(2), foodSum)
        assertEqualsBD(BigDecimal(4), fitnessSum)
        assertEqualsBD(BigDecimal(12288), householdSum)


        statisticsFilter = StatisticsFilterDb(transactionModes = listOf(TransactionMode.Card))
        val sumFilteredByMode = statisticsRepository.getSumByCategory(profile.id!!, true, statisticsFilter)
        assertEquals(1, sumFilteredByMode.size)
        householdSum = sumFilteredByMode.find { it.categoryId == household.id }!!.sum
        assertEqualsBD(BigDecimal(14336), householdSum)

        val firstSellerFirstLocationId = sellerLocationRepository.getByProfileIdAndSellerId(profile.id!!, firstSellerId).first().id!!
        val firstSellerSecondLocationId = sellerLocationRepository.getByProfileIdAndSellerId(profile.id!!, firstSellerId)[1].id!!

        val secondSellerLocationId =  sellerLocationRepository.getByProfileIdAndSellerId(profile.id!!, secondSellerId).first().id!!
        statisticsFilter = StatisticsFilterDb(sellerIds = listOf(firstSellerId))
        val sumFilteredBySeller = statisticsRepository.getSumByCategory(profile.id!!, true, statisticsFilter)
        assertEquals(1, sumFilteredBySeller.size)
        healthcareSum = sumFilteredByCurrencyWithOverlappingAccounts.find { it.categoryId == healthcare.id }!!.sum
        assertEqualsBD(BigDecimal(1792), healthcareSum)

        statisticsFilter = StatisticsFilterDb(sellerIds = listOf(firstSellerId), sellerLocationIds = listOf(firstSellerSecondLocationId, secondSellerLocationId))
        val sumFilteredBySellerWithOverlappingLocations = statisticsRepository.getSumByCategory(profile.id!!, true, statisticsFilter)
        assertEquals(2, sumFilteredBySellerWithOverlappingLocations.size)
        householdSum = sumFilteredBySellerWithOverlappingLocations.find { it.categoryId == household.id }!!.sum
        healthcareSum = sumFilteredBySellerWithOverlappingLocations.find { it.categoryId == healthcare.id }!!.sum
        assertEqualsBD(BigDecimal(1792), healthcareSum)
        assertEqualsBD(BigDecimal(12288), householdSum)

        statisticsFilter = StatisticsFilterDb(sellerLocationIds = listOf(firstSellerFirstLocationId, secondSellerLocationId))
        val sumFilteredByLocation = statisticsRepository.getSumByCategory(profile.id!!, true, statisticsFilter)
        assertEquals(2, sumFilteredBySellerWithOverlappingLocations.size)
        householdSum = sumFilteredByLocation.find { it.categoryId == household.id }!!.sum
        healthcareSum = sumFilteredByLocation.find { it.categoryId == healthcare.id }!!.sum
        assertEqualsBD(BigDecimal(512), healthcareSum)
        assertEqualsBD(BigDecimal(12288), householdSum)

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

        statisticsFilter = StatisticsFilterDb(sellerIds = listOf(firstSellerId), sellerLocationIds = listOf(firstSellerSecondLocationId, secondSellerLocationId))
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
        val account = AccountPojo(null, user2ProfileId, "USD", null, null, "Default Account", FALSE_BYTE, accountOrdinal, 1_000, 1, utcDate)
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

    /**
     * As of now, there are roughly 7 filter criteria with 2 additional special conditions for a total of 9 and 12 statistics endpoints. I'm not writing all 108. Instead, I'll
     * confirm that the common method they use works well with just one test
     */
    @Test
    fun applyFiltersTest() {
        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val user = userRepository.findByUuid(TestConstants.DEFAULT_USER_ID)!!
        val profile = profileRepository.findByUuid(user.id!!, Constants.DEFAULT_PROFILE_ID)

        val dataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)


        val fitness = categoryRepository.findByProfileIdAndStringId(profile!!.id!!, "fitness")!!
        val food = categoryRepository.findByProfileIdAndStringId(profile.id!!, "food")!!
        val utilities = categoryRepository.findByProfileIdAndStringId(profile.id!!, "utilities")!!
        val transportation = categoryRepository.findByProfileIdAndStringId(profile.id!!, "transportation")!!
        val clothing = categoryRepository.findByProfileIdAndStringId(profile.id!!, "clothing")!!
        val healthcare = categoryRepository.findByProfileIdAndStringId(profile.id!!, "healthcare")!!
        val household = categoryRepository.findByProfileIdAndStringId(profile.id!!, "household")!!

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
            .withItem("Bread", "food", BigDecimal(2))
            .withItem("Dumbbells", "fitness", BigDecimal(4))
            .withNewAccount("USD", "Test Account")
        val newAccountId = transactionBuilder.getAccount().id!!
        val id = transactionBuilder.commit().first()

        val id2 = dataBuilder.createTransaction()
            .atDate(secondDate)
            .withItem("Hammer", "utilities", BigDecimal(8))
            .withItem("Sweatpants", "fitness", BigDecimal(16))
            .commit().first()

        val id3 = dataBuilder.createTransaction()
            .atDate(thirdDate)
            .withItem("Fish", "food", BigDecimal(32))
            .withItem("Red shirt", "clothing", BigDecimal(64))
            .withItem("Fuel", "transportation", BigDecimal(128))
            .commit()

        dataBuilder.createTransaction()
            .atDate(forthDate)
            .withItem("Medicine", "healthcare", BigDecimal(256))
            .withNewSeller("Generic Hospital")
            .withNewAccount("USD", "Test Account 2")
            .commit()

        val firstSellerId = sellerRepository.getAllByProfileId(profile.id!!).first()!!.id!!

        dataBuilder.createTransaction()
            .atDate(fifthDate)
            .withItem("Vitamin C", "healthcare", BigDecimal(512))
            .withSeller(firstSellerId)
            .withNewSellerLocation("Brand New Branch")
            .withNewAccount("USD", "Test Account 3")
            .commit()

        dataBuilder.createTransaction()
            .atDate(sixthDate)
            .withItem("Bandages", "healthcare", BigDecimal(1024))
            .withSeller(firstSellerId)
            .withNewSellerLocation("New New Branch")
            .withNewAccount("USD", "Test Account 4")
            .commit()

        dataBuilder.createTransaction()
            .atDate(seventhDate)
            .withItem("Light bulb", "household", BigDecimal(2048))
            .withNewSeller("Generic Hardware Store")
            .withNewAccount("USD", "Test Account 5")
            .withMode(TransactionMode.Card)
            .commit()


        val secondSellerId = sellerRepository.getAllByProfileId(profile.id!!)[1].id!!

        dataBuilder.createTransaction()
            .atDate(eighthDate)
            .withItem("Paint", "household", BigDecimal(4096))
            .withItem("Light bulb 13W", "household", BigDecimal(8192))
            .withSeller(secondSellerId)
            .withNewSellerLocation("Somewhere out there")
            .withNewAccount("USD", "Test Account 6")
            .withMode(TransactionMode.Card)
            .commit()

        var statisticsFilter = StatisticsFilterDb()

        val itemsNoFilter = statisticsRepository.getItems(profile!!.id!!, statisticsFilter).map { it.description }


        statisticsFilter = StatisticsFilterDb(fromDate = thirdDate)
        val itemsFilteredByFromDate = statisticsRepository.getItems(profile.id!!, statisticsFilter).map { it.description }
        statisticsFilter = StatisticsFilterDb(toDate = sixthDate)
        val itemsFilteredByToDate = statisticsRepository.getItems(profile.id!!, statisticsFilter).map { it.description }

        val categories = listOf(food, clothing, transportation).map { it.id!! }
        statisticsFilter = StatisticsFilterDb(categoryIds = categories)
        val itemsFilteredByCategory = statisticsRepository.getItems(profile.id!!, statisticsFilter).map { it.description }

        statisticsFilter = StatisticsFilterDb(currencyCode = Currency.getInstance("USD"))
        val itemsFilteredByCurrency = statisticsRepository.getItems(profile.id!!, statisticsFilter).map { it.description }

        statisticsFilter = StatisticsFilterDb(currencyCode = Currency.getInstance("EUR"))
        val itemsFilteredByCurrencyNotOwnedByProfile = statisticsRepository.getItems(profile.id!!, statisticsFilter).map { it.description }

        statisticsFilter = StatisticsFilterDb(currencyCode = Currency.getInstance("USD"), accountIds = listOf(newAccountId))
        val itemsFilteredByCurrencyWithOverlappingAccounts = statisticsRepository.getItems(profile.id!!, statisticsFilter).map { it.description }

        statisticsFilter = StatisticsFilterDb(accountIds = listOf(accountId))
        val itemsFilteredByAccount = statisticsRepository.getItems(profile.id!!, statisticsFilter).map { it.description }

        statisticsFilter = StatisticsFilterDb(weekDays = listOf(DayOfWeek.WEDNESDAY))
        val itemsFilteredByWeekday = statisticsRepository.getItems(profile.id!!, statisticsFilter).map { it.description }
        statisticsFilter = StatisticsFilterDb(transactionModes = listOf(TransactionMode.Card))
        val itemsFilteredByMode = statisticsRepository.getItems(profile.id!!, statisticsFilter).map { it.description }
        val firstSellerFirstLocationId = sellerLocationRepository.getByProfileIdAndSellerId(profile.id!!, firstSellerId).first().id!!
        val firstSellerSecondLocationId = sellerLocationRepository.getByProfileIdAndSellerId(profile.id!!, firstSellerId)[1].id!!

        val secondSellerLocationId =  sellerLocationRepository.getByProfileIdAndSellerId(profile.id!!, secondSellerId).first().id!!
        statisticsFilter = StatisticsFilterDb(sellerIds = listOf(firstSellerId))
        val itemsFilteredBySeller = statisticsRepository.getItems(profile.id!!, statisticsFilter).map { it.description }

        statisticsFilter = StatisticsFilterDb(sellerIds = listOf(firstSellerId), sellerLocationIds = listOf(firstSellerSecondLocationId, secondSellerLocationId))
        val itemsFilteredBySellerWithOverlappingLocations = statisticsRepository.getItems(profile.id!!, statisticsFilter).map { it.description }

        statisticsFilter = StatisticsFilterDb(sellerLocationIds = listOf(firstSellerFirstLocationId, secondSellerLocationId))
        val itemsFilteredByLocation = statisticsRepository.getItems(profile.id!!, statisticsFilter).map { it.description }


        assertThat(itemsNoFilter, Matchers.containsInAnyOrder("Bread","Dumbbells","Hammer","Sweatpants","Fish","Red shirt","Fuel","Medicine","Vitamin C","Bandages","Light bulb","Paint","Light bulb 13W"))
        assertThat(itemsFilteredByFromDate, Matchers.containsInAnyOrder("Fish","Red shirt","Fuel","Medicine","Vitamin C","Bandages","Light bulb","Paint","Light bulb 13W"))
        assertThat(itemsFilteredByToDate, Matchers.containsInAnyOrder("Bread","Dumbbells","Hammer","Sweatpants","Fish","Red shirt","Fuel","Medicine","Vitamin C","Bandages"))
        assertThat(itemsFilteredByCategory, Matchers.containsInAnyOrder("Bread","Fish","Red shirt","Fuel"))
        assertThat(itemsFilteredByWeekday, Matchers.containsInAnyOrder("Bread","Dumbbells","Paint","Light bulb 13W"))
        assertThat(itemsFilteredByCurrency, Matchers.containsInAnyOrder("Bread","Dumbbells","Hammer","Sweatpants","Fish","Red shirt","Fuel","Medicine","Vitamin C","Bandages","Light bulb","Paint","Light bulb 13W"))
        assertThat(itemsFilteredByAccount, Matchers.containsInAnyOrder("Hammer","Sweatpants","Fish","Red shirt","Fuel"))
        assertThat(itemsFilteredByMode, Matchers.containsInAnyOrder("Light bulb","Paint","Light bulb 13W"))
        assertThat(itemsFilteredBySeller, Matchers.containsInAnyOrder("Medicine","Vitamin C","Bandages"))
        assertThat(itemsFilteredByLocation, Matchers.containsInAnyOrder("Vitamin C","Paint","Light bulb 13W"))


        assertThat(itemsFilteredByCurrencyWithOverlappingAccounts, Matchers.containsInAnyOrder("Bread","Dumbbells","Hammer","Sweatpants","Fish","Red shirt","Fuel","Medicine","Vitamin C","Bandages","Light bulb","Paint","Light bulb 13W"))
        assertThat(itemsFilteredBySellerWithOverlappingLocations, Matchers.containsInAnyOrder("Medicine","Vitamin C","Bandages","Paint","Light bulb 13W"))
        assertThat(itemsFilteredByCurrencyNotOwnedByProfile, `is`(empty()))
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StatisticsRepositoryTest::class.java)
    }

}