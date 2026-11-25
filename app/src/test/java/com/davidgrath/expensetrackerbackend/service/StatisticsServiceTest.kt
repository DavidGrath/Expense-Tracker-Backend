package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TestRunConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.entities.StatisticsFilterDb
import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.AccountPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.CategoryPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ProfilePojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.UserPojo
import com.davidgrath.expensetrackerbackend.generatedapi.model.StatisticsFilter
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.CategoryRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.repository.SellerLocationRepository
import com.davidgrath.expensetrackerbackend.repository.SellerRepository
import com.davidgrath.expensetrackerbackend.repository.TransactionRepository
import com.davidgrath.expensetrackerbackend.repository.UserRepository
import com.davidgrath.expensetrackerbackend.service.StatisticsService.ValidateStatisticsFilterResult
import com.davidgrath.expensetrackerbackend.service.exceptions.EntityNotFoundException
import com.davidgrath.expensetrackerbackend.service.exceptions.InvalidDateRangeException
import com.davidgrath.expensetrackerbackend.service.exceptions.StatisticsHeterogeneousCurrencyCodeException
import com.davidgrath.expensetrackerbackend.testutils.DataBuilder
import com.davidgrath.expensetrackerbackend.testutils.TestConstants
import com.davidgrath.expensetrackerbackend.testutils.TestData
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.assertEqualsBD
import com.davidgrath.expensetrackerbackend.testutils.getDefaultAccountId
import com.davidgrath.expensetrackerbackend.testutils.getDefaultProfileId
import com.davidgrath.expensetrackerbackend.testutils.tempInit
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.FALSE_BYTE
import com.davidgrath.expensetrackerbackend.utils.uuidToBytes
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Assert.*
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
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

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
    @Autowired
    lateinit var categoryRepository: CategoryRepository
    @Autowired
    lateinit var sellerRepository: SellerRepository
    @Autowired
    lateinit var sellerLocationRepository: SellerLocationRepository
    @Autowired
    lateinit var transactionService: TransactionService

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

        assertEquals(ValidateStatisticsFilterResult.Valid, statisticsService.validateFilter(profileId, statisticsFilter))
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

        assertEquals(ValidateStatisticsFilterResult.Valid, statisticsService.validateFilter(profileId, statisticsFilter))
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
        val account = AccountPojo(null, profileId, "GBP", null, null, "Account name", FALSE_BYTE, 2, 2_000, 2, utcDate)
        val newAccountId = accountRepository.insertAccount(account)
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)

        val currency = "USD"
        val statisticsFilter = StatisticsFilter().accounts(listOf(accountId, newAccountId)).currencyCode(currency)

        assertEquals(ValidateStatisticsFilterResult.Valid, statisticsService.validateFilter(profileId, statisticsFilter))
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
        val account = AccountPojo(null, profileId, "GBP", null, null, "Account name", FALSE_BYTE, 2, 2_000, 2, utcDate)
        val newAccountId = accountRepository.insertAccount(account)
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val statisticsFilter = StatisticsFilter().accounts(listOf(accountId, newAccountId))

        assertThrows(StatisticsHeterogeneousCurrencyCodeException::class.java, {
            statisticsService.validateFilter(profileId, statisticsFilter)
        })
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

        assertEquals(ValidateStatisticsFilterResult.Valid, statisticsService.validateFilter(profileId, statisticsFilter))
    }

    /**
     * ACR-6
     */
    @Test
    fun givenAccountListIsEmptyAndAccountListDoesNotHaveHeterogeneousCurrenciesAndCurrencyIsNullWhenValidateThenTrue() {
        val clock = timeHandler.getClock()
        val date = ZonedDateTime.now(clock)
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()

        val profileId = getDefaultProfileId(userRepository, profileRepository)
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)

        val statisticsFilter = StatisticsFilter()

        assertEquals(ValidateStatisticsFilterResult.Valid, statisticsService.validateFilter(profileId, statisticsFilter))
    }

    @Test
    fun givenSellerIsNotForProfileWhenValidateThenFail() {
        val clock = timeHandler.getClock()
        val date = ZonedDateTime.now(clock)
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()
        val user2Uuid = UUID.randomUUID()
        val user2UuidBytes = uuidToBytes(user2Uuid)
        val user2Id = userRepository.insertUser(UserPojo(null, user2UuidBytes,"user2", "password", "", utcDate))
        val defaultUserProfileUuid = uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID))
        val defaultUserAccount = getDefaultAccountId(userRepository, profileRepository, accountRepository)
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
        val account = AccountPojo(null, user2ProfileId, "USD", null, null, "Default Account", FALSE_BYTE, 1, 1_000, 1, utcDate)
        accountRepository.insertAccount(account)
        categoryRepository.insertCategory(CategoryPojo(null, user2ProfileId, "miscellaneous", FALSE_BYTE, null, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category", 1, 1_000, 1, utcDate))
        user2DataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(100))
            .withNewSeller("Hardware store")
            .commit()

        val user1ProfileId = getDefaultProfileId(userRepository, profileRepository)
        val user1Seller = sellerRepository.getAllByProfileId(user1ProfileId).first()!!.id!!
        val user2Seller = sellerRepository.getAllByProfileId(user2ProfileId).first()!!.id!!

        val statisticsFilter = StatisticsFilter().sellers(listOf(user1Seller, user2Seller)).accounts(listOf(defaultUserAccount))
        assertThrows(EntityNotFoundException::class.java, {
            statisticsService.validateFilter(user1ProfileId, statisticsFilter)
        })
    }


    @Test
    fun givenAccountIsNotForProfileWhenValidateThenFail() {
        val clock = timeHandler.getClock()
        val date = ZonedDateTime.now(clock)
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()
        val user2Uuid = UUID.randomUUID()
        val user2UuidBytes = uuidToBytes(user2Uuid)
        val user2Id = userRepository.insertUser(UserPojo(null, user2UuidBytes,"user2", "password", "", utcDate))
        val defaultUserProfileUuid = uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID))
        val defaultUserAccount = getDefaultAccountId(userRepository, profileRepository, accountRepository)
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
        val account = AccountPojo(null, user2ProfileId, "USD", null, null, "Default Account", FALSE_BYTE, 1, 1_000, 1,utcDate)
        val user2AccountId = accountRepository.insertAccount(account)
        categoryRepository.insertCategory(CategoryPojo(null, user2ProfileId, "miscellaneous", FALSE_BYTE, null, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category", 1, 1_000, 1, utcDate, ))
        user2DataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(100))
            .withNewSeller("Hardware store")
            .commit()

        val user1ProfileId = getDefaultProfileId(userRepository, profileRepository)


        val statisticsFilter = StatisticsFilter().accounts(listOf(defaultUserAccount, user2AccountId))
        assertThrows(EntityNotFoundException::class.java, {
            statisticsService.validateFilter(user1ProfileId, statisticsFilter)
        })
    }


    @Test
    fun givenCategoryIsNotForProfileWhenValidateThenFail() {
        val clock = timeHandler.getClock()
        val date = ZonedDateTime.now(clock)
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()
        val user2Uuid = UUID.randomUUID()
        val user2UuidBytes = uuidToBytes(user2Uuid)
        val user2Id = userRepository.insertUser(UserPojo(null, user2UuidBytes,"user2", "password", "", utcDate))
        val defaultUserProfileUuid = uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID))
        val defaultUserAccount = getDefaultAccountId(userRepository, profileRepository, accountRepository)
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
        val account = AccountPojo(null, user2ProfileId, "USD", null, null, "Default Account", FALSE_BYTE, 1, 1_000, 1, utcDate)
        val user2AccountId = accountRepository.insertAccount(account)
        val user2CategoryId = categoryRepository.insertCategory(CategoryPojo(null, user2ProfileId, "miscellaneous", FALSE_BYTE, null, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category", 1, 1_000, 1, utcDate, ))
        user2DataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(100))
            .withNewSeller("Hardware store")
            .commit()

        val user1ProfileId = getDefaultProfileId(userRepository, profileRepository)


        val statisticsFilter = StatisticsFilter().accounts(listOf(defaultUserAccount)).categories(listOf(user2CategoryId))
        assertThrows(EntityNotFoundException::class.java, {
            statisticsService.validateFilter(user1ProfileId, statisticsFilter)
        })
    }


    @Test
    fun givenSellerLocationIsNotForProfileWhenValidateThenFail() {
        val clock = timeHandler.getClock()
        val date = ZonedDateTime.now(clock)
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()
        val user2Uuid = UUID.randomUUID()
        val user2UuidBytes = uuidToBytes(user2Uuid)
        val user2Id = userRepository.insertUser(UserPojo(null, user2UuidBytes,"user2", "password", "", utcDate))
        val defaultUserProfileUuid = uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID))
        val defaultUserAccount = getDefaultAccountId(userRepository, profileRepository, accountRepository)
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
        val account = AccountPojo(null, user2ProfileId, "USD", null, null, "Default Account", FALSE_BYTE, 1, 1_000, 1, utcDate)
        accountRepository.insertAccount(account)
        categoryRepository.insertCategory(CategoryPojo(null, user2ProfileId, "miscellaneous", FALSE_BYTE, null, "${Constants.ICON_NAMESPACE_MATERIAL_SYMBOLS}:category", 1, 1_000, 1, utcDate, ))
        user2DataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(100))
            .withNewSeller("Hardware store")
            .withNewSellerLocation("Branch in the city")
            .commit()

        val user1ProfileId = getDefaultProfileId(userRepository, profileRepository)
        val user1Seller = sellerRepository.getAllByProfileId(user1ProfileId).first()!!.id!!
        val user2Seller = sellerRepository.getAllByProfileId(user2ProfileId).first()!!.id!!
        val user2SellerFirstLocation = sellerLocationRepository.getByProfileIdAndSellerId(user2ProfileId, user2Seller).first()!!.id!!

        val statisticsFilter = StatisticsFilter().accounts(listOf(defaultUserAccount)).sellerLocations(listOf(user2SellerFirstLocation))
        assertThrows(EntityNotFoundException::class.java, {
            statisticsService.validateFilter(user1ProfileId, statisticsFilter)
        })


    }

    @Test
    fun getItemsTest() {
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

        val firstDate = LocalDate.parse("2025-01-01") // Wednesday
        val secondDate = LocalDate.parse("2025-01-02")
        val thirdDate = LocalDate.parse("2025-01-03")
        val forthDate = LocalDate.parse("2025-01-04")

        val transactionBuilder = dataBuilder.createTransaction()
            .atDate(firstDate)
            .withItem("Bread", "food", BigDecimal(2))
            .withDetailedItem().mainDetails("Dumbbells", "fitness", BigDecimal(4))
            .withImages(TestData.Resource.Images.HAMMER).build()
            .withNewAccount("USD", "Test Account")
        val newAccountId = transactionBuilder.getAccount().id!!
        val id = transactionBuilder.commit().first()

        val id2 = dataBuilder.createTransaction()
            .atDate(secondDate)
            .withDetailedItem().mainDetails("Hammer", "utilities", BigDecimal(8))
            .withImages(TestData.Resource.Images.HAMMER).build()
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


        var statisticsFilter = StatisticsFilter()
        val baseUrl = Triple("http", "localhost", 8080)
        val transactionsNoFilter = statisticsService.getStatisticsTransactions(profile.id!!, statisticsFilter, baseUrl)
        assertEquals(4, transactionsNoFilter.size)
        assertNotNull(transactionsNoFilter[0].items[1].firstImage)
        assertNotNull(transactionsNoFilter[1].items[0].firstImage)
        val items = transactionsNoFilter.flatMap { it.items }
        assertEquals(8, items.size)

        statisticsFilter = StatisticsFilter().categories(listOf(utilities.id!!, fitness.id!!, transportation.id!!))
        val transactionsFilteredByCategory = statisticsService.getStatisticsTransactions(profile.id!!, statisticsFilter, baseUrl)
        assertEquals(3, transactionsFilteredByCategory.size)
        assertEqualsBD(BigDecimal(2), transactionsFilteredByCategory[0].excludedItemAmount!!)
        assertEqualsBD(BigDecimal(0), transactionsFilteredByCategory[1].excludedItemAmount!!)
        assertEqualsBD(BigDecimal(96), transactionsFilteredByCategory[2].excludedItemAmount!!)

        assertEquals(1, transactionsFilteredByCategory[0].excludedItemCount)
        assertEquals(0, transactionsFilteredByCategory[1].excludedItemCount)
        assertEquals(2, transactionsFilteredByCategory[2].excludedItemCount) //TODO Add charges and test as well

    }

    @Test
    fun givenFromDateNotNullAndToDateNotNullAndFromDateGreaterThanToDateWhenValidateFilterThenFail() {
        val clock = timeHandler.getClock()
        val date = ZonedDateTime.now(clock)
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()

        val profileId = getDefaultProfileId(userRepository, profileRepository)
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)

        val statisticsFilter = StatisticsFilter().from(LocalDate.parse("2025-06-30")).to(LocalDate.parse("2026-06-29"))

        assertThrows(InvalidDateRangeException::class.java) {
            statisticsService.validateFilter(profileId, statisticsFilter)
        }
    }


    @Test
    fun givenTransactionRangeDoesNotHaveDatesInIntervalWhenFetchTotalThenDatesIntervalHaveZeroes() {
        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()
        val dataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)

        val user = userRepository.findByUuid(uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)))!!
        val profile = profileRepository.findByUuid(user.id!!, uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID)))!!
        val profileId = profile.id!!

        val firstTransactionDate = LocalDate.parse("2025-01-02")
        val secondTransactionDate = LocalDate.parse("2025-01-02")
        val thirdTransactionDate = LocalDate.parse("2025-01-07")
        val fourthTransactionDate = LocalDate.parse("2025-01-09")
