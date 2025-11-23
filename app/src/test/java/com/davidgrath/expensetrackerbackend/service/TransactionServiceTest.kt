package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TimeAndLocaleHandler
import com.davidgrath.expensetrackerbackend.repository.*
import com.davidgrath.expensetrackerbackend.testutils.*
import com.davidgrath.expensetrackerbackend.utils.Constants
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalTime

@RunWith(SpringRunner::class)
@ContextConfiguration(
    classes = [
        TestConfiguration::class,
        TransactionService::class,
        TransactionRepository::class
    ])
@TestPropertySource(
    locations = ["classpath:application-dev.properties", "classpath:secrets-dev.properties"])
class TransactionServiceTest {

    @Autowired
    lateinit var transactionService: TransactionService
    @Autowired
    lateinit var timeAndLocaleHandler: TimeAndLocaleHandler
    @Autowired
    lateinit var profileRepository: ProfileRepository
    @Autowired
    lateinit var transactionRepository: TransactionRepository

    @Autowired
    lateinit var categoryRepository: CategoryRepository
    @Autowired
    lateinit var accountRepository: AccountRepository
    @Autowired
    lateinit var itemRepository: TransactionItemRepository
    @Autowired
    lateinit var imageRepository: ImageRepository
    @Autowired
    lateinit var itemImagesRepository: ItemImagesRepository
    @Autowired
    lateinit var userRepository: UserRepository

    @Before
    fun setUp() {
        tempInit(userRepository, profileRepository, accountRepository, categoryRepository, timeAndLocaleHandler)
    }

    @After
    fun tearDown() {
        deleteAll(itemImagesRepository, itemRepository, imageRepository, categoryRepository, transactionRepository, accountRepository, profileRepository, userRepository)
    }

    @Test
    fun givenTransactionRangeDoesNotHaveDatesInIntervalWhenFetchTotalThenDatesIntervalHaveZeroes() {
        val folder = Files.createTempDirectory("temp").toFile()
        folder.deleteOnExit()
        val dataBuilder = DataBuilder(folder, timeAndLocaleHandler, "", Constants.DEFAULT_PROFILE_ID, transactionRepository, categoryRepository, profileRepository, accountRepository, itemRepository, imageRepository, itemImagesRepository)
        val profile = profileRepository.findByStringId(Constants.DEFAULT_PROFILE_ID)!!
        val profileId = profile.id!!
        val time = LocalTime.of(8, 0, 0)
        val firstTransactionDate = LocalDate.parse("2025-01-02")
        val secondTransactionDate = LocalDate.parse("2025-01-02")
        val thirdTransactionDate = LocalDate.parse("2025-01-07")
        val fourthTransactionDate = LocalDate.parse("2025-01-09")
//        val fifthTransactionDate = LocalDate.parse("2025-01-11")
        val fifthTransactionDate = LocalDate.parse("2025-07-11")

        val accountId = getDefaultAccountId(profileRepository, accountRepository)
        //Empty Set
        var transactionSumByDates = transactionService.getTotalAmountByDate(profileId, true, LocalDate.parse("2025-07-01"), null, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        assertEquals(0, transactionSumByDates.size)

        //Today and empty
        transactionSumByDates = transactionService.getTotalAmountByDate(profileId, true, LocalDate.parse("2025-06-30"), null, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        assertEquals(1, transactionSumByDates.size)
        assertEqualsBD(BigDecimal.ZERO, transactionSumByDates.first().sum)

        //Multiple days and empty
        transactionSumByDates = transactionService.getTotalAmountByDate(profileId, true, LocalDate.parse("2025-06-01"), null, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
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

        //Just 1 transaction
        transactionSumByDates = transactionService.getTotalAmountByDate(profileId, true, LocalDate.parse("2025-01-02"), null, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        val daysBetweenJan2AndJun30Inclusive = 180
        assertEquals(daysBetweenJan2AndJun30Inclusive, transactionSumByDates.size)

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


        transactionSumByDates = transactionService.getTotalAmountByDate(profileId, true, LocalDate.parse("2025-01-02"), null, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        val sum1 = transactionSumByDates.find { it.aggregateDate == firstTransactionDate }!!
        var missingSum1 = transactionSumByDates.find { it.aggregateDate == LocalDate.parse("2025-01-03") }!!
        val missingSum2 = transactionSumByDates.find { it.aggregateDate == LocalDate.parse("2025-01-08") }!!
        val missingSum3 = transactionSumByDates.find { it.aggregateDate == LocalDate.parse("2025-01-10") }!!
        assertEqualsBD(BigDecimal(410), sum1.sum)
        assertEqualsBD(BigDecimal.ZERO, missingSum1.sum)
        assertEqualsBD(BigDecimal.ZERO, missingSum2.sum)
        assertEqualsBD(BigDecimal.ZERO, missingSum3.sum)

        transactionSumByDates = transactionService.getTotalAmountByDate(profileId, true, firstTransactionDate, thirdTransactionDate, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        val daysBetweenFirstAndThirdTransactionInclusive = 6

        grandSum = transactionSumByDates.map { it.sum }.reduce { acc, bigDecimal -> acc.plus(bigDecimal) }
        missingSum1 = transactionSumByDates.find { it.aggregateDate == LocalDate.parse("2025-01-03") }!!

        assertEquals(daysBetweenFirstAndThirdTransactionInclusive, transactionSumByDates.size)
        assertEqualsBD(BigDecimal(860), grandSum)
        assertEqualsBD(BigDecimal.ZERO, missingSum1.sum)
    }
}