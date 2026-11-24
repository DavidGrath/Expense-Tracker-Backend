package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TestRunConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
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

}