//        val fifthTransactionDate = LocalDate.parse("2025-01-11")
        val fifthTransactionDate = LocalDate.parse("2025-07-11")

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        //Empty Set
        var statisticsFilter = StatisticsFilterDb(fromDate = LocalDate.parse("2025-07-01"))
        var transactionSumByDates = statisticsService.getTotalAmountByDate(profileId, true, statisticsFilter)
        assertEquals(0, transactionSumByDates.size)

        //Today and empty
        statisticsFilter = StatisticsFilterDb(fromDate = LocalDate.parse("2025-06-30"))
        transactionSumByDates = statisticsService.getTotalAmountByDate(profileId, true, statisticsFilter)
        assertEquals(1, transactionSumByDates.size)
        assertEqualsBD(BigDecimal.ZERO, transactionSumByDates.first().sum)

        //Multiple days and empty
        statisticsFilter = StatisticsFilterDb(fromDate = LocalDate.parse("2025-06-01"))
        transactionSumByDates = statisticsService.getTotalAmountByDate(profileId, true, statisticsFilter)
        val daysInJune = 30
        var grandSum = transactionSumByDates.map { it.sum }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }
        assertEquals(daysInJune, transactionSumByDates.size)
        assertEqualsBD(BigDecimal.ZERO, grandSum)

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
            .withItem("Description", "miscellaneous", BigDecimal(1000.00))
            .atDate(fifthTransactionDate)
            .commit()


        statisticsFilter = StatisticsFilterDb(fromDate = LocalDate.parse("2025-01-02"))
        transactionSumByDates = statisticsService.getTotalAmountByDate(profileId, true, statisticsFilter)
        val sum1 = transactionSumByDates.find { it.aggregateDate == firstTransactionDate }!!
        var missingSum1 = transactionSumByDates.find { it.aggregateDate == LocalDate.parse("2025-01-03") }!!
        val missingSum2 = transactionSumByDates.find { it.aggregateDate == LocalDate.parse("2025-01-08") }!!
