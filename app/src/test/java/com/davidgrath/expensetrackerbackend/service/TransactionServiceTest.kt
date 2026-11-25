package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TestRunConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.entities.StatisticsFilterDb
import com.davidgrath.expensetrackerbackend.generatedapi.model.SuggestionsFieldType
import com.davidgrath.expensetrackerbackend.repository.*
import com.davidgrath.expensetrackerbackend.testutils.*
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.uuidToBytes
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

@SpringBootTest
@RunWith(SpringRunner::class)
@ContextConfiguration(
    classes = [
        TestRunConfiguration::class,
        TransactionService::class,
        TransactionRepository::class
    ])
class TransactionServiceTest {

    @Autowired
    lateinit var transactionService: TransactionService
    @Autowired
    lateinit var timeHandler: TimeHandler
    @Autowired
    lateinit var profileRepository: ProfileRepository

    @Autowired
    lateinit var categoryRepository: CategoryRepository
    @Autowired
    lateinit var accountRepository: AccountRepository
    @Autowired
    lateinit var userRepository: UserRepository
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
    fun correctTimeTest() {
        val accountId = getDefaultAccountId(userRepository, profileRepository, accountRepository)
        val user = userRepository.findByUuid(uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)))!!
        val profile = profileRepository.findByUuid(user.id!!, uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID)))!!
        val profileId = profile.id!!
        val category = categoryRepository.findByProfileIdAndStringId(profileId, "miscellaneous")!!
        val transactionId = transactionService.addTransaction(profileId, accountId, false, BigDecimal.TEN, "Description", category.id!!,
            ZoneId.of("Etc/GMT-2"))
        val transaction = transactionService.getByProfileIdAndId(profileId, transactionId)!!
        val expectedTime = LocalTime.parse("10:00")
        assertEquals(expectedTime, transaction.time)
    }

    @Test
    fun givenTextHasHigherFrequencyAndLowerRecencyWhenGetSuggestionsThenSuggestionAppearsLater() {
        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()

        val user = userRepository.findByUuid(uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)))!!
        val profile = profileRepository.findByUuid(user.id!!, uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID)))!!
        val profileId = profile.id!!

        val dataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)

        val irrelevantDescription = "Water"
        val higherFrequencyDescription = "Chocolate"
        val lowerFrequencyDescription = "Cookies"
        val today = LocalDate.parse("2025-06-30")

        val bucket1Dates = listOf(today, today.minusDays(1)) //Total frequency: 2

        val _bucket3Dates = listOf(today.minusDays(15), today.minusDays(16), today.minusDays(20), today.minusDays(25), today.minusDays(25))
        val bucket3Dates = _bucket3Dates + _bucket3Dates //Total frequency: 10

        dataBuilder.createTransaction()
            .withItem(irrelevantDescription, "miscellaneous", BigDecimal(1000.00))
            .repeatIntoDates(bucket1Dates)
            .useDatesAsCreationDates(true)
            .commit()

        dataBuilder.createTransaction()
            .withItem(higherFrequencyDescription, "miscellaneous", BigDecimal(1000.00))
            .repeatIntoDates(bucket1Dates)
            .useDatesAsCreationDates(true)
            .commit()

        dataBuilder.createTransaction()
            .withItem(lowerFrequencyDescription, "miscellaneous", BigDecimal(1000.00))
            .repeatIntoDates(bucket3Dates)
            .useDatesAsCreationDates(true)
            .commit()

        val suggestions = transactionService.getGenericSuggestions(profileId, SuggestionsFieldType.DESCRIPTION, "C")
        assertEquals(2, suggestions.size)

        assertEquals(lowerFrequencyDescription, suggestions[0])
        assertEquals(higherFrequencyDescription, suggestions[1])
    }


    @Test
    fun givenTextHasHigherFrecencyWhenGetSuggestionsThenSuggestionAppearsEarlier() {

        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()

        val user = userRepository.findByUuid(uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)))!!
        val profile = profileRepository.findByUuid(user.id!!, uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID)))!!
        val profileId = profile.id!!

        val dataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)

        val irrelevantDescription = "Water"
        val higherFrequencyDescription = "Chocolate"
        val lowerFrequencyDescription = "Cookies"
        val today = LocalDate.parse("2025-06-30")

        val _bucket1Dates = listOf(today, today.minusDays(1), today.minusDays(1), today.minusDays(2), today.minusDays(4))
        val bucket1Dates = _bucket1Dates + _bucket1Dates //Total frequency: 10

        val bucket3Dates = listOf(today.minusDays(15), today.minusDays(16)) //Total frequency: 2

        dataBuilder.createTransaction()
            .withItem(irrelevantDescription, "miscellaneous", BigDecimal(1000.00))
            .repeatIntoDates(bucket1Dates)
            .useDatesAsCreationDates(true)
            .commit()

        dataBuilder.createTransaction()
            .withItem(higherFrequencyDescription, "miscellaneous", BigDecimal(1000.00))
            .repeatIntoDates(bucket1Dates)
            .useDatesAsCreationDates(true)
            .commit()

        dataBuilder.createTransaction()
            .withItem(lowerFrequencyDescription, "miscellaneous", BigDecimal(1000.00))
            .repeatIntoDates(bucket3Dates)
            .useDatesAsCreationDates(true)
            .commit()

        val suggestions = transactionService.getGenericSuggestions(profileId,SuggestionsFieldType.DESCRIPTION, "C")

        assertEquals(2, suggestions.size)
        assertEquals(higherFrequencyDescription, suggestions[0])
        assertEquals(lowerFrequencyDescription, suggestions[1])
    }

    /**
     * I've decided to let the suggestions be case-sensitive, because
     * otherwise coercing them to lowercase for the sake of weighting means the user would never get sentence-case suggestions
     */
    @Test
    fun frecencyMixedCaseTest() {

        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()

        val user = userRepository.findByUuid(uuidToBytes(UUID.fromString(TestConstants.DEFAULT_USER_ID)))!!
        val profile = profileRepository.findByUuid(user.id!!, uuidToBytes(UUID.fromString(Constants.DEFAULT_PROFILE_ID)))!!
        val profileId = profile.id!!

        val dataBuilder = DataBuilder(folder, timeHandler, null, Constants.DEFAULT_PROFILE_ID, testUtilComponent)

        val irrelevantDescription = "Water"
        val higherFrequencyDescription = "Chocolate"
        val lowerFrequencyDescription = "Cookies"
        val lowerFrequencyDescriptionB = "COokiEs"
        val today = LocalDate.parse("2025-06-30")

        val bucket1Dates = listOf(today, today.minusDays(1)) //Total frequency: 2

        val _bucket3Dates = listOf(today.minusDays(15), today.minusDays(16), today.minusDays(20), today.minusDays(25), today.minusDays(25))

        dataBuilder.createTransaction()
            .withItem(irrelevantDescription, "miscellaneous", BigDecimal(1000.00))
            .repeatIntoDates(bucket1Dates)
            .useDatesAsCreationDates(true)
            .commit()

        dataBuilder.createTransaction()
            .withItem(higherFrequencyDescription, "miscellaneous", BigDecimal(1000.00))
            .repeatIntoDates(bucket1Dates)
            .useDatesAsCreationDates(true)
            .commit()

        dataBuilder.createTransaction()
            .withItem(lowerFrequencyDescription, "miscellaneous", BigDecimal(1000.00))
            .repeatIntoDates(_bucket3Dates)
            .useDatesAsCreationDates(true)
            .commit()
        dataBuilder.createTransaction()
            .withItem(lowerFrequencyDescriptionB, "miscellaneous", BigDecimal(1000.00))
            .repeatIntoDates(_bucket3Dates)
            .useDatesAsCreationDates(true)
            .commit()

        val suggestions = transactionService.getGenericSuggestions(profileId,SuggestionsFieldType.DESCRIPTION, "c")
        assertEquals(3, suggestions.size)
        assertTrue(lowerFrequencyDescription in suggestions.take(2))
        assertTrue(lowerFrequencyDescriptionB in suggestions.take(2))
        assertEquals(higherFrequencyDescription, suggestions[2])
    }

    /**
     * TODO Add a test here similar to the one at [com.davidgrath.expensetrackerbackend.service.StatisticsServiceTest.getItemsTest]
     */


}