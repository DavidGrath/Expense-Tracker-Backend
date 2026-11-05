package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.configuration.TestRunConfiguration
import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.testutils.DataBuilder
import com.davidgrath.expensetrackerbackend.testutils.TestConstants
import com.davidgrath.expensetrackerbackend.testutils.TestUtilComponent
import com.davidgrath.expensetrackerbackend.testutils.assertEqualsBD
import com.davidgrath.expensetrackerbackend.testutils.getDefaultAccountId
import com.davidgrath.expensetrackerbackend.testutils.tempInit
import com.davidgrath.expensetrackerbackend.utils.Constants
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.TRUE_BYTE
import org.junit.After
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal
import java.nio.file.Files
import java.time.LocalDate
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
class TransactionItemRepositoryTest {

    @Autowired
    lateinit var transactionItemRepository: TransactionItemRepository
    @Autowired
    lateinit var categoryRepository: CategoryRepository
    @Autowired
    lateinit var userRepository: UserRepository
    @Autowired
    lateinit var profileRepository: ProfileRepository
    @Autowired
    lateinit var accountRepository: AccountRepository
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
    fun totalSpentFiltersTest() {

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

        val sumNoFilter = transactionItemRepository.getTransactionItemDebitSum(profile.id!!, null, null, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        val accounts = listOf(newAccountId)
        val sumFilteredByAccount = transactionItemRepository.getTransactionItemDebitSum(profile.id!!, null, null,  accounts, emptyList(), emptyList(), emptyList(), emptyList())
        val dates = listOf(firstTransactionDate, thirdTransactionDate, fifthTransactionDate)
        val sumFilteredByDateList = transactionItemRepository.getTransactionItemDebitSum(profile.id!!, null, null, emptyList(), dates, emptyList(), emptyList(), emptyList())
        val categories = listOf(fitness!!.id!!, utilities!!.id!!, clothing!!.id!!)
        val sumFilteredByCategory = transactionItemRepository.getTransactionItemDebitSum(profile.id!!, null, null, emptyList(), emptyList(), categories, emptyList(), emptyList())
        val modes = listOf(TransactionMode.Transfer)
        val sumFilteredByMode = transactionItemRepository.getTransactionItemDebitSum(profile.id!!, null, null, emptyList(), emptyList(), emptyList(), modes, emptyList())
        val sellers = listOf(sellerId)
        val sumFilteredBySeller = transactionItemRepository.getTransactionItemDebitSum(profile.id!!, null, null, emptyList(), emptyList(), emptyList(), emptyList(), sellers)
        val sumFilteredByDateRange = transactionItemRepository.getTransactionItemDebitSum(profile.id!!, secondTransactionDate, fifthTransactionDate, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

        assertEqualsBD(BigDecimal(3310.00), sumNoFilter)
        assertEqualsBD(BigDecimal(600.00), sumFilteredByAccount)
        assertEqualsBD(BigDecimal(1960.00), sumFilteredByDateList)
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

        val sumNoFilter = transactionItemRepository.getTransactionItemCreditSum(profile.id!!, null, null, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        val accounts = listOf(newAccountId)
        val sumFilteredByAccount = transactionItemRepository.getTransactionItemCreditSum(profile.id!!, null, null, accounts, emptyList(), emptyList(), emptyList(), emptyList())
        val dates = listOf(firstTransactionDate, thirdTransactionDate, fifthTransactionDate)
        val sumFilteredByDateList = transactionItemRepository.getTransactionItemCreditSum(profile.id!!, null, null, emptyList(), dates, emptyList(), emptyList(), emptyList())
        val categories = listOf(fitness!!.id!!, utilities!!.id!!, clothing!!.id!!)
        val sumFilteredByCategory = transactionItemRepository.getTransactionItemCreditSum(profile.id!!, null, null, emptyList(), emptyList(), categories, emptyList(), emptyList())
        val modes = listOf(TransactionMode.Transfer)
        val sumFilteredByMode = transactionItemRepository.getTransactionItemCreditSum(profile.id!!, null, null, emptyList(), emptyList(), emptyList(), modes, emptyList())
        val sellers = listOf(sellerId)
        val sumFilteredBySeller = transactionItemRepository.getTransactionItemCreditSum(profile.id!!, null, null, emptyList(), emptyList(), emptyList(), emptyList(), sellers)
        val sumFilteredByDateRange = transactionItemRepository.getTransactionItemCreditSum(profile.id!!, secondTransactionDate, fifthTransactionDate, emptyList(), emptyList(), emptyList(),  emptyList(),  emptyList())

        assertEqualsBD(BigDecimal(3310.00), sumNoFilter)
        assertEqualsBD(BigDecimal(600.00), sumFilteredByAccount)
        assertEqualsBD(BigDecimal(1960.00), sumFilteredByDateList)
        assertEqualsBD(BigDecimal(2150.00), sumFilteredByCategory)
        assertEqualsBD(BigDecimal(450.00), sumFilteredByMode)
        assertEqualsBD(BigDecimal(750.00), sumFilteredBySeller)
        assertEqualsBD(BigDecimal(2800.00), sumFilteredByDateRange)

    }
}