//        val missingSum3 = transactionSumByDates.find { it.aggregateDate == LocalDate.parse("2025-01-10") }!!
        assertEqualsBD(BigDecimal(410), sum1.sum)
        assertEqualsBD(BigDecimal.ZERO, missingSum1.sum)
        assertEqualsBD(BigDecimal.ZERO, missingSum2.sum)
//        assertEqualsBD(BigDecimal.ZERO, missingSum3.sum)  No longer the expected behavior

        statisticsFilter = StatisticsFilterDb(fromDate = firstTransactionDate, toDate = thirdTransactionDate)
        transactionSumByDates = statisticsService.getTotalAmountByDate(profileId, true, statisticsFilter)
        val daysBetweenFirstAndThirdTransactionInclusive = 6

        grandSum = transactionSumByDates.map { it.sum }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }
        missingSum1 = transactionSumByDates.find { it.aggregateDate == LocalDate.parse("2025-01-03") }!!

        assertEquals(daysBetweenFirstAndThirdTransactionInclusive, transactionSumByDates.size)
        assertEqualsBD(BigDecimal(860), grandSum)
        assertEqualsBD(BigDecimal.ZERO, missingSum1.sum)
    }


    @Test
    fun intervalCumSumTest() {
        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()
        val dataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)

        val user = userRepository.findByUuid(uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)))!!
        val profile = profileRepository.findByUuid(user.id!!, uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID)))!!
        val profileId = profile.id!!
        val firstTransactionDate = LocalDate.parse("2025-01-02")
        val secondTransactionDate = LocalDate.parse("2025-01-02")
        val thirdTransactionDate = LocalDate.parse("2025-01-07")
        val fourthTransactionDate = LocalDate.parse("2025-01-09")
