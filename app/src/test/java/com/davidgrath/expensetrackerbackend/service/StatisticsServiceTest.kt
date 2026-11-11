package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TestRunConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.entities.StatisticsFilterDb
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
import com.davidgrath.expensetrackerbackend.testutils.DataBuilder
import com.davidgrath.expensetrackerbackend.testutils.TestConstants
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
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

        assertEquals(ValidateStatisticsFilterResult.CurrencyOrAccountIssue, statisticsService.validateFilter(profileId, statisticsFilter))
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
        assertEquals(ValidateStatisticsFilterResult.EntityDoesNotBelongToOwner, statisticsService.validateFilter(user1ProfileId, statisticsFilter))
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
        assertEquals(ValidateStatisticsFilterResult.EntityDoesNotBelongToOwner, statisticsService.validateFilter(user1ProfileId, statisticsFilter))
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
        assertEquals(ValidateStatisticsFilterResult.EntityDoesNotBelongToOwner, statisticsService.validateFilter(user1ProfileId, statisticsFilter))
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
        assertEquals(ValidateStatisticsFilterResult.EntityDoesNotBelongToOwner, statisticsService.validateFilter(user1ProfileId, statisticsFilter))
    }
}