//        val fifthTransactionDate = LocalDate.parse("2025-01-11")
        val fifthTransactionDate = LocalDate.parse("2025-07-11")

        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        //Empty Set
        var statisticsFilter = StatisticsFilterDb(fromDate = LocalDate.parse("2025-07-01"))
        var transactionSumByDates = transactionService.getCumSumByDate(profileId, true, statisticsFilter)
        assertEquals(0, transactionSumByDates.size)

        //Today and empty
        statisticsFilter = StatisticsFilterDb(fromDate = LocalDate.parse("2025-06-30"))
        transactionSumByDates = transactionService.getCumSumByDate(profileId, true, statisticsFilter)
        assertEquals(1, transactionSumByDates.size)
        assertEqualsBD(BigDecimal.ZERO, transactionSumByDates.first().sum)

        //Multiple days and empty
        statisticsFilter = StatisticsFilterDb(fromDate = LocalDate.parse("2025-06-01"))
        transactionSumByDates = transactionService.getCumSumByDate(profileId, true, statisticsFilter)
        val daysInJune = 30
        statisticsFilter = StatisticsFilterDb(fromDate = LocalDate.parse("2025-06-01"))
        var regularSumByDates = statisticsService.getTotalAmountByDate(profileId, true, statisticsFilter)
        var grandSumRegular = regularSumByDates.map { it.sum }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }
        var grandSumCumulative = transactionSumByDates.last().sum
        assertEquals(daysInJune, transactionSumByDates.size)
        assertEqualsBD(grandSumRegular, grandSumCumulative)
        assertEqualsBD(BigDecimal.ZERO, grandSumCumulative)

        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(110.00))
            .atDate(firstTransactionDate)
            .commit()
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(300.00))
            .atDate(secondTransactionDate)
            .commit()

        //Just 1 transaction
        statisticsFilter = StatisticsFilterDb(fromDate = LocalDate.parse("2025-01-02"))
        transactionSumByDates = transactionService.getCumSumByDate(profileId, true, statisticsFilter)
//        val daysBetweenJan2AndJun30Inclusive = 180 //This behaviour is no longer valid based on the decision table GTT. CumSum is effectively a copy of this function. TODO Write the appropriate tests
//        assertEquals(daysBetweenJan2AndJun30Inclusive, transactionSumByDates.size)
        assertEquals(1, transactionSumByDates.size)

        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(450.00))
            .atDate(thirdTransactionDate)
            .commit()
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(750.00))
            .atDate(fourthTransactionDate)
            .commit()
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(1000.00))
            .atDate(fifthTransactionDate)
            .commit()


        statisticsFilter = StatisticsFilterDb(fromDate = LocalDate.parse("2025-01-02"))
        transactionSumByDates = transactionService.getCumSumByDate(profileId, true, statisticsFilter)
        val sum1 = transactionSumByDates.find { it.aggregateDate == firstTransactionDate }!!
        var missingSum1 = transactionSumByDates.find { it.aggregateDate == LocalDate.parse("2025-01-03") }!!
        val missingSum2 = transactionSumByDates.find { it.aggregateDate == LocalDate.parse("2025-01-08") }!!
//        val missingSum3 = transactionSumByDates.find { it.aggregateDate == LocalDate.parse("2025-01-10") }!!
        assertEqualsBD(BigDecimal(410), sum1.sum)
        assertEqualsBD(BigDecimal(410), missingSum1.sum)
        assertEqualsBD(BigDecimal(860), missingSum2.sum)
//        assertEqualsBD(BigDecimal(1610), missingSum3.sum)

        statisticsFilter = StatisticsFilterDb(fromDate = firstTransactionDate, toDate = thirdTransactionDate)
        transactionSumByDates = transactionService.getCumSumByDate(profileId, true, statisticsFilter)
        val daysBetweenFirstAndThirdTransactionInclusive = 6
        statisticsFilter = StatisticsFilterDb(fromDate = firstTransactionDate, toDate = thirdTransactionDate)
        regularSumByDates = statisticsService.getTotalAmountByDate(profileId, true, statisticsFilter)
        grandSumRegular = regularSumByDates.map { it.sum }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }
        grandSumCumulative = transactionSumByDates.last().sum
        missingSum1 = transactionSumByDates.find { it.aggregateDate == LocalDate.parse("2025-01-03") }!!

        assertEquals(daysBetweenFirstAndThirdTransactionInclusive, transactionSumByDates.size)
        assertEqualsBD(grandSumRegular, grandSumCumulative)
        assertEqualsBD(BigDecimal(860), grandSumCumulative)
        assertEqualsBD(BigDecimal(410), missingSum1.sum)
    }


    /**
     * GTT-5,GTT-6, GTT-7,GTT-8,GTT-13,GTT-14
     */
    @Test
    fun givenDateRangeInFutureWhenGetTotalByDateThenEmptySet() {
        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()
        val dataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)
        val transactionDate = LocalDate.parse("2025-01-01")

        val user = userRepository.findByUuid(uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)))!!
        val profile = profileRepository.findByUuid(user.id!!, uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID)))!!
        val profileId = profile.id!!

        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(110.00))
            .atDate(transactionDate)
            .commit()

        val dateAfterToday = LocalDate.parse("2025-07-01")
        val statisticsFilter = StatisticsFilterDb(fromDate = dateAfterToday)
        val transactionSumByDates = statisticsService.getTotalAmountByDate(profileId, true, statisticsFilter)
        assertEquals(0, transactionSumByDates.size)
    }

    /**
     * GTT-9
     */
    @Test
    fun givenToDateIsNullAndNoTransactionIsWithinRangeFromStartTillCurrentDateWhenGetTransactionsThenAllZeroes() {
        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()

        val user = userRepository.findByUuid(uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)))!!
        val profile = profileRepository.findByUuid(user.id!!, uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID)))!!
        val profileId = profile.id!!

        //Multiple days and empty
        var startDate = LocalDate.parse("2025-06-01")
        val today = LocalDate.parse("2025-06-30")
        val endDate = today
        val statisticsFilter = StatisticsFilterDb(fromDate = startDate, toDate = endDate)
        val transactionSumByDates = statisticsService.getTotalAmountByDate(profileId, true, statisticsFilter)
        val daysInJune = 30
        var grandSum = transactionSumByDates.map { it.sum }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }
        assertEquals(daysInJune, transactionSumByDates.size)
        assertEqualsBD(BigDecimal.ZERO, grandSum)
    }



    /**
     * GTT-10
     */
    @Test
    fun givenToDateIsNullAndAtLeast1TransactionIsWithinRangeFromNonNullStartTillCurrentDateWhenGetTransactionsThenMaxDateIsUsedAndDataReturned() {

        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()
        val dataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)
        val transactionDate = LocalDate.parse("2025-01-01")

        val user = userRepository.findByUuid(uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)))!!
        val profile = profileRepository.findByUuid(user.id!!, uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID)))!!
        val profileId = profile.id!!

        val dateInNormalRange = LocalDate.parse("2025-06-01")
        val dateInFuture = LocalDate.parse("2025-07-01")



        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(110.00))
            .atDate(dateInNormalRange)
            .commit()
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(300.00))
            .atDate(dateInFuture)
            .commit()


        val startDate = LocalDate.parse("2025-06-01")
        val statisticsFilter = StatisticsFilterDb(fromDate = startDate)
        val transactionSumByDates = statisticsService.getTotalAmountByDate(profileId, true, statisticsFilter)
        val grandSum = transactionSumByDates.map { it.sum }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }
        assertEquals(1, transactionSumByDates.size)
        val today = LocalDate.parse("2025-06-30")
        assertEquals(dateInNormalRange, transactionSumByDates.first().aggregateDate)
        val sumOnDateOutsideRange = transactionSumByDates.find { it.aggregateDate == dateInFuture }
        assertEqualsBD(BigDecimal(110), transactionSumByDates.first().sum)
        assertNull(sumOnDateOutsideRange)
    }

    /**
     * GTT-17,GTT-19,GTT-25
     */
    @Test
    fun givenFromDateIsNullAndThereIsNoTransactionBeforeCurrentDateWhenGetTransactionsByDateThenEmptyListReturned() {


        val user = userRepository.findByUuid(uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)))!!
        val profile = profileRepository.findByUuid(user.id!!, uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID)))!!
        val profileId = profile.id!!

        val today = LocalDate.parse("2025-06-30")
        val endDate = today
        val statisticsFilter = StatisticsFilterDb(toDate = endDate)
        val transactionSumByDates = statisticsService.getTotalAmountByDate(profileId, true, statisticsFilter)
        MatcherAssert.assertThat(transactionSumByDates, `is`(empty()))

    }



    /**
     * GTT-18
     */
    @Test
    fun givenFromDateIsNullAndAtLeast1TransactionIsWithinRangeTillCurrentDateThenMinDateIsUsedAndDataReturned() {

        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()
        val dataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)

        val user = userRepository.findByUuid(uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)))!!
        val profile = profileRepository.findByUuid(user.id!!, uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID)))!!
        val profileId = profile.id!!

        val dateInNormalRange = LocalDate.parse("2025-01-02")
        val dateInFuture = LocalDate.parse("2025-07-01")



        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(110.00))
            .atDate(dateInNormalRange)
            .commit()
        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(300.00))
            .atDate(dateInFuture)
            .commit()


        val today = LocalDate.parse("2025-06-30")
        val statisticsFilter = StatisticsFilterDb(toDate = today)
        val transactionSumByDates = statisticsService.getTotalAmountByDate(profileId, true, statisticsFilter)
        val grandSum = transactionSumByDates.map { it.sum }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }

        val daysBetweenJan2AndJun30Inclusive = 180
        assertEquals(daysBetweenJan2AndJun30Inclusive, transactionSumByDates.size)
        assertEquals(today, transactionSumByDates.last().aggregateDate)
        val firstSummary = transactionSumByDates.firstOrNull()
        assertEquals(dateInNormalRange, firstSummary?.aggregateDate)
        assertEqualsBD(BigDecimal(110), firstSummary?.sum!!)
        val sumOnDateOutsideRange = transactionSumByDates.find { it.aggregateDate == dateInFuture }
        assertEqualsBD(BigDecimal(110), grandSum)
        assertNull(sumOnDateOutsideRange)
    }


    /**
     * GTT-26
     */
    @Test
    fun givenFromAndToDateAreNullAndAtLeast1TransactionIsWithinRangeTillCurrentDateThenMinAndMaxUsedAndDataReturned() {

        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()
        val dataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)

        val user = userRepository.findByUuid(uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)))!!
        val profile = profileRepository.findByUuid(user.id!!, uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID)))!!
        val profileId = profile.id!!

        val firstTransactionDate = LocalDate.parse("2025-01-02")
        val secondTransactionDate = LocalDate.parse("2025-01-31")
        val dateInFuture = LocalDate.parse("2025-07-01")


        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(110.00))
            .atDate(firstTransactionDate)
            .commit()

        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(300.00))
            .atDate(secondTransactionDate)
            .commit()

        dataBuilder.createTransaction()
            .withItem("Description", "miscellaneous", BigDecimal(400.00))
            .atDate(dateInFuture)
            .commit()


        val statisticsFilter = StatisticsFilterDb(fromDate = null, toDate = null)
        val transactionSumByDates = statisticsService.getTotalAmountByDate(profileId, true, statisticsFilter)
        val grandSum = transactionSumByDates.map { it.sum }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }

        val daysBetweenJan2AndJan31Inclusive = 30
        assertEquals(daysBetweenJan2AndJan31Inclusive, transactionSumByDates.size)
        assertEquals(firstTransactionDate, transactionSumByDates.first().aggregateDate)
        assertEquals(secondTransactionDate, transactionSumByDates.last().aggregateDate)

        assertEqualsBD(BigDecimal(110), transactionSumByDates.first().sum)
        assertEqualsBD(BigDecimal(300), transactionSumByDates.last().sum)
        val sumOnDateOutsideRange = transactionSumByDates.find { it.aggregateDate == dateInFuture }
        assertEqualsBD(BigDecimal(410), grandSum)
        assertNull(sumOnDateOutsideRange)
    }

}